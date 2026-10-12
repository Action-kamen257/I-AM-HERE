package com.iamhere.emergency.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iamhere.emergency.MainActivity
import com.iamhere.emergency.data.repository.EmergencyManager

class PowerButtonSosReceiver : BroadcastReceiver() {

    companion object {
        private val toggleTimestamps = mutableListOf<Long>()
        private const val REQUIRED_TOGGLES = 4
        private const val WINDOW_MS = 3000L // 3 seconds window
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_SCREEN_OFF) {
            val now = System.currentTimeMillis()

            synchronized(toggleTimestamps) {
                toggleTimestamps.add(now)
                // Filter out timestamps older than WINDOW_MS
                toggleTimestamps.removeAll { now - it > WINDOW_MS }

                if (toggleTimestamps.size >= REQUIRED_TOGGLES) {
                    toggleTimestamps.clear()

                    val manager = EmergencyManager.getInstance()
                    if (!manager.isEmergencyActive.value) {
                        manager.triggerSosImmediate(isTestMode = false)

                        // Launch MainActivity over lock screen
                        context?.let { ctx ->
                            val mainIntent = Intent(ctx, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            ctx.startActivity(mainIntent)
                        }
                    }
                }
            }
        }
    }
}
