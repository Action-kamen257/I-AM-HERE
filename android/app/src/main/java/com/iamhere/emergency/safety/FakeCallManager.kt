package com.iamhere.emergency.safety

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCallManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FakeCallManager? = null

        fun getInstance(context: Context): FakeCallManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FakeCallManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var callDurationJob: Job? = null
    private var ringtone: Ringtone? = null

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    var callerName: String = "Dad"
    var callerNumber: String = "+91 98200 12345"

    private val _isCallPending = MutableStateFlow(false)
    val isCallPending: StateFlow<Boolean> = _isCallPending.asStateFlow()

    private val _pendingCountdown = MutableStateFlow<Int?>(null)
    val pendingCountdown: StateFlow<Int?> = _pendingCountdown.asStateFlow()

    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    private val _isCallConnected = MutableStateFlow(false)
    val isCallConnected: StateFlow<Boolean> = _isCallConnected.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    fun scheduleFakeCall(name: String, delaySec: Int) {
        callerName = name
        cancelPending()

        if (delaySec <= 0) {
            startRinging()
            return
        }

        _isCallPending.value = true
        timerJob = scope.launch {
            for (sec in delaySec downTo 1) {
                _pendingCountdown.value = sec
                delay(1000)
            }
            _isCallPending.value = false
            _pendingCountdown.value = null
            startRinging()
        }
    }

    fun startRinging() {
        _isCallPending.value = false
        _pendingCountdown.value = null
        _isRinging.value = true
        _isCallConnected.value = false
        _callDurationSeconds.value = 0

        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate pattern for incoming call
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    fun answerCall() {
        stopRingingAudio()
        _isRinging.value = false
        _isCallConnected.value = true
        _callDurationSeconds.value = 0

        callDurationJob?.cancel()
        callDurationJob = scope.launch {
            while (isActive && _isCallConnected.value) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun endCall() {
        stopRingingAudio()
        callDurationJob?.cancel()
        callDurationJob = null
        _isRinging.value = false
        _isCallConnected.value = false
        _callDurationSeconds.value = 0
    }

    fun cancelPending() {
        timerJob?.cancel()
        timerJob = null
        _isCallPending.value = false
        _pendingCountdown.value = null
    }

    private fun stopRingingAudio() {
        try {
            ringtone?.stop()
            ringtone = null
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
