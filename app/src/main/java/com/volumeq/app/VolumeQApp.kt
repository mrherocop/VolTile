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

// ─── SONIQ Neo-Brutalist Colors (Inline to match v2.1 pattern) ────────
private val Lime            = Color(0xFFCCFF00)
private val Black           = Color(0xFF000000)
private val White           = Color(0xFFFFFFFF)
private val Magenta         = Color(0xFFFF00FF)
private val Cyan            = Color(0xFF00FFFF)
private val Purple          = Color(0xFF7000FF)
private val SliderGreen     = Color(0xFF22C55E)
private val SliderBlue      = Color(0xFF1D4ED8)
private val SliderOrange    = Color(0xFFEA580C)
private val SliderLightBlue = Color(0xFF93C5FD)
private val BatteryOrange   = Color(0xFFFF5500)
private val ShadowBlack     = Color(0xFF000000)

// ─── Activity ────────────────────────────────────────────────
class VolumeQApp : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("crash_log", throwable.stackTraceToString()).commit()
            defaultHandler?.uncaughtException(thread, throwable) ?: kotlin.system.exitProcess(1)
        }

        val prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        val existingCrash = prefs.getString("crash_log", null)

        if (existingCrash != null) {
            setContent {
                VolumeQTheme {
                    Surface(color = Color.Red, modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text("FATAL CRASH DETECTED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Please copy the text below and send it to me:", color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            Button(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(existingCrash))
                            }) {
                                Text("Copy Log to Clipboard", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(existingCrash, color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { 
                                prefs.edit().remove("crash_log").apply()
                                // Restart the activity to try again
                                startActivity(Intent(this@VolumeQApp, VolumeQApp::class.java))
                                finish()
                            }) {
                                Text("Clear Crash Log & Restart")
                            }
                        }
                    }
                }
            }
            return // Do not start the service or show the rest of the app!
        }

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
            }.onFailure { startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}

// ─── Main Screen (SONIQ Brutalist Port) ──────────────────────
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
                .fillMaxHeight()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            SoniqHeader()

            // ── Ringer Mode Selection ──
            RingerModeSection(
                currentMode = state.ringerMode,
                onModeChange = onRingerMode
            )

            // ── Volume Streams ──
            VolumeStreamsCard(
                state = state,
                onMedia = onMedia,
                onRing = onRing,
                onAlarm = onAlarm,
                onNotification = onNotification,
                onCall = onCall
            )

            // ── Social Links ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialButton("GITHUB", Color(0xFF5B7BFF), Modifier.weight(1f), onGitHub)
                SocialButton("LINKEDIN", Magenta, Modifier.weight(1f), onLinkedIn)
                SocialButton("INSTAGRAM", Color(0xFFFFFF00), Modifier.weight(1f), onInstagram)
            }

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
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
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
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
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
    onNotification: (Int) -> Unit,
    onCall: (Int) -> Unit,
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
                iconRes = R.drawable.ic_open_new,
                currentVolume = state.notificationVolume,
                maxVolume = state.notificationMax,
                trackColor = SliderGreen,
                contentDescription = "Notification volume",
                onValueChange = onNotification
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoniqSliderRow(
    iconRes: Int,
    currentVolume: Int,
    maxVolume: Int,
    trackColor: Color,
    contentDescription: String,
    onValueChange: (Int) -> Unit
) {
    val pct = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Lime)
                .border(4.dp, Black),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Black,
                modifier = Modifier.size(32.dp)
            )
        }

        // Slider component
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .border(3.dp, Black)
                    .background(White)
            ) {
                // Track fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f)
                        .background(trackColor)
                )
            }

            // Slider thumb logic wrapped inside standard slider for interaction
            Slider(
                value = currentVolume.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..(maxVolume.toFloat().coerceAtLeast(1f)),
                steps = (maxVolume - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Black)
                            .drawBehind {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.75f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(4.dp.toPx(), 4.dp.toPx())
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    topLeft = Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                    size = Size(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                    )
                }
            )
        }

        // Percentage text
        Text(
            text = "$pct%",
            color = Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

// ─── Buttons ─────────────────────────────────────────────────
@Composable
fun SocialButton(
    text: String,
    hoverBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BrutalistCard(background = White, shadowOffset = 8.dp, modifier = modifier) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BatteryOptButton(onClick: () -> Unit) {
    BrutalistCard(background = BatteryOrange, shadowOffset = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alarm),
                contentDescription = null,
                tint = Black,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "DISABLE BATTERY OPTIMISATION",
                color = Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Modifiers ───────────────────────────────────────────────
fun Modifier.brutalistShadow(offset: Dp = 8.dp): Modifier =
    this
        .padding(bottom = offset, end = offset)
        .drawBehind {
            val offsetPx = offset.toPx()
            drawRect(
                color = ShadowBlack,
                topLeft = Offset(offsetPx, offsetPx),
                size = Size(size.width, size.height)
            )
        }

@Composable
fun BrutalistCard(
    background: Color,
    shadowOffset: Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .brutalistShadow(shadowOffset)
            .border(4.dp, Black, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(background)
    ) {
        content()
    }
}
