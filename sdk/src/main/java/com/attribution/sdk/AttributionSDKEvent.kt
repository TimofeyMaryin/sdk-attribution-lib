package com.attribution.sdk

import android.content.Context
import android.hardware.usb.UsbDevice.getDeviceId
import android.util.Log
import com.attribution.sdk.data.AuthModel
import com.attribution.sdk.data.AuthToken
import com.attribution.sdk.data.EventData
import com.attribution.sdk.info.DeviceInfo
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class AttributionSDKEvent(private val context: Context) {

    private val prefs = context.getSharedPreferences("attribution_sdk", Context.MODE_PRIVATE)

    private val deviceInfo = DeviceInfo(context)
    private val gson = Gson()

    fun sendEventToServer(eventName: String) {

        val eventData = EventData(
            event = eventName,
            bundleId = context.packageName,
            deviceId = deviceInfo.getDeviceId()
        )

        val json = gson.toJson(eventData)
        val request = Request.Builder()
            .url("http://192.168.1.227:8080/event")
            .addHeader("Authorization", "Bearer ${getToken()}")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EventTracker", "Ошибка отправки события: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun getToken(): String {
        val token = prefs.getString("token", "") ?: "Error"
        Log.e("TAG", "getToken: $token", )
        return token

    }


}