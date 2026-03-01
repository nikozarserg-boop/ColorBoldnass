package com.example.colorboldnass.ui.screen

import android.content.Context
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.video.FileOutputOptions
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.colorboldnass.data.model.CaptureMode
import com.example.colorboldnass.data.model.ColorBlindnessMode
import com.example.colorboldnass.domain.camera.CameraManager
import com.example.colorboldnass.domain.filter.ColorBlindnessFilter
import com.example.colorboldnass.ui.components.CameraPreview
import com.example.colorboldnass.ui.components.CaptureModeSelector
import com.example.colorboldnass.ui.components.ColorBlindnessModeSelector
import com.example.colorboldnass.ui.components.ErrorSnackbar
import com.example.colorboldnass.ui.components.PhotoButton
import com.example.colorboldnass.ui.components.RecordButton
import com.example.colorboldnass.ui.components.SuccessSnackbar
import com.example.colorboldnass.ui.viewmodel.MainViewModel
import com.example.colorboldnass.util.MiuiPermissionHelper
import kotlinx.coroutines.delay
import androidx.camera.video.VideoRecordEvent
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

// Экран камеры
@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    lifecycleOwner: LifecycleOwner,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorBlindnessMode by viewModel.colorBlindnessMode.collectAsState()
    val captureMode by viewModel.captureMode.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val allModes by remember { mutableStateOf(viewModel.getAllColorBlindnessModes()) }
    val scope = rememberCoroutineScope()

    // Инициализируем камеру
    val cameraManager = remember { CameraManager(context) }
    var isCameraReady by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successText by remember { mutableStateOf("") }
    var currentRecording by remember { mutableStateOf<androidx.camera.video.Recording?>(null) }

    // Переменные для управления зумом
    var zoomRatio by remember { mutableStateOf(1f) }
    var currentZoomState by remember { mutableStateOf<ZoomState?>(null) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    // Наблюдение за состоянием зума через LiveData
    DisposableEffect(isCameraReady) {
        val zoomLiveData = cameraManager.getZoomState()
        val observer = Observer<ZoomState> { state ->
            currentZoomState = state
            zoomRatio = state.zoomRatio
        }
        
        zoomLiveData?.observe(lifecycleOwner, observer)
        
        onDispose {
            zoomLiveData?.removeObserver(observer)
        }
    }

    // Таймер для скрытия индикатора зума
    LaunchedEffect(zoomRatio) {
        if (isCameraReady) {
            showZoomIndicator = true
            delay(500)
            showZoomIndicator = false
        }
    }

    // Скрыть сообщение об успехе через 3 секунды
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(3000)
            showSuccessMessage = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isCameraReady) {
                if (!isCameraReady) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    val minZoom = currentZoomState?.minZoomRatio ?: 1f
                    val maxZoom = currentZoomState?.maxZoomRatio ?: 8f
                    
                    val newZoomRatio = (zoomRatio * zoom).coerceIn(minZoom, maxZoom)
                    if (newZoomRatio != zoomRatio) {
                        zoomRatio = newZoomRatio
                        cameraManager.setZoom(newZoomRatio)
                    }
                }
            }
    ) {
        // Предпросмотр камеры
         CameraPreview(
             modifier = Modifier
                 .fillMaxSize()
                 .align(Alignment.Center),
             onSurfaceProviderReady = { surfaceProvider ->
                 cameraManager.setupCamera(
                     lifecycleOwner = lifecycleOwner,
                     previewSurfaceProvider = surfaceProvider,
                     onSuccess = { 
                         android.util.Log.d("CameraScreen", "Камера успешно инициализирована")
                         isCameraReady = true 
                     },
                     onError = { error ->
                         android.util.Log.e("CameraScreen", "Ошибка камеры: ${error.message}", error)
                         viewModel.setErrorMessage("Ошибка инициализации камеры: ${error.message}")
                     }
                 )
             },
             isLoading = !isCameraReady
         )

        // Верхняя панель - выбор режима цветовой слепоты
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Режим цветовой слепоты:",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            ColorBlindnessModeSelector(
                currentMode = colorBlindnessMode,
                modes = allModes,
                onModeSelected = { viewModel.setColorBlindnessMode(it) }
            )
        }

        // Нижняя панель с управлением
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(12.dp)
        ) {
            // Обновляем фильтр в камере при изменении режима
            LaunchedEffect(colorBlindnessMode) {
                cameraManager.currentColorBlindnessMode = colorBlindnessMode
                android.util.Log.d("CameraScreen", "Фильтр изменен на: ${colorBlindnessMode.displayName}")
            }

            // Выбор режима съёмки
             CaptureModeSelector(
                 isPhotoMode = captureMode == CaptureMode.PHOTO,
                 onPhotoModeClick = { viewModel.setCaptureMode(CaptureMode.PHOTO) },
                 onVideoModeClick = { viewModel.setCaptureMode(CaptureMode.VIDEO) },
                 modifier = Modifier.fillMaxWidth()
             )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопки действия
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(horizontal = 12.dp),
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 // Кнопка загрузки из галереи
                 Button(
                     onClick = onNavigateToGallery,
                     colors = ButtonDefaults.buttonColors(
                         containerColor = Color(0xFF2196F3)
                     ),
                     shape = RoundedCornerShape(12.dp),
                     modifier = Modifier
                         .size(60.dp)
                         .padding(4.dp)
                 ) {
                     Icon(
                         imageVector = Icons.Default.Image,
                         contentDescription = "Открыть галерею",
                         tint = Color.White
                     )
                 }

                 Spacer(modifier = Modifier.weight(1f))

                 // Основная кнопка снимка/записи
                 if (captureMode == CaptureMode.PHOTO) {
                     PhotoButton(
                         onClick = {
                             handleTakePhoto(context, cameraManager, viewModel, scope)
                         },
                         enabled = isCameraReady
                     )
                 } else {
                     RecordButton(
                         isRecording = isRecording,
                         onClick = {
                             handleVideoRecord(
                                 context,
                                 cameraManager,
                                 viewModel,
                                 isRecording,
                                 currentRecording,
                                 { newRecording -> currentRecording = newRecording },
                                 { success -> 
                                     if (!success) {
                                         successText = "Видео сохранено"
                                         showSuccessMessage = true
                                     }
                                 }
                             )
                         },
                         enabled = isCameraReady
                     )
                 }

                 Spacer(modifier = Modifier.weight(1f))

                 // Пустое место для симметрии
                 Box(modifier = Modifier.size(60.dp))
             }

            if (isRecording && captureMode == CaptureMode.VIDEO) {
                Text(
                    text = "[ЗАПИСЬ] Идет запись видео...",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Индикатор текущего зума с плавной анимацией
        AnimatedVisibility(
            visible = showZoomIndicator && zoomRatio > 1.01f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${"%.1f".format(zoomRatio)}x",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Отображение сообщений
        if (showSuccessMessage) {
            SuccessSnackbar(
                message = if (successText.isEmpty()) "Видео сохранено успешно" else successText,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp),
                isVisible = showSuccessMessage
            )
        }

        if (!errorMessage.isNullOrEmpty()) {
            ErrorSnackbar(
                message = errorMessage ?: "Неизвестная ошибка",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp),
                isVisible = !errorMessage.isNullOrEmpty()
            )
        }
    }
}

// Вспомогательная функция для снятия фото
private fun handleTakePhoto(
     context: Context,
     cameraManager: CameraManager?,
     viewModel: MainViewModel,
     scope: kotlinx.coroutines.CoroutineScope
) {
     cameraManager?.let { manager ->
          try {
               val outputDir = manager.getOutputDirectory()
               if (!outputDir.exists() || !outputDir.canWrite()) {
                   viewModel.setErrorMessage("Ошибка: нет доступа к директории сохранения")
                   return@let
               }
               
               val outputFile = java.io.File(outputDir, manager.getOutputFileName())

               manager.takePhoto(
                   outputFile = outputFile,
                   executor = ContextCompat.getMainExecutor(context),
                   onSuccess = { file ->
                       scope.launch {
                           // Применяем фильтр цветовой слепоты к фото
                           try {
                               val currentMode = viewModel.colorBlindnessMode.value
                               if (currentMode != ColorBlindnessMode.NORMAL) {
                                   val originalBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                   if (originalBitmap != null) {
                                       val filteredBitmap = ColorBlindnessFilter.getInstance().applyFilter(originalBitmap, currentMode)
                                       java.io.FileOutputStream(file).use { fos ->
                                           filteredBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                                           fos.flush()
                                       }
                                       originalBitmap.recycle()
                                       filteredBitmap.recycle()
                                   }
                               }
                           } catch (e: Exception) {
                               android.util.Log.e("CameraScreen", "Ошибка применения фильтра к фото: ${e.message}")
                           }
                           viewModel.setErrorMessage("Фото сохранено: ${file.name}")
                       }
                   },
                   onError = { error ->
                       viewModel.setErrorMessage("Ошибка при снятии фото: ${error.message}")
                   }
               )
          } catch (e: Exception) {
               viewModel.setErrorMessage("Ошибка: ${e.message}")
          }
     } ?: run {
          viewModel.setErrorMessage("Ошибка: камера не инициализирована")
     }
}

// Вспомогательная функция для видеозаписи
private fun handleVideoRecord(
    context: Context,
    cameraManager: CameraManager?,
    viewModel: MainViewModel,
    isRecording: Boolean,
    currentRecording: androidx.camera.video.Recording?,
    onRecordingStarted: (androidx.camera.video.Recording) -> Unit,
    onRecordingFinished: (Boolean) -> Unit
) {
    cameraManager?.let { manager ->
        try {
            val videoCapture = manager.getVideoCapture() ?: run {
                viewModel.setErrorMessage("Ошибка: видеозапись не инициализирована")
                return@let
            }

            if (isRecording) {
                currentRecording?.stop()
            } else {
                val outputDir = manager.getVideoOutputDirectory()
                if (!outputDir.exists() || !outputDir.canWrite()) {
                    viewModel.setErrorMessage("Ошибка: нет доступа к директории сохранения видео")
                    return@let
                }

                val outputFile = java.io.File(outputDir, manager.getVideoFileName())

                try {
                    val pendingRecording = videoCapture.output.prepareRecording(
                        context,
                        androidx.camera.video.FileOutputOptions.Builder(outputFile).build()
                    )

                    val recording = pendingRecording
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(context)) { videoRecordEvent ->
                            when (videoRecordEvent) {
                                is VideoRecordEvent.Start -> {
                                    viewModel.setRecording(true)
                                }
                                is VideoRecordEvent.Finalize -> {
                                    viewModel.setRecording(false)
                                    if (videoRecordEvent.hasError()) {
                                        viewModel.setErrorMessage("Ошибка видеозаписи: ${videoRecordEvent.cause?.message}")
                                    } else {
                                        viewModel.setErrorMessage("Видео сохранено: ${outputFile.name}")
                                    }
                                }
                                else -> {}
                            }
                        }
                    onRecordingStarted(recording)
                } catch (e: Exception) {
                    viewModel.setErrorMessage("Ошибка инициализации видеозаписи: ${e.message}")
                    viewModel.setRecording(false)
                }
            }
        } catch (e: Exception) {
            viewModel.setErrorMessage("Ошибка видеозаписи: ${e.message}")
            viewModel.setRecording(false)
        }
    }
}
