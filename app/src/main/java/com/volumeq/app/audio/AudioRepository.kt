package com.volumeq.app.audio

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioRepository(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _volumeState = MutableStateFlow(VolumeState())
    val volumeState: StateFlow<VolumeState> = _volumeState.asStateFlow()

    init {
        updateState()
    }

    fun updateState() {
        _volumeState.update {
            VolumeState(
                mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING),
                ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
                alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
                notificationMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                callVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                callMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                ringerMode = audioManager.ringerMode
            )
        }
    }

    fun setStreamVolume(streamType: Int, volume: Int) {
        // FLAG_SHOW_UI shows the system volume bar so the user gets visual feedback
        audioManager.setStreamVolume(streamType, volume, AudioManager.FLAG_SHOW_UI)
        updateState()
    }

    fun setRingerMode(mode: Int) {
        audioManager.ringerMode = mode
        updateState()
    }
}
