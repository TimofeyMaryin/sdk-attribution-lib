package com.attribution.sdk.info

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.Locale
import java.util.UUID

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