package com.attribution.sdk

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.attribution.sdk.data.InstallData
import com.attribution.sdk.info.DeviceInfo
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.Executors




class AttributionSDK(private val context: Context) {

    private val prefs = context.getSharedPreferences("attribution_sdk", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val gson = Gson()


    // Собираем данные установки и отправляем их на сервер
    fun sendInstallData(
        serverUrl: String,
        data: (InstallData) -> Unit, // DEBUG!!
    ) {
        val deviceInfo = DeviceInfo(context)


        val bundleId = context.packageName
        val deviceId = deviceInfo.getDeviceId()
        val firstInstall = isFirstInstall()
        val appName = deviceInfo.getAppName()
        val unityAdsData = fetchUnityAdsData()
        val androidVersion = deviceInfo.getAndroidVersion()
        val apiLevel = deviceInfo.getApiLevel()
        val appVersion = deviceInfo.getAppVersion()
        val country = deviceInfo.getCountry()
        val deviceManufacturer = deviceInfo.getDeviceManufacturer()
        val deviceModel = deviceInfo.getDeviceModel()
        val isFromPlayStore = deviceInfo.isFromPlayStore()
        val language = deviceInfo.getLanguage()
        val network = deviceInfo.getNetworkType()


        if (isFirstInstall()) {

            fetchGoogleAdId(context) { gaid ->
                fetchInstallReferrer { referrer ->
                    if (firstInstall) {
                        markInstalled()
                    }

                    val installData = InstallData(
                        bundleId = bundleId,
                        deviceId = deviceId,
                        installReferrer = referrer,
                        isFirstInstall = firstInstall,
                        unityAdsData = unityAdsData,
                        appName = appName,
                        googleAdId = gaid,
                        androidVersion = androidVersion,
                        apiLevel = apiLevel,
                        appVersion = appVersion,
                        country = country,
                        deviceManufacturer = deviceManufacturer,
                        deviceModel = deviceModel,
                        isFromPlayStore = isFromPlayStore,
                        language = language,
                        networkType = network,
                    )

                    Log.e("TAG", "sendInstallData: $installData", )
                    data(installData)
                    sendDataToServer(serverUrl, installData)
                }

            }
        } else {
            Log.e("TAG", "sendInstallData: is not first install!", )
        }
    }

    private fun isFirstInstall(): Boolean {
        return !prefs.getBoolean("isInstalled", false)
    }

    private fun fetchGoogleAdId(context: Context, callback: (String?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val adId = if (!adInfo.isLimitAdTrackingEnabled) adInfo.id else null  // Проверяем ограничения
                callback(adId)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
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