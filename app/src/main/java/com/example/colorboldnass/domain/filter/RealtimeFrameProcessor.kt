package com.example.colorboldnass.domain.filter

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.colorboldnass.data.model.ColorBlindnessMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private const val TAG = "RealtimeFrameProcessor"

/**
 * Обработчик кадров камеры в реальном времени.
 * Преобразует YUV420 кадры из камеры в RGB и применяет фильтры цветовой слепоты.
 */
class RealtimeFrameProcessor(
    private val onFrameProcessed: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentColorMode: ColorBlindnessMode = ColorBlindnessMode.NORMAL
    private var lastProcessedTime = 0L
    private val processIntervalMs = 100 // Обрабатываем каждый ~100ms кадр для оптимизации

    fun setColorMode(mode: ColorBlindnessMode) {
        currentColorMode = mode
    }

    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < processIntervalMs) {
            imageProxy.close()
            return
        }
        lastProcessedTime = currentTime

        scope.launch {
            try {
                val bitmap = imageProxy.toBitmap()

                if (bitmap != null) {
                    val processedBitmap = if (currentColorMode != ColorBlindnessMode.NORMAL) {
                        ColorBlindnessFilter.getInstance().applyFilter(bitmap, currentColorMode)
                    } else {
                        bitmap
                    }

                    onFrameProcessed(processedBitmap)

                    if (processedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обработке кадра: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
        }
    }
}

fun ImageProxy.toBitmap(): Bitmap? {
    val format = format
    if (format != ImageFormat.YUV_420_888) {
        Log.e(TAG, "Unsupported image format: $format")
        return null
    }

    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}