package com.volumeq.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Always expand fully - no partial expand
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141428),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFF444466), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF7B8FFF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Volume",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            SheetSliderRow(
                icon = Icons.Outlined.PlayArrow,
                label = "Media",
                value = state.mediaVolume,
                max = state.mediaMax,
                onValueChange = onMediaVolumeChange
            )
            SheetSliderRow(
                icon = Icons.Outlined.Notifications,
                label = "Ring",
                value = state.ringVolume,
                max = state.ringMax,
                onValueChange = onRingVolumeChange
            )
            SheetSliderRow(
                icon = Icons.Outlined.Alarm,
                label = "Alarm",
                value = state.alarmVolume,
                max = state.alarmMax,
                onValueChange = onAlarmVolumeChange
            )
            SheetSliderRow(
                icon = Icons.Outlined.NotificationsActive,
                label = "Notification",
                value = state.notificationVolume,
                max = state.notificationMax,
                onValueChange = onNotificationVolumeChange
            )
            SheetSliderRow(
                icon = Icons.Outlined.Call,
                label = "Call",
                value = state.callVolume,
                max = state.callMax,
                onValueChange = onCallVolumeChange
            )
        }
    }
}

@Composable
fun SheetSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    val isMuted = value == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute/unmute icon button
        IconButton(
            onClick = { onValueChange(if (isMuted) max / 2 else 0) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label mute toggle",
                tint = if (isMuted) Color(0xFF555577) else Color(0xFF7B8FFF),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color(0xFFCCCCEE),
            fontSize = 13.sp,
            modifier = Modifier.width(76.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..max.toFloat().coerceAtLeast(1f),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF7B8FFF),
                activeTrackColor = Color(0xFF7B8FFF),
                inactiveTrackColor = Color(0xFF2A2A4A)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (max > 0) "${((value.toFloat() / max.toFloat()) * 100).toInt()}%" else "0%",
            color = Color(0xFF888AAA),
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}
