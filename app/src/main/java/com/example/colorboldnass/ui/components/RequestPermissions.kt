package com.example.colorboldnass.ui.components

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Компонент для запроса разрешений
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(onPermissionsResult: (Boolean) -> Unit) {
    // Список необходимых разрешений
    val permissions = remember {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    // Состояние для управления разрешениями
    val permissionState = rememberMultiplePermissionsState(permissions) { permissions ->
        val allGranted = permissions.all { it.value }
        onPermissionsResult(allGranted)
    }

    // Запрос разрешений при запуске (только один раз)
    LaunchedEffect(Unit) {
        if (permissionState.allPermissionsGranted) {
            // Если разрешения уже выданы
            onPermissionsResult(true)
        } else if (!permissionState.shouldShowRationale) {
            // Если нужно запросить разрешения
            permissionState.launchMultiplePermissionRequest()
        }
    }
}
