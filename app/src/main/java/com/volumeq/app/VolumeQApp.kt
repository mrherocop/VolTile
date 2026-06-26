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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumeq.app.audio.VolumeState
import com.volumeq.app.service.VolumeService
import com.volumeq.app.ui.theme.VolumeQTheme
import com.volumeq.app.viewmodel.VolumeViewModel

// ─── Color Palette ───────────────────────────────────────────
private val BgDeep   = Color(0xFF060914)
private val BgCard   = Color(0xFF0E1120)
private val BgStrip  = Color(0xFF141830)
private val Border   = Color(0xFF1E2240)
private val AccBlue  = Color(0xFF5B7BFF)
private val AccGreen = Color(0xFF3DD68C)
private val AccRed   = Color(0xFFFF5F6D)
private val AccAmber = Color(0xFFFFB347)
private val TextHi   = Color(0xFFEEEEFF)
private val TextMid  = Color(0xFF8890BB)
private val TextLow  = Color(0xFF454870)

// Per-stream accent colours
private val StreamColors = mapOf(
    "Media"        to Color(0xFF5B7BFF),
    "Ring"         to Color(0xFFBB6BFF),
    "Alarm"        to Color(0xFFFF8C42),
    "Notification" to Color(0xFFFFD166),
    "Call"         to Color(0xFF3DD68C),
)

// ─── Activity ────────────────────────────────────────────────
class VolumeQApp : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startVolumeService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            VolumeQTheme {
                val vm: VolumeViewModel = viewModel()
                val state by vm.volumeState.collectAsState()
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                DisposableEffect(lifecycle) {
                    val obs = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME) vm.refresh()
                    }
                    lifecycle.addObserver(obs)
                    onDispose { lifecycle.removeObserver(obs) }
                }
                MainScreen(
                    state = state,
                    onMedia        = vm::setMediaVolume,
                    onRing         = vm::setRingVolume,
                    onAlarm        = vm::setAlarmVolume,
                    onNotification = vm::setNotificationVolume,
                    onCall         = vm::setCallVolume,
                    onRingerToggle = vm::toggleRingerMode,
                    onBatteryOpt   = ::requestBatteryOptimization
                )
            }
        }
    }

    private fun startVolumeService() {
        val i = Intent(this, VolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .apply { data = Uri.parse("package:$packageName") }
                )
            }.onFailure {
                startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            }
        }
    }
}

// ─── Root Screen ─────────────────────────────────────────────
@Composable
fun MainScreen(
    state: VolumeState,
    onMedia: (Int) -> Unit,
    onRing: (Int) -> Unit,
    onAlarm: (Int) -> Unit,
    onNotification: (Int) -> Unit,
    onCall: (Int) -> Unit,
    onRingerToggle: () -> Unit,
    onBatteryOpt: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Decorative glow blob top-right
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 100.dp, y = (-60).dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(listOf(AccBlue.copy(alpha = .15f), Color.Transparent)),
                    CircleShape
                )
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            HeaderRow()
            Spacer(Modifier.height(20.dp))
            RingerModeRow(state.ringerMode, onRingerToggle)
            Spacer(Modifier.height(20.dp))
            VolumeCard(state, onMedia, onRing, onAlarm, onNotification, onCall)
            Spacer(Modifier.height(16.dp))
            HowToCard()
            Spacer(Modifier.height(12.dp))
            BatteryButton(onBatteryOpt)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Header ──────────────────────────────────────────────────
@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon circle
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF2A3BCC), AccBlue))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "VolTile",
                color = TextHi,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Volume Manager",
                color = TextLow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))
        PulseDot()
    }
}

@Composable
fun PulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot pulse"
    )
    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(34.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(AccGreen.copy(alpha = .15f))
        )
        // Inner dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AccGreen)
        )
    }
}

// ─── Ringer Mode ─────────────────────────────────────────────
@Composable
fun RingerModeRow(mode: Int, onToggle: () -> Unit) {
    data class ModeOption(val label: String, val icon: ImageVector, val value: Int, val color: Color)
    val options = listOf(
        ModeOption("Sound",   Icons.Outlined.VolumeUp,   AudioManager.RINGER_MODE_NORMAL,  AccGreen),
        ModeOption("Vibrate", Icons.Outlined.Vibration,  AudioManager.RINGER_MODE_VIBRATE, AccBlue),
        ModeOption("Silent",  Icons.Outlined.VolumeOff,  AudioManager.RINGER_MODE_SILENT,  AccRed),
    )

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "RINGER MODE",
                color = TextLow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { opt ->
                    val active = mode == opt.value
                    val bgColor by animateColorAsState(
                        if (active) opt.color.copy(alpha = .18f) else Color.Transparent,
                        label = "ringer bg"
                    )
                    val borderColor by animateColorAsState(
                        if (active) opt.color else Border,
                        label = "ringer border"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    // Cycle until we land on this option
                                    if (mode != opt.value) onToggle()
                                }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                opt.icon,
                                contentDescription = opt.label,
                                tint = if (active) opt.color else TextLow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                opt.label,
                                color = if (active) opt.color else TextLow,
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Volume Card ─────────────────────────────────────────────
@Composable
fun VolumeCard(
    state: VolumeState,
    onMedia: (Int) -> Unit, onRing: (Int) -> Unit, onAlarm: (Int) -> Unit,
    onNotification: (Int) -> Unit, onCall: (Int) -> Unit,
) {
    GlassCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                "VOLUME STREAMS",
                color = TextLow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            VolumeRow(Icons.Outlined.MusicNote,        "Media",        state.mediaVolume,        state.mediaMax,        onMedia)
            VolumeRow(Icons.Outlined.RingVolume,       "Ring",         state.ringVolume,         state.ringMax,         onRing)
            VolumeRow(Icons.Outlined.Alarm,            "Alarm",        state.alarmVolume,        state.alarmMax,        onAlarm)
            VolumeRow(Icons.Outlined.NotificationsNone,"Notification", state.notificationVolume, state.notificationMax, onNotification)
            VolumeRow(Icons.Outlined.Call,             "Call",         state.callVolume,         state.callMax,         onCall)
        }
    }
}

@Composable
fun VolumeRow(
    icon: ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    val accent = StreamColors[label] ?: AccBlue
    val isMuted = value == 0
    val pct = if (max > 0) (value * 100f / max).toInt() else 0

    val trackColor by animateColorAsState(
        if (isMuted) TextLow.copy(alpha = .3f) else accent,
        animationSpec = tween(300),
        label = "$label track"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with background circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMuted) BgStrip else accent.copy(alpha = .15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onValueChange(if (isMuted) max / 2 else 0)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = "$label mute toggle",
                tint = if (isMuted) TextLow else accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = if (isMuted) TextLow else TextMid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$pct%",
                    color = if (isMuted) TextLow else accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..max.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth().height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = trackColor,
                    activeTrackColor = trackColor,
                    inactiveTrackColor = Border,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                )
            )
        }
    }
}

// ─── How-To Card ─────────────────────────────────────────────
@Composable
fun HowToCard() {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = AccBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "QUICK SETTINGS TILE",
                    color = TextLow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            listOf(
                "Swipe down twice to open Quick Settings.",
                "Tap the edit/pencil icon.",
                "Drag the VolTile tile into your panel.",
                "Tap the tile anytime to open this screen."
            ).forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AccBlue.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            color = AccBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(step, color = TextMid, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Battery Button ──────────────────────────────────────────
@Composable
fun BatteryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.BatteryChargingFull,
                contentDescription = null,
                tint = AccAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Disable Battery Optimisation",
                color = AccAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Shared Card Component ────────────────────────────────────
@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}
