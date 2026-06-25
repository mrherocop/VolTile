package com.volumeq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.volumeq.app.ui.VolumeBottomSheet
import com.volumeq.app.ui.theme.VolumeQTheme
import com.volumeq.app.viewmodel.VolumeViewModel

class VolumeQApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VolumeQTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    val viewModel: VolumeViewModel = viewModel()
                    val state by viewModel.volumeState.collectAsState()
                    
                    VolumeBottomSheet(
                        state = state,
                        onMediaVolumeChange = viewModel::setMediaVolume,
                        onRingVolumeChange = viewModel::setRingVolume,
                        onAlarmVolumeChange = viewModel::setAlarmVolume,
                        onNotificationVolumeChange = viewModel::setNotificationVolume,
                        onCallVolumeChange = viewModel::setCallVolume,
                        onDismissRequest = { finish() }
                    )
                }
            }
        }
    }
}
