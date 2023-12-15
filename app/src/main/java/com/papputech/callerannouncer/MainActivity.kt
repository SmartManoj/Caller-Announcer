package com.papputech.callerannouncer
import android.Manifest
import android.content.Context
import android.content.IntentFilter

import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.papputech.callerannouncer.ui.theme.CallerAnnouncerTheme
import androidx.compose.material.*
import androidx.compose.material3.Shapes
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


@Composable
fun MainScreen() {
   val context = LocalContext.current
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo

    // Accessing SSID requires location permissions on some Android versions
    val ssid = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")  // Removing quotes
    } else {
        "Permission required"
    }

    Text(text = "Connected WiFi SSID: $ssid")
}
class MainActivity : ComponentActivity() {
    private lateinit var callReceiver: CallReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize CallReceiver
        callReceiver = CallReceiver()

        setContent {
            MainScreen()
        }

        // Check and request permissions
        checkAndRequestPermissions()
    }



    private fun checkAndRequestPermissions() {
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
//TODO call logs add
        if (!hasPhoneStatePermission || !hasContactsPermission) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS), PERMISSION_REQUEST_CODE)
        } else {
            // Permissions are already granted, register receiver
            registerCallReceiver()
        }
    }

    private fun registerCallReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
        }
        registerReceiver(callReceiver, filter)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, register receiver
                registerCallReceiver()
            } else {
                // Permissions not granted. You can show a message to the user or handle accordingly.
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver and shutdown TTS when activity is destroyed
        if (this::callReceiver.isInitialized) {
            unregisterReceiver(callReceiver)
            callReceiver.shutdownTTS()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }
}