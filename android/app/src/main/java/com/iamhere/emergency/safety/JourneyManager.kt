package com.iamhere.emergency.safety

import android.content.Context
import com.iamhere.emergency.data.repository.EmergencyManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JourneySession(
    val id: String,
    val destination: String,
    val durationMinutes: Int,
    val checkInIntervalMinutes: Int,
    val startedAt: Long,
    var nextCheckInAt: Long,
    val expiresAt: Long
)

class JourneyManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: JourneyManager? = null

        fun getInstance(context: Context): JourneyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JourneyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var journeyJob: Job? = null

    private val _activeJourney = MutableStateFlow<JourneySession?>(null)
    val activeJourney: StateFlow<JourneySession?> = _activeJourney.asStateFlow()

    private val _isCheckInPending = MutableStateFlow(false)
    val isCheckInPending: StateFlow<Boolean> = _isCheckInPending.asStateFlow()

    private val _checkInGraceSeconds = MutableStateFlow<Int?>(null)
    val checkInGraceSeconds: StateFlow<Int?> = _checkInGraceSeconds.asStateFlow()

    fun startJourney(destination: String, durationMinutes: Int, checkInIntervalMinutes: Int) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (durationMinutes * 60 * 1000L)
        val nextCheckIn = now + (checkInIntervalMinutes * 60 * 1000L).coerceAtMost(durationMinutes * 60 * 1000L)

        val session = JourneySession(
            id = "journey_${now}",
            destination = destination.ifBlank { "Safe Destination" },
            durationMinutes = durationMinutes,
            checkInIntervalMinutes = checkInIntervalMinutes,
            startedAt = now,
            nextCheckInAt = nextCheckIn,
            expiresAt = expiresAt
        )

        _activeJourney.value = session
        _isCheckInPending.value = false
        _checkInGraceSeconds.value = null

        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        journeyJob?.cancel()
        journeyJob = scope.launch {
            while (isActive && _activeJourney.value != null) {
                delay(1000)
                val current = _activeJourney.value ?: break
                val now = System.currentTimeMillis()

                // Check if journey expired
                if (now >= current.expiresAt) {
                    // Journey completed without incident
                    endJourney()
                    break
                }

                // Check if check-in interval is reached
                if (now >= current.nextCheckInAt && !_isCheckInPending.value) {
                    _isCheckInPending.value = true
                    // Start 60-second grace countdown
                    launchGraceCountdown()
                }
            }
        }
    }

    private fun launchGraceCountdown() {
        scope.launch {
            for (sec in 60 downTo 1) {
                if (!_isCheckInPending.value) break
                _checkInGraceSeconds.value = sec
                delay(1000)
            }

            if (_isCheckInPending.value) {
                // User missed check-in grace period! Auto-trigger emergency SOS
                _checkInGraceSeconds.value = null
                _isCheckInPending.value = false
                _activeJourney.value = null
                EmergencyManager.getInstance().triggerSosImmediate(isTestMode = false)
            }
        }
    }

    fun checkInSafe() {
        val current = _activeJourney.value ?: return
        val now = System.currentTimeMillis()
        current.nextCheckInAt = now + (current.checkInIntervalMinutes * 60 * 1000L).coerceAtMost(current.expiresAt - now)
        _activeJourney.value = current
        _isCheckInPending.value = false
        _checkInGraceSeconds.value = null
    }

    fun endJourney() {
        journeyJob?.cancel()
        journeyJob = null
        _activeJourney.value = null
        _isCheckInPending.value = false
        _checkInGraceSeconds.value = null
    }
}
