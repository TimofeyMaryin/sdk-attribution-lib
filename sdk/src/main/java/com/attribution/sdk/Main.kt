package com.attribution.sdk

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.attribution.sdk.data.InstallData
import com.attribution.sdk.info.DeviceInfo
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant




class AttributionSDK(private val context: Context) {

    private val prefs = context.getSharedPreferences("attribution_sdk", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val gson = Gson()


    @RequiresApi(Build.VERSION_CODES.O)
    fun sendInstallData(
        serverUrl: String,
        data: (InstallData) -> Unit, // DEBUG!!
    ) {
        val deviceInfo = DeviceInfo(context)

        val installData = InstallData(
            bundleId = context.packageName,
            deviceId = deviceInfo.getDeviceId(),
            isFirstInstall = isFirstInstall(),
            unityAdsData = fetchUnityAdsData(),
            appName = deviceInfo.getAppName(),
            androidVersion = deviceInfo.getAndroidVersion(),
            apiLevel = deviceInfo.getApiLevel(),
            appVersion = deviceInfo.getAppVersion(),
            country = deviceInfo.getCountry(),
            deviceManufacturer = deviceInfo.getDeviceManufacturer(),
            deviceModel = deviceInfo.getDeviceModel(),
            isFromPlayStore = deviceInfo.isFromPlayStore(),
            language = deviceInfo.getLanguage(),
            networkType = deviceInfo.getNetworkType(),
            timestamp = Instant.now().toEpochMilli(),
            installReferrer = null,  // Заполним позже
            googleAdId = null,       // Заполним позже
            utmData = null           // Заполним позже
        )

        if (!installData.isFirstInstall) {
            Log.e("TAG", "sendInstallData: is not first install!")
            return
        }

        // ⚡️ Запускаем параллельно получение GAID, InstallReferrer и UTM
        CoroutineScope(Dispatchers.IO).launch {
            val gaidDeferred = async { fetchGoogleAdId(context) }
            val referrerDeferred = async { fetchInstallReferrer() }
            val utmDeferred = async { fetchUTMData(context) }

            val googleAdId = gaidDeferred.await()
            val installReferrer = referrerDeferred.await()
            val utmData = utmDeferred.await()

            // ✅ Обновляем InstallData
            val updatedInstallData = installData.copy(
                googleAdId = googleAdId,
                installReferrer = installReferrer,
                utmData = utmData
            )

            Log.e("TAG", "sendInstallData: $updatedInstallData")
            data(updatedInstallData)
            sendDataToServer(serverUrl, updatedInstallData)

            // Помечаем установку как завершённую
            sendDataToServer(serverUrl, installData)
            markInstalled()
        }
    }

    private fun isFirstInstall(): Boolean {
        return !prefs.getBoolean("isInstalled", false)
    }



    private suspend fun fetchGoogleAdId(context: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (!adInfo.isLimitAdTrackingEnabled) adInfo.id else null
        } catch (e: Exception) {
            Log.e("TAG", "Ошибка получения GAID: ${e.message}")
            null
        }
    }

    private suspend fun fetchUTMData(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("attribution_prefs", Context.MODE_PRIVATE)
        return@withContext prefs.getString("utm_data", null)
    }

    private suspend fun fetchInstallReferrer(): String? = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        val referrer = referrerClient.installReferrer.installReferrer
                        Log.e("InstallReferrer", "Полученные UTM-метки: $referrer")  // 🔹 Логируем UTM
                        continuation.resume(referrer, null)
                    } else {
                        continuation.resume(null, null)
                    }
                    referrerClient.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() {
                    continuation.resume(null, null)
                }
            })
        }
    }

    private fun markInstalled() {
        prefs.edit().putBoolean("isInstalled", true).apply()
    }

    private fun fetchInstallReferrer(onResult: (String?) -> Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    val response = referrerClient.installReferrer
                    onResult(response.installReferrer)
                } else {
                    onResult(null)
                }
                referrerClient.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                onResult(null)
            }
        })
    }

    // Симуляция интеграции с Unity ADS
    private fun fetchUnityAdsData(): String {
        // Здесь можно реализовать вызовы к Unity Ads SDK.
        // Для MVP возвращаем симулированное значение.
        return "unity_ads_simulated_data"
    }

    private fun sendDataToServer(serverUrl: String, installData: InstallData) {
        val json = gson.toJson(installData)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$serverUrl/install")
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Обработка ошибки отправки данных
                Log.e("TAG", "onFailure httpClient: ${e.printStackTrace()}", )
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                // Можно добавить обработку успешного ответа
                Log.e("TAG", "onResponse success: ${response.body?.string()}", )
                response.close()
            }
        })
    }



}