package com.example.colorboldnass.domain.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Репозиторий для работы с медиа-файлами
class MediaRepository(private val context: Context) {

    // Загрузить битмап из URI
    suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                // Проверка размера
                if (bitmap != null && bitmap.width * bitmap.height > 8000 * 8000) {
                    bitmap.recycle()
                    throw IllegalArgumentException("Изображение слишком большое")
                }
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Загрузить битмап из файла
    suspend fun loadBitmapFromFile(file: File): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            // Проверка размера
            if (bitmap != null && bitmap.width * bitmap.height > 8000 * 8000) {
                bitmap.recycle()
                throw IllegalArgumentException("Изображение слишком большое")
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Сохранить битмап в файл
    suspend fun saveBitmap(bitmap: Bitmap, fileName: String = "IMG_${System.currentTimeMillis()}.png"): File =
        withContext(Dispatchers.IO) {
            try {
                val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
                outputDir.mkdirs()
                val file = File(outputDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
                    outputStream.flush()
                }

                file
            } catch (e: Exception) {
                throw Exception("Ошибка сохранения файла: ${e.message}")
            }
        }

    // Получить директорию для сохранения медиа
    fun getMediaDirectory(): File {
        return try {
            val mediaDir = context.getExternalFilesDir(null) ?: context.filesDir
            mediaDir.mkdirs()
            mediaDir
        } catch (e: Exception) {
            context.filesDir
        }
    }

    // Очистить кеш директорию
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Получить размер файла в МБ
    fun getFileSizeMB(file: File): Double {
        return try {
            file.length() / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }
}
