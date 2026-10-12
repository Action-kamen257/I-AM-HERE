package com.iamhere.emergency.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.iamhere.emergency.data.model.LocationPoint
import com.iamhere.emergency.data.repository.EmergencyManager
import java.util.Locale

class EmergencyCallManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            isTtsReady = true
        }
    }

    /**
     * Initiates immediate 112 emergency call
     */
    @SuppressLint("MissingPermission")
    fun dial112() {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:112")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    /**
     * Reads current location coordinates and medical notes to 112 operator over speaker
     */
    fun speakLocationToOperator(location: LocationPoint?) {
        if (!isTtsReady || location == null) return

        val manager = EmergencyManager.getInstance()
        val speechText = buildString {
            append("Emergency distress alert. ")
            append("User name: ${manager.userName}. ")
            append("Current coordinates: latitude ${String.format(Locale.US, "%.5f", location.latitude)}, ")
            append("longitude ${String.format(Locale.US, "%.5f", location.longitude)}. ")
            append("Accuracy radius: ${location.accuracy.toInt()} meters. ")
            if (manager.medicalNotes.isNotBlank()) {
                append("Medical information: ${manager.medicalNotes}. ")
            }
        }

        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "112_LOCATION_SPEECH")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
