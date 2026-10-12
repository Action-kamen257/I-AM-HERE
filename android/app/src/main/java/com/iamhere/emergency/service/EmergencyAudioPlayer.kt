package com.iamhere.emergency.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

class EmergencyAudioPlayer(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var isAlarmPlaying = false
    private var alarmJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Victim device is kept strictly silent so it does not make sound or alert an attacker
    var isVictimSilentMode: Boolean = true

    /**
     * Emergency audio alarm - silenced when isVictimSilentMode is enabled.
     */
    fun startEmergencyAlarm() {
        if (isVictimSilentMode) return
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        alarmJob = scope.launch {
            val vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
            val vibrationAmplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

            while (isActive && isAlarmPlaying) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, vibrationAmplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationPattern, -1)
                }
                delay(600)
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 400)
                delay(600)
            }
        }
    }

    fun playWarningBeep() {
        if (isVictimSilentMode) return
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopEmergencyAlarm() {
        isAlarmPlaying = false
        alarmJob?.cancel()
        alarmJob = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
