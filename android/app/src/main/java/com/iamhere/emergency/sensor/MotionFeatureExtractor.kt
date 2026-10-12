package com.iamhere.emergency.sensor

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Feature Extraction Pipeline for Motion & Shake Dynamics.
 * Adheres strictly to project training rule:
 * Prepares and validates feature vectors for machine learning classification.
 * Does NOT trigger automated ML training. Reports when manual training is ready.
 */
class MotionFeatureExtractor(val windowSize: Int = 64) {

    companion object {
        private const val TAG = "MotionFeatureExtractor"
        const val FEATURE_COUNT = 10
    }

    data class MotionSample(
        val x: Float,
        val y: Float,
        val z: Float,
        val timestampMs: Long
    ) {
        val magnitude: Float
            get() = sqrt(x * x + y * y + z * z)
    }

    private val sampleBuffer = ArrayDeque<MotionSample>(windowSize)

    fun addSample(x: Float, y: Float, z: Float, timestampMs: Long = System.currentTimeMillis()) {
        if (sampleBuffer.size >= windowSize) {
            sampleBuffer.removeFirst()
        }
        sampleBuffer.addLast(MotionSample(x, y, z, timestampMs))
    }

    fun isWindowFull(): Boolean = sampleBuffer.size >= windowSize

    /**
     * Extracts 10-dimensional statistical and frequency proxy feature vector:
     * 0: Mean magnitude (m/s^2)
     * 1: StdDev magnitude
     * 2: Peak-to-Peak (Max - Min magnitude)
     * 3: Mean X
     * 4: Mean Y
     * 5: Mean Z
     * 6: Energy proxy (variance sum)
     * 7: Zero-Crossing Rate around mean magnitude
     * 8: Max absolute acceleration jerk
     * 9: Dominant axis index (0=X, 1=Y, 2=Z)
     */
    fun extractFeatures(): FloatArray? {
        if (sampleBuffer.size < windowSize) return null
        val samples = sampleBuffer.toList()
        return computeFeatures(samples)
    }

    fun computeFeatures(samples: List<MotionSample>): FloatArray {
        val n = samples.size.toFloat()
        val magnitudes = samples.map { it.magnitude }

        val meanMag = magnitudes.sum() / n
        val meanX = samples.sumOf { it.x.toDouble() }.toFloat() / n
        val meanY = samples.sumOf { it.y.toDouble() }.toFloat() / n
        val meanZ = samples.sumOf { it.z.toDouble() }.toFloat() / n

        val varianceMag = magnitudes.map { (it - meanMag).pow(2) }.sum() / n
        val stdDevMag = sqrt(varianceMag)

        val minMag = magnitudes.minOrNull() ?: 0f
        val maxMag = magnitudes.maxOrNull() ?: 0f
        val peakToPeak = maxMag - minMag

        // Zero-crossing rate around mean magnitude
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            val prevDiff = magnitudes[i - 1] - meanMag
            val currDiff = magnitudes[i] - meanMag
            if ((prevDiff > 0 && currDiff < 0) || (prevDiff < 0 && currDiff > 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / (samples.size - 1)

        // Jerk approximation
        var maxJerk = 0f
        for (i in 1 until samples.size) {
            val dt = (samples[i].timestampMs - samples[i - 1].timestampMs).coerceAtLeast(1L) / 1000f
            val djerk = kotlin.math.abs(magnitudes[i] - magnitudes[i - 1]) / dt
            if (djerk > maxJerk) maxJerk = djerk
        }

        // Dominant axis
        val sumAbsX = samples.sumOf { kotlin.math.abs(it.x.toDouble()) }
        val sumAbsY = samples.sumOf { kotlin.math.abs(it.y.toDouble()) }
        val sumAbsZ = samples.sumOf { kotlin.math.abs(it.z.toDouble()) }
        val dominantAxis = when {
            sumAbsX >= sumAbsY && sumAbsX >= sumAbsZ -> 0f
            sumAbsY >= sumAbsX && sumAbsY >= sumAbsZ -> 1f
            else -> 2f
        }

        return floatArrayOf(
            meanMag,
            stdDevMag,
            peakToPeak,
            meanX,
            meanY,
            meanZ,
            varianceMag,
            zcr,
            maxJerk,
            dominantAxis
        )
    }

    /**
     * Validates the feature pipeline with synthetic samples.
     * Complies with training rule: Never automatically start training.
     * Prepare and validate pipeline, then report that manual training is ready.
     */
    fun validatePipeline(): Boolean {
        val syntheticSamples = mutableListOf<MotionSample>()
        val baseTime = System.currentTimeMillis()
        for (i in 0 until windowSize) {
            val t = i * 20L // 50 Hz
            val x = (kotlin.math.sin(i * 0.2) * 15.0).toFloat()
            val y = (kotlin.math.cos(i * 0.2) * 5.0).toFloat()
            val z = 9.8f + (kotlin.math.sin(i * 0.4) * 8.0).toFloat()
            syntheticSamples.add(MotionSample(x, y, z, baseTime + t))
        }

        val features = computeFeatures(syntheticSamples)
        val isValid = features.size == FEATURE_COUNT && !features.any { it.isNaN() || it.isInfinite() }

        if (isValid) {
            Log.i(TAG, "[MotionMLPipeline] Feature extraction pipeline validated successfully. Dimensions: ${features.size}. Pipeline ready for manual model training.")
        } else {
            Log.e(TAG, "[MotionMLPipeline] Feature extraction validation failed.")
        }
        return isValid
    }
}
