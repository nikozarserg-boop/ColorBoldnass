package com.example.colorboldnass.ui.screen

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.colorboldnass.ui.viewmodel.MainViewModel

// Главный экран приложения с управлением навигацией
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    // Требуемые разрешения для приложения камеры
    // НЕ требуются разрешения на хранилище для сохранения в галерею (Android 10+)
    val requiredPermissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
    }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = requiredPermissions
    )

    val isOnCameraScreen = remember { mutableStateOf(true) }

    // Проверка разрешений при первой загрузке
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Проверка всех разрешений
        if (permissionsState.allPermissionsGranted) {
            // Основной контент приложения
            if (isOnCameraScreen.value) {
                CameraScreen(
                    viewModel = viewModel,
                    lifecycleOwner = lifecycleOwner,
                    onNavigateToGallery = { isOnCameraScreen.value = false }
                )
            } else {
                GalleryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { isOnCameraScreen.value = true }
                )
            }
        } else {
            // Экран требования разрешений
            PermissionRequiredScreen(
                onRequestPermissions = {
                    permissionsState.launchMultiplePermissionRequest()
                }
            )
        }
    }
}

// Экран для запроса разрешений
@Composable
private fun PermissionRequiredScreen(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Требуются разрешения") },
            text = {
                Text(
                    "Для работы приложения требуются следующие разрешения:\n" +
                    "• Доступ к камере\n" +
                    "• Запись звука\n\n" +
                    "Это необходимо для съёмки фото и видео.\n" +
                    "Фото и видео автоматически сохраняются в галерею.\n\n" +
                    "ВАЖНО: При запросе разрешений выберите\n" +
                    "\"Разрешить\", \"Всегда разрешить\",\n" +
                    "или \"Allow\"\n" +
                    "(не \"один раз\" и не \"только в приложении\")\n\n" +
                    "Если разрешения были отклонены ранее:\n" +
                    "Откройте Настройки → Приложения → ColorBoldnass → Разрешения\n" +
                    "и разрешите доступ к Камере и Микрофону.\n\n" +
                    "Пожалуйста, предоставьте разрешения для продолжения."
                )
            },
            confirmButton = {
                Button(onClick = onRequestPermissions) {
                    Text("Предоставить разрешения")
                }
            }
        )
    }
}
