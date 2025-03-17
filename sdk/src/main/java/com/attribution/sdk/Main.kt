package com.attribution.sdk

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

data class InstallData(
    val bundleId: String,
    val deviceId: String,
    val installReferrer: String? = null,
    val isFirstInstall: Boolean,
    val unityAdsData: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class AttributionSDK(private val context: Context) {

    private val prefs = context.getSharedPreferences("attribution_sdk", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    private fun isFirstInstall(): Boolean {
        return !prefs.getBoolean("isInstalled", false)
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
    fun fetchUnityAdsData(): String {
        // Здесь можно реализовать вызовы к Unity Ads SDK.
        // Для MVP возвращаем симулированное значение.
        return "unity_ads_simulated_data"
    }

    private fun generateUniqueDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (androidId == "9774d56d682e549c" || androidId.isNullOrEmpty()) {
            UUID.randomUUID().toString() // Генерируем случайный ID, если `ANDROID_ID` некорректен
        } else {
            androidId
        }
    }


    // Собираем данные установки и отправляем их на сервер
    fun sendInstallData(
        serverUrl: String,
        data: (InstallData) -> Unit, // DEBUG!!
    ) {
        val bundleId = context.packageName
        val deviceId = generateUniqueDeviceId(context)
        val firstInstall = isFirstInstall()

        fetchInstallReferrer { referrer ->
            if (firstInstall) {
                markInstalled()
            }
            val unityAdsData = fetchUnityAdsData()
            val installData = InstallData(
                bundleId = bundleId,
                deviceId = deviceId,
                installReferrer = referrer,
                isFirstInstall = firstInstall,
                unityAdsData = unityAdsData
            )

            Log.e("TAG", "sendInstallData: $installData", )
            data(installData)
            sendDataToServer(serverUrl, installData)
        }
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