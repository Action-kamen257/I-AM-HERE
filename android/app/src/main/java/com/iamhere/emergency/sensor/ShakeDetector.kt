package com.iamhere.emergency.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

enum class ShakeSensitivity(val thresholdG: Float, val reversalsRequired: Int, val windowMs: Long) {
    LOW(3.2f, 4, 700L),
    MEDIUM(2.7f, 3, 800L),
    HIGH(2.2f, 3, 900L)
}

/**
 * Shake and Impact Detector using hardware Accelerometer.
 * Features:
 * - Vigorous multi-reversal shake detection to filter out casual bumps.
 * - Freefall-then-impact fall detection (low-g followed by high-g spike).
 * - Continuous sample feeding into MotionFeatureExtractor for ML pipeline readiness.
 */
class ShakeDetector(
    private val onShakeDetected: () -> Unit,
    private val onImpactDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeDetector"
        private const val GRAVITY = 9.80665f
        private const val FREEFALL_THRESHOLD_G = 0.25f
        private const val IMPACT_THRESHOLD_G = 3.5f
        private const val FREEFALL_WINDOW_MS = 600L
        private const val COOLDOWN_MS = 3000L // 3s cooldown between triggers
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isListening = false

    var sensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM
    var isImpactDetectionEnabled: Boolean = true

    val featureExtractor = MotionFeatureExtractor()

    // Shake reversal tracking
    private var lastDirection = 0 // +1 or -1
    private var reversalCount = 0
    private var firstReversalTime = 0L
    private var lastTriggerTime = 0L

    // Impact / Fall tracking
    private var potentialFreefallStartTime = 0L
    private var freefallDetected = false

    fun start(context: Context): Boolean {
        if (isListening) return true

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.w(TAG, "Hardware Accelerometer not available on device")
            return false
        }

        // Validate ML pipeline upon start
        featureExtractor.validatePipeline()

        // SENSOR_DELAY_GAME provides ~50Hz sampling
        val registered = sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME) ?: false
        isListening = registered
        Log.i(TAG, "ShakeDetector started: $registered, sensitivity: $sensitivity")
        return registered
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        reversalCount = 0
        freefallDetected = false
        Log.i(TAG, "ShakeDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()

        // Feed ML feature extraction pipeline
        featureExtractor.addSample(x, y, z, now)

        val totalAccel = sqrt(x * x + y * y + z * z)
        val gForce = totalAccel / GRAVITY

        // Check for cooldown
        if (now - lastTriggerTime < COOLDOWN_MS) return

        // 1. Fall & Impact Detection
        if (isImpactDetectionEnabled) {
            checkImpact(gForce, now)
        }

        // 2. Vigorous Multi-Direction Shake Detection
        checkShake(x, y, z, gForce, now)
    }

    private fun checkImpact(gForce: Float, now: Long) {
        if (gForce < FREEFALL_THRESHOLD_G) {
            // Phone in free-fall
            if (potentialFreefallStartTime == 0L) {
                potentialFreefallStartTime = now
            } else if (now - potentialFreefallStartTime >= 120L) { // minimum 120ms weightlessness
                freefallDetected = true
            }
        } else {
            // Acceleration back above freefall threshold
            if (freefallDetected) {
                val timeSinceFreefall = now - potentialFreefallStartTime
                if (timeSinceFreefall <= FREEFALL_WINDOW_MS && gForce >= IMPACT_THRESHOLD_G) {
                    Log.w(TAG, "Hard Impact / Fall detected! gForce: $gForce after freefall")
                    lastTriggerTime = now
                    freefallDetected = false
                    potentialFreefallStartTime = 0L
                    onImpactDetected()
                    return
                }
            }

            if (now - potentialFreefallStartTime > FREEFALL_WINDOW_MS) {
                potentialFreefallStartTime = 0L
                freefallDetected = false
            }
        }
    }

    private fun checkShake(x: Float, y: Float, z: Float, gForce: Float, now: Long) {
        if (gForce >= sensitivity.thresholdG) {
            // Determine primary movement direction based on maximum component
            val currentDirection = when {
                kotlin.math.abs(x) >= kotlin.math.abs(y) && kotlin.math.abs(x) >= kotlin.math.abs(z) -> if (x > 0) 1 else -1
                kotlin.math.abs(y) >= kotlin.math.abs(x) && kotlin.math.abs(y) >= kotlin.math.abs(z) -> if (y > 0) 1 else -1
                else -> if (z > 0) 1 else -1
            }

            if (lastDirection != 0 && currentDirection != lastDirection) {
                // Direction reversal detected!
                if (reversalCount == 0) {
                    firstReversalTime = now
                }
                reversalCount++

                val elapsed = now - firstReversalTime
                if (elapsed <= sensitivity.windowMs) {
                    if (reversalCount >= sensitivity.reversalsRequired) {
                        Log.w(TAG, "Vigorous shake confirmed! Reversals: $reversalCount in ${elapsed}ms")
                        lastTriggerTime = now
                        reversalCount = 0
                        lastDirection = 0
                        onShakeDetected()
                        return
                    }
                } else {
                    // Window expired, restart count
                    reversalCount = 1
                    firstReversalTime = now
                }
            }
            lastDirection = currentDirection
        } else if (reversalCount > 0 && now - firstReversalTime > sensitivity.windowMs) {
            // Reset if no subsequent reversals within window
            reversalCount = 0
            lastDirection = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }
}
