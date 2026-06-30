package com.volumeq.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.volumeq.app.R
import com.volumeq.app.VolumeQApp

class VolumeService : Service() {

    companion object {
        const val CHANNEL_ID = "voltile_service_channel"
        const val NOTIFICATION_ID = 1001

        // Custom actions for Notification Panel interaction
        const val ACTION_VOLUME_UP = "com.volumeq.app.ACTION_VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.volumeq.app.ACTION_VOLUME_DOWN"
        const val ACTION_RINGER_TOGGLE = "com.volumeq.app.ACTION_RINGER_TOGGLE"
        const val ACTION_RINGER_SET = "com.volumeq.app.ACTION_RINGER_SET"
        const val ACTION_MEDIA_MUTE = "com.volumeq.app.ACTION_MEDIA_MUTE"

        const val EXTRA_STREAM_TYPE = "extra_stream_type"
        const val EXTRA_RINGER_MODE = "extra_ringer_mode"
    }

    private val am: AudioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val nm: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    // Dynamic receiver to catch system-wide volume and ringer changes in real-time
    private val systemVolumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == "android.media.VOLUME_CHANGED_ACTION" || 
                action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Register to receive real-time updates when physical buttons are pressed
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        registerReceiver(systemVolumeReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            handleNotificationAction(intent)
        }

        updateNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(systemVolumeReceiver) }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, VolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun handleNotificationAction(intent: Intent) {
        when (intent.action) {
            ACTION_VOLUME_UP -> {
                val stream = intent.getIntExtra(EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC)
                val current = am.getStreamVolume(stream)
                val max = am.getStreamMaxVolume(stream)
                if (current < max) {
                    am.setStreamVolume(stream, current + 1, AudioManager.FLAG_SHOW_UI)
                }
            }
            ACTION_VOLUME_DOWN -> {
                val stream = intent.getIntExtra(EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC)
                val current = am.getStreamVolume(stream)
                if (current > 0) {
                    am.setStreamVolume(stream, current - 1, AudioManager.FLAG_SHOW_UI)
                }
            }
            ACTION_RINGER_TOGGLE -> {
                val nextMode = when (am.ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                setRingerModeSafe(nextMode)
            }
            ACTION_RINGER_SET -> {
                val mode = intent.getIntExtra(EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
                setRingerModeSafe(mode)
            }
            ACTION_MEDIA_MUTE -> {
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (current > 0) {
                    // Mute
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                } else {
                    // Restore to 50%
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, max / 2, AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }

    private fun setRingerModeSafe(mode: Int) {
        runCatching {
            am.ringerMode = mode
        }.onFailure {
            // Can fail if Do Not Disturb access is required and not granted
            // We launch DND settings if it fails so the user can grant it
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun updateNotification() {
        val collapsedViews = RemoteViews(packageName, R.layout.notification_volume_panel_collapsed)
        val expandedViews = RemoteViews(packageName, R.layout.notification_volume_panel_expanded)

        // 1. Get current values
        val mediaVal = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaPct = (mediaVal * 100) / mediaMax

        val ringVal = am.getStreamVolume(AudioManager.STREAM_RING)
        val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val ringPct = (ringVal * 100) / ringMax

        val alarmVal = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val alarmPct = (alarmVal * 100) / alarmMax

        val callVal = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        val callMax = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)
        val callPct = (callVal * 100) / callMax

        // 2. Set progress/text in expanded view (SeekBar progress + % label)
        expandedViews.setProgressBar(R.id.seek_media, mediaMax, mediaVal, false)
        expandedViews.setTextViewText(R.id.tv_media_pct, "$mediaPct%")

        expandedViews.setProgressBar(R.id.seek_ring, ringMax, ringVal, false)
        expandedViews.setTextViewText(R.id.tv_ring_pct, "$ringPct%")

        expandedViews.setProgressBar(R.id.seek_alarm, alarmMax, alarmVal, false)
        expandedViews.setTextViewText(R.id.tv_alarm_pct, "$alarmPct%")

        expandedViews.setProgressBar(R.id.seek_call, callMax, callVal, false)
        expandedViews.setTextViewText(R.id.tv_call_pct, "$callPct%")

        // 3. Ringer Mode — update collapsed text + expanded button colors (SONIQ brutalist scheme)
        val ringerText = when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Sound On"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Silent"
        }
        collapsedViews.setTextViewText(R.id.ringer_status_text, ringerText)

        // SONIQ colors: active = green #22C55E, inactive = white #FFFFFF
        val activeModeBg  = Color.parseColor("#22C55E")
        val inactiveModeBg = Color.parseColor("#FFFFFF")

        expandedViews.setInt(R.id.btn_mode_sound, "setBackgroundColor",
            if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL) activeModeBg else inactiveModeBg)
        expandedViews.setInt(R.id.btn_mode_vibrate, "setBackgroundColor",
            if (am.ringerMode == AudioManager.RINGER_MODE_VIBRATE) activeModeBg else inactiveModeBg)
        expandedViews.setInt(R.id.btn_mode_silent, "setBackgroundColor",
            if (am.ringerMode == AudioManager.RINGER_MODE_SILENT) activeModeBg else inactiveModeBg)

        // 4. Set Pending Intents
        // Collapsed quick-toggle buttons
        collapsedViews.setOnClickPendingIntent(R.id.btn_quick_ringer, getServicePendingIntent(ACTION_RINGER_TOGGLE, 11))
        collapsedViews.setOnClickPendingIntent(R.id.btn_quick_media_mute, getServicePendingIntent(ACTION_MEDIA_MUTE, 12))

        // Expanded: "Open App" button
        val openIntent = Intent(this, VolumeQApp::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openPending = PendingIntent.getActivity(this, 100, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        expandedViews.setOnClickPendingIntent(R.id.btn_open_app, openPending)

        // Expanded: Ringer mode buttons
        expandedViews.setOnClickPendingIntent(R.id.btn_mode_sound, getServicePendingIntent(ACTION_RINGER_SET, 13, ringerMode = AudioManager.RINGER_MODE_NORMAL))
        expandedViews.setOnClickPendingIntent(R.id.btn_mode_vibrate, getServicePendingIntent(ACTION_RINGER_SET, 14, ringerMode = AudioManager.RINGER_MODE_VIBRATE))
        expandedViews.setOnClickPendingIntent(R.id.btn_mode_silent, getServicePendingIntent(ACTION_RINGER_SET, 15, ringerMode = AudioManager.RINGER_MODE_SILENT))

        // 5. Build and display the foreground notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_volume_outline)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getServicePendingIntent(
        action: String,
        requestCode: Int,
        streamType: Int? = null,
        ringerMode: Int? = null
    ): PendingIntent {
        val intent = Intent(this, VolumeService::class.java).apply {
            this.action = action
            streamType?.let { putExtra(EXTRA_STREAM_TYPE, it) }
            ringerMode?.let { putExtra(EXTRA_RINGER_MODE, it) }
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SONIQ Volume Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps SONIQ running in the background for instant QS Tile volume access"
                setShowBadge(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
