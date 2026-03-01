package com.example.colorboldnass.domain.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.colorboldnass.data.model.ColorBlindnessMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Класс для применения фильтров цветовой слепоты к изображениям с использованием GPU ускорения
class ColorBlindnessFilter {
    companion object {
        private val instance = ColorBlindnessFilter()
        fun getInstance() = instance
    }

    // Применить фильтр к битмапу с использованием ColorMatrix (очень быстро через Canvas)
    suspend fun applyFilter(bitmap: Bitmap, mode: ColorBlindnessMode): Bitmap = withContext(Dispatchers.Default) {
        if (mode == ColorBlindnessMode.NORMAL) {
            return@withContext bitmap
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(getColorMatrix(mode))
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        result
    }

    // Получить матрицу для фильтра (оптимизировано для sRGB пространства)
    fun getColorMatrix(mode: ColorBlindnessMode): ColorMatrix {
        val matrixValues = when (mode) {
            ColorBlindnessMode.NORMAL -> floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            ColorBlindnessMode.PROTANOPIA -> floatArrayOf(
                0.56667f, 0.43333f, 0f, 0f, 0f,
                0.55833f, 0.44167f, 0f, 0f, 0f,
                0f, 0.24167f, 0.75833f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            ColorBlindnessMode.DEUTERANOPIA -> floatArrayOf(
                0.625f, 0.375f, 0f, 0f, 0f,
                0.70f, 0.30f, 0f, 0f, 0f,
                0f, 0.30f, 0.70f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            ColorBlindnessMode.TRITANOPIA -> floatArrayOf(
                0.95f, 0.05f, 0f, 0f, 0f,
                0f, 0.43333f, 0.56667f, 0f, 0f,
                0f, 0.475f, 0.525f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            ColorBlindnessMode.ACHROMATOPSIA -> floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }
        return ColorMatrix(matrixValues)
    }

    // Для совместимости со старым кодом
    fun applyColorFilter(color: Int, mode: ColorBlindnessMode): Int {
        if (mode == ColorBlindnessMode.NORMAL) return color
        
        // Масштабируем компоненты цвета [0, 255] -> [0.0, 1.0]
        val a = (color shr 24 and 0xFF).toFloat()
        val r = (color shr 16 and 0xFF).toFloat()
        val g = (color shr 8 and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        
        val matrix = getColorMatrix(mode).array
        
        val nr = (matrix[0] * r + matrix[1] * g + matrix[2] * b + matrix[4]).toInt().coerceIn(0, 255)
        val ng = (matrix[5] * r + matrix[6] * g + matrix[7] * b + matrix[9]).toInt().coerceIn(0, 255)
        val nb = (matrix[10] * r + matrix[11] * g + matrix[12] * b + matrix[14]).toInt().coerceIn(0, 255)
        
        return (a.toInt() shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
}