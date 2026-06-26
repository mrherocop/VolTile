package com.volumeq.app.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioRepository(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val appVolumeMap = mutableMapOf<String, Int>()

    private val _volumeState = MutableStateFlow(VolumeState())
    val volumeState: StateFlow<VolumeState> = _volumeState.asStateFlow()

    init {
        updateState()
    }

    fun updateState() {
        val activeAppsList = getActiveAudioApps()
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
                ringerMode = audioManager.ringerMode,
                activeApps = activeAppsList
            )
        }
    }

    fun setStreamVolume(streamType: Int, volume: Int) {
        audioManager.setStreamVolume(streamType, volume, AudioManager.FLAG_SHOW_UI)
        updateState()
    }

    fun setRingerMode(mode: Int) {
        audioManager.ringerMode = mode
        updateState()
    }

    fun setAppVolume(packageName: String, volume: Int) {
        appVolumeMap[packageName] = volume
        updateState()
    }

    private fun getActiveAudioApps(): List<ActiveAppAudio> {
        val list = mutableListOf<ActiveAppAudio>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configs = audioManager.activePlaybackConfigurations
            val pm = context.packageManager
            for (config in configs) {
                // Use reflection to call hidden getClientUid() method on AudioPlaybackConfiguration
                val uid = runCatching {
                    val method = config.javaClass.getMethod("getClientUid")
                    method.invoke(config) as Int
                }.getOrNull() ?: continue

                val packages = pm.getPackagesForUid(uid)
                if (!packages.isNullOrEmpty()) {
                    val pkgName = packages[0]
                    if (pkgName == context.packageName) continue

                    val appLabel = runCatching {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(pkgName)

                    val vol = appVolumeMap[pkgName] ?: 100
                    if (list.none { it.packageName == pkgName }) {
                        list.add(ActiveAppAudio(pkgName, appLabel, vol))
                    }
                }
            }
        }
        return list
    }
}
