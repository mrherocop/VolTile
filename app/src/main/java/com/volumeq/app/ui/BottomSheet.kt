package com.volumeq.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.volumeq.app.audio.VolumeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeBottomSheet(
    state: VolumeState,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onNotificationVolumeChange: (Int) -> Unit,
    onCallVolumeChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Volume",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            
            SliderRow(
                icon = Icons.Outlined.PlayArrow,
                label = "Media",
                value = state.mediaVolume,
                max = state.mediaMax,
                onValueChange = onMediaVolumeChange
            )
            
            SliderRow(
                icon = Icons.Outlined.Notifications,
                label = "Ring",
                value = state.ringVolume,
                max = state.ringMax,
                onValueChange = onRingVolumeChange
            )
            
            SliderRow(
                icon = Icons.Outlined.Alarm,
                label = "Alarm",
                value = state.alarmVolume,
                max = state.alarmMax,
                onValueChange = onAlarmVolumeChange
            )
            
            SliderRow(
                icon = Icons.Outlined.NotificationsActive,
                label = "Notification",
                value = state.notificationVolume,
                max = state.notificationMax,
                onValueChange = onNotificationVolumeChange
            )
            
            SliderRow(
                icon = Icons.Outlined.Call,
                label = "Call",
                value = state.callVolume,
                max = state.callMax,
                onValueChange = onCallVolumeChange
            )
        }
    }
}
