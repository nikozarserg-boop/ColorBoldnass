package com.example.colorboldnass.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Утилита для оптимизации производительности приложения
 */
object PerformanceOptimizer {

    /**
     * Получить информацию о доступной памяти
     */
    fun getAvailableMemory(context: Context): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }

    /**
     * Получить количество доступных процессоров
     */
    fun getAvailableProcessors(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    /**
     * Проверить, работает ли приложение в режиме низкой памяти
     */
    fun isLowMemoryDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityManager?.isLowRamDevice == true
        } else {
            false
        }
    }

    /**
     * Получить рекомендуемый размер потока для обработки изображений
     */
    fun getOptimalThreadCount(): Int {
        val processors = getAvailableProcessors()
        return when {
            processors <= 2 -> 1    // Слабые устройства
            processors <= 4 -> 2    // Средние устройства
            processors <= 8 -> 3    // Сильные устройства
            else -> 4               // Очень сильные устройства
        }
    }

    /**
     * Рекомендуемый размер чанка для обработки пикселей
     */
    fun getOptimalChunkSize(totalPixels: Int): Int {
        return when {
            totalPixels < 1_000_000 -> totalPixels  // Обработать всё за раз
            totalPixels < 5_000_000 -> totalPixels / 2
            else -> totalPixels / 4
        }
    }
}
