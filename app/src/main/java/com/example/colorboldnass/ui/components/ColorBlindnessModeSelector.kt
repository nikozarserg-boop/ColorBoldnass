package com.example.colorboldnass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.colorboldnass.data.model.ColorBlindnessMode

// Селектор режимов цветовой слепоты
@Composable
fun ColorBlindnessModeSelector(
    currentMode: ColorBlindnessMode,
    modes: List<ColorBlindnessMode>,
    onModeSelected: (ColorBlindnessMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { mode ->
            val isSelected = currentMode == mode
            
            Button(
                onClick = { 
                    onModeSelected(mode) 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Селектор режимов съёмки (фото/видео)
@Composable
fun CaptureModeSelector(
    isPhotoMode: Boolean,
    onPhotoModeClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPhotoModeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPhotoMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isPhotoMode)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Снимок")
        }

        Button(
            onClick = onVideoModeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isPhotoMode)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (!isPhotoMode)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Видео")
        }
    }
}
