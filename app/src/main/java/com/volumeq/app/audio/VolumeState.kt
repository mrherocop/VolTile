package com.volumeq.app.audio

data class ActiveAppAudio(
    val packageName: String,
    val appName: String,
    val volume: Int = 100 // 0 to 100%
)

data class VolumeState(
    val mediaVolume: Int = 0,
    val mediaMax: Int = 15,
    val ringVolume: Int = 0,
    val ringMax: Int = 7,
    val alarmVolume: Int = 0,
    val alarmMax: Int = 7,
    val notificationVolume: Int = 0,
    val notificationMax: Int = 7,
    val callVolume: Int = 0,
    val callMax: Int = 5,
    val isMediaMuted: Boolean = false,
    val isRingMuted: Boolean = false,
    val isAlarmMuted: Boolean = false,
    val isNotificationMuted: Boolean = false,
    val isCallMuted: Boolean = false,
    val ringerMode: Int = 2, // AudioManager.RINGER_MODE_NORMAL
    val activeApps: List<ActiveAppAudio> = emptyList()
)
