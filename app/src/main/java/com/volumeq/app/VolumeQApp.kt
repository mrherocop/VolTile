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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumeq.app.audio.VolumeState
import com.volumeq.app.service.VolumeService
import com.volumeq.app.ui.theme.*
import com.volumeq.app.viewmodel.VolumeViewModel

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
                SoniqMainScreen(
                    state          = state,
                    onMedia        = vm::setMediaVolume,
                    onRing         = vm::setRingVolume,
                    onAlarm        = vm::setAlarmVolume,
                    onNotification = vm::setNotificationVolume,
                    onCall         = vm::setCallVolume,
                    onRingerMode   = { vm.setRingerMode(it) },
                    onBatteryOpt   = ::requestBatteryOptimization,
                    onGitHub       = { openUrl("https://github.com/mrherocop/VolTile") },
                    onLinkedIn     = { openUrl("https://www.linkedin.com/") },
                    onInstagram    = { openUrl("https://www.instagram.com/") },
                )
            }
        }
    }

    private fun startVolumeService() {
        val i = Intent(this, VolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .apply { data = Uri.parse("package:$packageName") }
                )
            }.onFailure { startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}

// ─── Root Screen ─────────────────────────────────────────────
@Composable
fun SoniqMainScreen(
    state: VolumeState,
    onMedia: (Int) -> Unit,
    onRing: (Int) -> Unit,
    onAlarm: (Int) -> Unit,
    onNotification: (Int) -> Unit,
    onCall: (Int) -> Unit,
    onRingerMode: (Int) -> Unit,
    onBatteryOpt: () -> Unit,
    onGitHub: () -> Unit,
    onLinkedIn: () -> Unit,
    onInstagram: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lime),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            SoniqHeader()

            // ── Ringer Mode Buttons ──
            RingerModeSection(state.ringerMode, onRingerMode)

            // ── Volume Streams ──
            VolumeStreamsCard(state, onMedia, onRing, onAlarm, onCall, onNotification)

            // ── Link Buttons ──
            LinkButtonsRow(onGitHub, onLinkedIn, onInstagram)

            // ── Battery Button ──
            BatteryOptButton(onBatteryOpt)
        }
    }
}

// ─── Header ──────────────────────────────────────────────────
@Composable
fun SoniqHeader() {
    BrutalistCard(
        background = White,
        shadowOffset = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SONIQ",
                color = Black,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                letterSpacing = (-2).sp,
                lineHeight = 72.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Ringer Mode ─────────────────────────────────────────────
@Composable
fun RingerModeSection(currentMode: Int, onModeChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RING
        RingerModeButton(
            label = "RING",
            iconRes = R.drawable.ic_bell,
            background = Magenta,
            textColor = Black,
            selected = currentMode == AudioManager.RINGER_MODE_NORMAL,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(AudioManager.RINGER_MODE_NORMAL) }
        )
        // VIBRATE
        RingerModeButton(
            label = "VIBRATE",
            iconRes = R.drawable.ic_vibrate,
            background = Cyan,
            textColor = Black,
            selected = currentMode == AudioManager.RINGER_MODE_VIBRATE,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(AudioManager.RINGER_MODE_VIBRATE) }
        )
        // SILENT
        RingerModeButton(
            label = "SILENT",
            iconRes = R.drawable.ic_bell_off,
            background = Purple,
            textColor = White,
            selected = currentMode == AudioManager.RINGER_MODE_SILENT,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(AudioManager.RINGER_MODE_SILENT) }
        )
    }
}

@Composable
fun RingerModeButton(
    label: String,
    iconRes: Int,
    background: Color,
    textColor: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // When selected: translate(8,8) + no shadow (permanently pressed)
    // When not selected: full 8dp shadow, no translate
    val offsetDp = if (selected) 8.dp else 0.dp
    val shadowDp = if (selected) 0.dp else 8.dp

    Box(
        modifier = modifier
            .offset(x = offsetDp, y = offsetDp)
            .brutalistShadow(shadowDp)
            .border(4.dp, Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Volume Streams Card ─────────────────────────────────────
@Composable
fun VolumeStreamsCard(
    state: VolumeState,
    onMedia: (Int) -> Unit,
    onRing: (Int) -> Unit,
    onAlarm: (Int) -> Unit,
    onCall: (Int) -> Unit,
    onNotification: (Int) -> Unit,
) {
    BrutalistCard(background = White, shadowOffset = 8.dp) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SoniqSliderRow(
                iconRes = R.drawable.ic_music_note,
                currentVolume = state.mediaVolume,
                maxVolume = state.mediaMax,
                trackColor = SliderGreen,
                contentDescription = "Media volume",
                onValueChange = onMedia
            )
            SoniqSliderRow(
                iconRes = R.drawable.ic_ring_vol,
                currentVolume = state.ringVolume,
                maxVolume = state.ringMax,
                trackColor = SliderBlue,
                contentDescription = "Ring volume",
                onValueChange = onRing
            )
            SoniqSliderRow(
                iconRes = R.drawable.ic_alarm_clock,
                currentVolume = state.alarmVolume,
                maxVolume = state.alarmMax,
                trackColor = SliderOrange,
                contentDescription = "Alarm volume",
                onValueChange = onAlarm
            )
            SoniqSliderRow(
                iconRes = R.drawable.ic_phone,
                currentVolume = state.callVolume,
                maxVolume = state.callMax,
                trackColor = SliderLightBlue,
                contentDescription = "Call volume",
                onValueChange = onCall
            )
            SoniqSliderRow(
                iconRes = R.drawable.ic_notif,
                currentVolume = state.notificationVolume,
                maxVolume = state.notificationMax,
                trackColor = SliderGreen,
                contentDescription = "Notification volume",
                onValueChange = onNotification
            )
        }
    }
}

@Composable
fun SoniqSliderRow(
    iconRes: Int,
    currentVolume: Int,
    maxVolume: Int,
    trackColor: Color,
    contentDescription: String,
    onValueChange: (Int) -> Unit,
) {
    val pct = if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon box: #CCFF00 with 4px black border, 56×56
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(4.dp, Black)
                .background(Lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Black,
                modifier = Modifier.size(28.dp)
            )
        }

        // Slider + percentage
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Slider(
                value = currentVolume.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..maxVolume.toFloat().coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Black,
                    activeTrackColor = trackColor,
                    inactiveTrackColor = White,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                )
            )
            Text(
                text = "$pct%",
                color = Black,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}

// ─── Link Buttons Row ────────────────────────────────────────
@Composable
fun LinkButtonsRow(
    onGitHub: () -> Unit,
    onLinkedIn: () -> Unit,
    onInstagram: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinkButton("GITHUB",    Cyan,    Modifier.weight(1f), onGitHub)
        LinkButton("LINKEDIN",  Magenta, Modifier.weight(1f), onLinkedIn)
        LinkButton("INSTAGRAM", Color(0xFFFFFF00), Modifier.weight(1f), onInstagram)
    }
}

@Composable
fun LinkButton(
    label: String,
    hoverColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val offsetDp = if (pressed) 8.dp else 0.dp
    val shadowDp = if (pressed) 0.dp else 8.dp

    Box(
        modifier = modifier
            .offset(x = offsetDp, y = offsetDp)
            .brutalistShadow(shadowDp)
            .border(4.dp, Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = !pressed
                onClick()
            }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Black,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
        )
    }
}

// ─── Battery Button ──────────────────────────────────────────
@Composable
fun BatteryOptButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val offsetDp = if (pressed) 8.dp else 0.dp
    val shadowDp = if (pressed) 0.dp else 8.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetDp, y = offsetDp)
            .brutalistShadow(shadowDp)
            .border(4.dp, Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(BatteryOrange)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = !pressed
                onClick()
            }
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_outline),
                contentDescription = null,
                tint = Black,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "DISABLE BATTERY OPTIMISATION",
                color = Black,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Brutalist Card ──────────────────────────────────────────
@Composable
fun BrutalistCard(
    background: Color = White,
    shadowOffset: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .brutalistShadow(shadowOffset)
            .border(4.dp, Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(background)
    ) {
        content()
    }
}

// ─── Shadow Modifier ─────────────────────────────────────────
// Hard-edge neo-brutalist black shadow: draws a solid black rectangle
// offset by (offset, offset) below/right of the component, exactly as the
// CSS `box-shadow: 8px 8px 0px 0px rgba(0,0,0,1)` does in the HTML.
fun Modifier.brutalistShadow(offset: Dp = 8.dp): Modifier =
    this
        .padding(bottom = offset, end = offset)
        .drawBehind {
            val offsetPx = offset.toPx()
            drawRect(
                color = ShadowBlack,
                topLeft = androidx.compose.ui.geometry.Offset(offsetPx, offsetPx),
                size = Size(size.width, size.height)
            )
        }
