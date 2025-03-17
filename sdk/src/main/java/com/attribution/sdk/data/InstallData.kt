package com.attribution.sdk.data

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