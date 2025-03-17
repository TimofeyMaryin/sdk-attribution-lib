package com.attribution.sdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attribution.sdk.ui.theme.AttributionSDKTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sdk = AttributionSDK(this)

        enableEdgeToEdge()
        setContent {
            var bundleId by remember { mutableStateOf("") }
            var deviceId by remember { mutableStateOf("") }
            var installReferrer by remember { mutableStateOf("") }
            var isFirst by remember { mutableStateOf("") }


            LaunchedEffect(key1 = Unit) {
                sdk.sendInstallData("http://192.168.1.227:8080") {
                    bundleId = it.bundleId
                    deviceId = it.deviceId
                    installReferrer = it.installReferrer ?: "Error"
                    isFirst = it.isFirstInstall.toString()
                }
            }
            AttributionSDKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {

                                Text(text = "BundleID: $bundleId")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "DeviceId: $deviceId")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "InstallReferrer: $installReferrer")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Is First Launch: $isFirst")

                            }

                        }

                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AttributionSDKTheme {
        Greeting("Android")
    }
}