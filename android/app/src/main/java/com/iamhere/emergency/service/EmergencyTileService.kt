package com.iamhere.emergency.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.iamhere.emergency.MainActivity
import com.iamhere.emergency.data.repository.EmergencyManager

@RequiresApi(Build.VERSION_CODES.N)
class EmergencyTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val manager = EmergencyManager.getInstance()

        if (manager.isEmergencyActive.value) {
            // Already active - launch app to permit safe PIN cancellation
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(appIntent)
        } else {
            // Instant SOS trigger directly from Quick Settings tile
            manager.triggerSosImmediate(isTestMode = false)
            updateTileState()

            // Open app on top of lock screen
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(appIntent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = EmergencyManager.getInstance().isEmergencyActive.value

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "SOS ACTIVE"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "I AM HERE SOS"
        }
        tile.updateTile()
    }
}
