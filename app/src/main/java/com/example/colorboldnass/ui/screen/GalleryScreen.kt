package com.example.colorboldnass.ui.screen

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.colorboldnass.data.model.ColorBlindnessMode
import com.example.colorboldnass.domain.filter.ColorBlindnessFilter
import com.example.colorboldnass.ui.components.ColorBlindnessModeSelector
import com.example.colorboldnass.ui.components.ErrorSnackbar
import com.example.colorboldnass.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.ExperimentalMaterial3Api

// Экран галереи с выбором и редактированием файлов
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorBlindnessMode by viewModel.colorBlindnessMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val allModes by remember { mutableStateOf(viewModel.getAllColorBlindnessModes()) }
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Лаунчер для выбора изображения
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            selectedImageUri = selectedUri
            scope.launch {
                try {
                    isLoading = true
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmapFromUri(context, selectedUri)
                    }
                    selectedBitmap = bitmap
                     // Применяем текущий фильтр к загруженному изображению
                     processedBitmap = bitmap?.let {
                         ColorBlindnessFilter.getInstance().applyFilter(it, colorBlindnessMode)
                     }
                } catch (e: Exception) {
                    viewModel.setErrorMessage("Ошибка загрузки изображения: ${e.message}")
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхняя панель
        TopAppBar(
            title = { Text("Редактор изображений") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопка выбора изображения
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Загрузить изображение",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Выбрать изображение")
            }

            // Блок для выбранного изображения
             val bitmap = processedBitmap
             if (bitmap != null) {
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(300.dp)
                         .background(
                             color = Color.DarkGray,
                             shape = RoundedCornerShape(12.dp)
                         )
                         .padding(8.dp)
                 ) {
                     if (isLoading) {
                         CircularProgressIndicator(
                             modifier = Modifier.align(Alignment.Center),
                             color = MaterialTheme.colorScheme.primary
                         )
                     } else {
                         Image(
                             bitmap = bitmap.asImageBitmap(),
                             contentDescription = "Обработанное изображение",
                             modifier = Modifier.fillMaxSize(),
                             contentScale = ContentScale.Fit
                         )
                     }
                 }

                // Селектор режимов цветовой слепоты
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Выберите режим фильтра:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    ColorBlindnessModeSelector(
                        currentMode = colorBlindnessMode,
                        modes = allModes,
                        onModeSelected = { mode ->
                            viewModel.setColorBlindnessMode(mode)
                            // Применяем новый фильтр к изображению на фоновом потоке
                            scope.launch {
                                selectedBitmap?.let { bitmap ->
                                    isLoading = true
                                    try {
                                        processedBitmap = withContext(Dispatchers.Default) {
                                            ColorBlindnessFilter.getInstance().applyFilter(bitmap, mode)
                                        }
                                    } catch (e: Exception) {
                                        viewModel.setErrorMessage("Ошибка применения фильтра: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    )
                }

                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val bitmapToSave = processedBitmap
                            if (bitmapToSave != null) {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        val savedFile = withContext(Dispatchers.IO) {
                                            saveBitmapToFile(context, bitmapToSave)
                                        }
                                        viewModel.setErrorMessage("Изображение сохранено: ${savedFile.name}")
                                    } catch (e: Exception) {
                                        viewModel.setErrorMessage("Ошибка при сохранении: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                viewModel.setErrorMessage("Нет изображения для сохранения")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("СОХРАНИТЬ")
                    }

                    Button(
                        onClick = {
                            selectedImageUri = null
                            selectedBitmap = null
                            processedBitmap = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ОЧИСТИТЬ")
                    }
                }
            } else {
                // Прозрачный блок когда ничего не выбрано
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Изображение не выбрано",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Отображение ошибок
        if (!errorMessage.isNullOrEmpty()) {
            ErrorSnackbar(
                message = errorMessage ?: "Неизвестная ошибка",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 80.dp),
                isVisible = !errorMessage.isNullOrEmpty()
            )
        }
    }
}

// Функция загрузки битмапа из URI
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
     return try {
         context.contentResolver.openInputStream(uri)?.use { inputStream ->
             val bitmap = BitmapFactory.decodeStream(inputStream)
             if (bitmap == null) {
                 throw Exception("Не удалось декодировать изображение")
             }
             // Проверка размера
             if (bitmap.width * bitmap.height > 8000 * 8000) {
                 throw Exception("Изображение слишком большое")
             }
             bitmap
         }
     } catch (e: Exception) {
         null
     }
}

// Функция сохранения битмапа в галерею
private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
     val fileName = "IMG_${System.currentTimeMillis()}.png"
     
     // На Android 10+ используем MediaStore для сохранения в галерею
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         val contentValues = ContentValues().apply {
             put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
             put(MediaStore.Images.Media.MIME_TYPE, "image/png")
             put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ColorBoldnass")
         }
         
         val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
         if (uri != null) {
             context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                 bitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
                 outputStream.flush()
             }
             // Возвращаем фиксированный файл для совместимости
             return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
                        "ColorBoldnass/$fileName")
         }
     }
     
     // Fallback для Android 9 и ниже
     val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
     val outputDir = File(picturesDir, "ColorBoldnass")
     outputDir.mkdirs()
     val file = File(outputDir, fileName)
     
     file.outputStream().use { outputStream ->
         bitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
         outputStream.flush()
     }
     
     return file
}
