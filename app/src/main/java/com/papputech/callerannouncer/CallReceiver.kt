package com.papputech.callerannouncer
import android.Manifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale


class CallReceiver : BroadcastReceiver() {
    private var lastNumber: String? = null
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private lateinit var tts: TextToSpeech

    override fun onReceive(context: Context, intent: Intent) {
        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val newState = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> TelephonyManager.CALL_STATE_IDLE
        }

        // Initialize Text-to-Speech
        if (!this::tts.isInitialized) {
            tts = TextToSpeech(context, null)
        }

        // Check for state transition from RINGING to OFFHOOK
        if (lastState == TelephonyManager.CALL_STATE_RINGING && newState == TelephonyManager.CALL_STATE_OFFHOOK) {
            // Stop TTS if it's currently speaking
            if (tts.isSpeaking) {
                tts.stop()
            }
        }

        // Announce caller's name if the phone is ringing and it's a new call
        if (number != null && newState == TelephonyManager.CALL_STATE_RINGING && lastState != newState) {
            announceCallerName(number, context)
        }

        lastState = newState
    }

    private fun announceCallerName(phoneNumber: String?, context: Context) {
        if (phoneNumber == null) return
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo = wifiManager.connectionInfo

    // Accessing SSID requires location permissions on some Android versions
    val ssid = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")  // Removing quotes
    } else {
        "Permission required"
    }
        if (ssid != "Smart Wifi") return
        val callerName = getContactName(phoneNumber, context) ?: ""
        tts = TextToSpeech(context) { status ->
            tts.language =  Locale("ta", "IN")
            if (status == TextToSpeech.SUCCESS) {
                var result = "$callerName கூப்பிடுறாங்க ."
                result += result
                tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun getContactName(phoneNumber: String, context: Context): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                var index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return if ( index != -1) {
                    cursor.getString(index)
                } else {
                    null
                }

            }
        }
        return null
    }

    // Remember to add a method to shutdown Text-to-Speech
    fun shutdownTTS() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
