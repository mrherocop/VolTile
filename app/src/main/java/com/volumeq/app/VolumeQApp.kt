package com.volumeq.app

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumeq.app.audio.VolumeState
import com.volumeq.app.service.VolumeService
import com.volumeq.app.ui.theme.VolumeQTheme
import com.volumeq.app.viewmodel.VolumeViewModel

class VolumeQApp : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the background service is running every time the app opens
        startVolumeService()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            VolumeQTheme {
                val vm: VolumeViewModel = viewModel()
                val state by vm.volumeState.collectAsState()

                // Refresh volume state whenever the app comes back to the foreground
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                MainScreen(
                    state = state,
                    onMediaVolumeChange = vm::setMediaVolume,
                    onRingVolumeChange = vm::setRingVolume,
                    onAlarmVolumeChange = vm::setAlarmVolume,
                    onNotificationVolumeChange = vm::setNotificationVolume,
                    onCallVolumeChange = vm::setCallVolume,
                    onToggleRingerMode = vm::toggleRingerMode,
                    onRequestBatteryOptimization = ::requestBatteryOptimization
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
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }.onFailure {
                // Fallback: open battery settings page if direct intent is blocked
                startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Main Screen
// ──────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    state: VolumeState,
    onMediaVolumeChange: (Int) -> Unit,
    onRingVolumeChange: (Int) -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onNotificationVolumeChange: (Int) -> Unit,
    onCallVolumeChange: (Int) -> Unit,
    onToggleRingerMode: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF0F0F22))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF7B8FFF),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "VolTile",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.weight(1f))
                // Always-On pill
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF1A2E1A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Active",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "24/7 background service · tap the QS tile anytime",
                color = Color(0xFF555577),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ── Ringer Mode Row ──────────────────────────────
            RingerModeCard(
                ringerMode = state.ringerMode,
                onToggle = onToggleRingerMode
            )

            Spacer(Modifier.height(16.dp))

            // ── Volume Controls Card ─────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Volume Controls",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )

                    VolumeRow(
                        icon = Icons.Outlined.MusicNote,
                        label = "Media",
                        value = state.mediaVolume,
                        max = state.mediaMax,
                        onValueChange = onMediaVolumeChange
                    )
                    HorizontalDivider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
                    VolumeRow(
                        icon = Icons.Outlined.RingVolume,
                        label = "Ring",
                        value = state.ringVolume,
                        max = state.ringMax,
                        onValueChange = onRingVolumeChange
                    )
                    HorizontalDivider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
                    VolumeRow(
                        icon = Icons.Outlined.Alarm,
                        label = "Alarm",
                        value = state.alarmVolume,
                        max = state.alarmMax,
                        onValueChange = onAlarmVolumeChange
                    )
                    HorizontalDivider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
                    VolumeRow(
                        icon = Icons.Outlined.NotificationsNone,
                        label = "Notif.",
                        value = state.notificationVolume,
                        max = state.notificationMax,
                        onValueChange = onNotificationVolumeChange
                    )
                    HorizontalDivider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
                    VolumeRow(
                        icon = Icons.Outlined.Call,
                        label = "Call",
                        value = state.callVolume,
                        max = state.callMax,
                        onValueChange = onCallVolumeChange
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick Settings Tile Guide ────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF7B8FFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Quick Settings Tile",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Swipe down twice to open Quick Settings.",
                        "Tap the pencil/edit icon.",
                        "Drag the VolTile tile into your panel.",
                        "Tap the tile anytime to open this screen."
                    ).forEachIndexed { i, step ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                "${i + 1}.",
                                color = Color(0xFF7B8FFF),
                                fontSize = 13.sp,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(step, color = Color(0xFF888AAA), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Battery Optimisation Button ──────────────────
            OutlinedButton(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B8FFF)),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                )
            ) {
                Icon(
                    Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Disable Battery Optimisation (for 24/7)")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Ringer Mode Card
// ──────────────────────────────────────────────────────────────

@Composable
fun RingerModeCard(ringerMode: Int, onToggle: () -> Unit) {
    val (icon, label, tint) = when (ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> Triple(Icons.Outlined.Vibration, "Vibrate", Color(0xFF7B8FFF))
        AudioManager.RINGER_MODE_SILENT -> Triple(Icons.Outlined.VolumeOff, "Silent", Color(0xFFFF6B6B))
        else -> Triple(Icons.Outlined.VolumeUp, "Sound On", Color(0xFF4CAF50))
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ringer Mode", color = Color(0xFF888AAA), fontSize = 11.sp)
                Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(
                onClick = onToggle,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF1E1E3A),
                    contentColor = Color(0xFF7B8FFF)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Switch", fontSize = 12.sp)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Volume Row with mute-toggle button + slider + percentage
// ──────────────────────────────────────────────────────────────

@Composable
fun VolumeRow(
    icon: ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    val isMuted = value == 0
    val fraction = if (max > 0) value.toFloat() / max.toFloat() else 0f
    val pct = (fraction * 100).toInt()

    // Animate the slider value for smoother visual feedback
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        label = "$label slider animation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute / unmute button – tapping icon toggles mute
        IconButton(
            onClick = {
                if (isMuted) onValueChange(max / 2) else onValueChange(0)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isMuted) "Unmute $label" else "Mute $label",
                tint = if (isMuted) Color(0xFF444466) else Color(0xFF7B8FFF),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(2.dp))

        // Label
        Text(
            text = label,
            color = if (isMuted) Color(0xFF555577) else Color(0xFFCCCCEE),
            fontSize = 13.sp,
            modifier = Modifier.width(46.dp)
        )

        // Slider
        Slider(
            value = animatedValue,
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..max.toFloat().coerceAtLeast(1f),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = if (isMuted) Color(0xFF444466) else Color(0xFF7B8FFF),
                activeTrackColor = if (isMuted) Color(0xFF333355) else Color(0xFF7B8FFF),
                inactiveTrackColor = Color(0xFF202038)
            )
        )

        Spacer(Modifier.width(8.dp))

        // Percentage label – fixed width so layout doesn't jump
        Text(
            text = "$pct%",
            color = if (isMuted) Color(0xFF444466) else Color(0xFF888AAA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(34.dp)
        )
    }
}
