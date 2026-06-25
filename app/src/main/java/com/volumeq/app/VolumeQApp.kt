package com.volumeq.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumeq.app.service.VolumeService
import com.volumeq.app.ui.theme.VolumeQTheme
import com.volumeq.app.viewmodel.VolumeViewModel

class VolumeQApp : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Always start the background service
        startVolumeService()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            VolumeQTheme {
                val viewModel: VolumeViewModel = viewModel()
                val state by viewModel.volumeState.collectAsState()
                MainScreen(
                    state = state,
                    onMediaVolumeChange = viewModel::setMediaVolume,
                    onRingVolumeChange = viewModel::setRingVolume,
                    onAlarmVolumeChange = viewModel::setAlarmVolume,
                    onNotificationVolumeChange = viewModel::setNotificationVolume,
                    onCallVolumeChange = viewModel::setCallVolume,
                    onRequestBatteryOptimization = { requestBatteryOptimization() }
                )
            }
        }
    }

    private fun startVolumeService() {
        val intent = Intent(this, VolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

@Composable
fun MainScreen(
    state: com.volumeq.app.audio.VolumeState,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onNotificationVolumeChange: (Int) -> Unit,
    onCallVolumeChange: (Int) -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    val background = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D0D1A), Color(0xFF12122A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = "VolTile",
                    tint = Color(0xFF7B8FFF),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "VolTile",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B2C6B)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Always On",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Background service active · Use the Quick Settings tile anytime",
                color = Color(0xFF666899),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Volume sliders card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141428),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Volume Controls",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    VolumeSliderRow(
                        icon = Icons.Outlined.PlayArrow,
                        label = "Media",
                        value = state.mediaVolume,
                        max = state.mediaMax,
                        onValueChange = onMediaVolumeChange
                    )
                    VolumeSliderRow(
                        icon = Icons.Outlined.Notifications,
                        label = "Ring",
                        value = state.ringVolume,
                        max = state.ringMax,
                        onValueChange = onRingVolumeChange
                    )
                    VolumeSliderRow(
                        icon = Icons.Outlined.Alarm,
                        label = "Alarm",
                        value = state.alarmVolume,
                        max = state.alarmMax,
                        onValueChange = onAlarmVolumeChange
                    )
                    VolumeSliderRow(
                        icon = Icons.Outlined.NotificationsActive,
                        label = "Notification",
                        value = state.notificationVolume,
                        max = state.notificationMax,
                        onValueChange = onNotificationVolumeChange
                    )
                    VolumeSliderRow(
                        icon = Icons.Outlined.Call,
                        label = "Call",
                        value = state.callVolume,
                        max = state.callMax,
                        onValueChange = onCallVolumeChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // How to use card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141428),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Access Tile",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Swipe down twice from the top of your screen.\n2. Tap the edit/pencil icon in Quick Settings.\n3. Find and drag the VolTile tile into your panel.\n4. Tap it anytime to control all your volumes instantly.",
                        color = Color(0xFF888AAA),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Battery optimisation button
            OutlinedButton(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B8FFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7B8FFF))
            ) {
                Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disable Battery Optimisation (for 24/7 service)")
            }
        }
    }
}

@Composable
fun VolumeSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    val isMuted = value == 0
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
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
                modifier = Modifier.width(80.dp)
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
}
