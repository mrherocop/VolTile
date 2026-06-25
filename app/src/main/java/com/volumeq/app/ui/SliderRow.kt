package com.volumeq.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SliderRow(
    icon: ImageVector,
    label: String,
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..max.toFloat(),
            steps = if (max > 1) max - 1 else 0,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = if (max > 0) "${((value.toFloat() / max.toFloat()) * 100).roundToInt()}%" else "0%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp)
        )
    }
}
