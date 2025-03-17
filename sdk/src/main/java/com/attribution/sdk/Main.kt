package com.attribution.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

data class InstallData(
    val bundleId: String,
    val appName: String,
    val appVersion: String,
    val deviceId: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val language: String,
    val country: String,
    val installReferrer: String?,
    val isFirstInstall: Boolean,
    val googleAdId: String?,
    val networkType: String,
    val isFromPlayStore: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val unityAdsData: String?,
)


class DeviceInfo(private val context: Context) {

    // Уникальный идентификатор устройства (Android ID)
    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (androidId == "9774d56d682e549c" || androidId.isNullOrEmpty()) {
            UUID.randomUUID().toString()  // Генерируем случайный ID, если ANDROID_ID недоступен
        } else {
            androidId
        }
    }

    // Название приложения
    fun getAppName(): String {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(context.packageName, 0)
        return packageManager.getApplicationLabel(appInfo).toString()
    }

    // Версия приложения
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Error"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    // Модель устройства
    fun getDeviceModel(): String = Build.MODEL

    // Производитель устройства
    fun getDeviceManufacturer(): String = Build.MANUFACTURER

    // Версия Android
    fun getAndroidVersion(): String = Build.VERSION.RELEASE

    // API Level
    fun getApiLevel(): Int = Build.VERSION.SDK_INT

    // Системный язык
    fun getLanguage(): String = Locale.getDefault().language


    fun getCountry(): String = context.resources.configuration.locales[0].country

    fun isFromPlayStore(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending"
    }


    // Тип сети (Wi-Fi или Mobile)
    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return when (activeNetwork?.type) {
            android.net.ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
            android.net.ConnectivityManager.TYPE_MOBILE -> "Mobile"
            else -> "Unknown"
        }
    }
}

class AttributionSDK(private val context: Context) {

    private val prefs = context.getSharedPreferences("attribution_sdk", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val gson = Gson()

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


}