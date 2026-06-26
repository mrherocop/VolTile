package com.volumeq.app.tile

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.volumeq.app.VolumeQApp
import com.volumeq.app.service.VolumeService

class VolumeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val mediaVal = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaPct = (mediaVal * 100) / mediaMax

        // Tile state is active if any stream has volume, inactive if all streams are muted (0)
        val isAnyActive = mediaVal > 0 ||
                am.getStreamVolume(AudioManager.STREAM_RING) > 0 ||
                am.getStreamVolume(AudioManager.STREAM_ALARM) > 0 ||
                am.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0 ||
                am.getStreamVolume(AudioManager.STREAM_VOICE_CALL) > 0

        tile.state = if (isAnyActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Media: $mediaPct%"
        }
        tile.updateTile()

        ensureServiceRunning()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        ensureServiceRunning()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, VolumeQApp::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun ensureServiceRunning() {
        val serviceIntent = Intent(this, VolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
