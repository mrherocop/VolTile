package com.volumeq.app.viewmodel

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import com.volumeq.app.audio.AudioRepository
import com.volumeq.app.audio.VolumeState
import kotlinx.coroutines.flow.StateFlow

class VolumeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioRepository(application)
    val volumeState: StateFlow<VolumeState> = repository.volumeState

    /** Call this from the Activity's onResume so state refreshes if another app changed volume */
    fun refresh() {
        repository.updateState()
    }

    fun setMediaVolume(volume: Int) {
        repository.setStreamVolume(AudioManager.STREAM_MUSIC, volume)
    }

    fun setRingVolume(volume: Int) {
        repository.setStreamVolume(AudioManager.STREAM_RING, volume)
    }

    fun setAlarmVolume(volume: Int) {
        repository.setStreamVolume(AudioManager.STREAM_ALARM, volume)
    }

    fun setNotificationVolume(volume: Int) {
        repository.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume)
    }

    fun setCallVolume(volume: Int) {
        repository.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume)
    }

    fun toggleRingerMode() {
        val current = volumeState.value.ringerMode
        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        repository.setRingerMode(next)
    }
}
