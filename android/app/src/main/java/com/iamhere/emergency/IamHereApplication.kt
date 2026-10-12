package com.iamhere.emergency

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build

class IamHereApplication : Application() {

    companion object {
        const val EMERGENCY_CHANNEL_ID = "emergency_sos_channel"
        lateinit var instance: IamHereApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_emergency_name)
            val descriptionText = getString(R.string.channel_emergency_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(EMERGENCY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false)
                vibrationPattern = null
                setSound(null, null)
                setBypassDnd(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
