package com.example.colorboldnass.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Вспомогательный класс для работы с разрешениями на MIUI устройствах.
 * MIUI имеет более строгие ограничения на доступ к камере.
 */
object MiuiPermissionHelper {

    // Кэш результата проверки разрешения для оптимизации
    private var permissionCheckCache: Pair<Long, Boolean>? = null
    private const val CACHE_DURATION_MS = 5000 // Кэш на 5 секунд

    /**
     * Проверить, может ли приложение использовать камеру на MIUI.
     * Возвращает true если разрешение явно выдано, не just "один раз".
     */
    fun isCameraPermissionGrantedPermanently(context: Context): Boolean {
        return try {
            // Проверяем кэш
            val currentTime = System.currentTimeMillis()
            permissionCheckCache?.let { (cacheTime, result) ->
                if (currentTime - cacheTime < CACHE_DURATION_MS) {
                    return result
                }
            }

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appOpsManager = ContextCompat.getSystemService(context, AppOpsManager::class.java)
                val mode = appOpsManager?.checkOpNoThrow(
                    "android:camera",
                    android.os.Process.myUid(),
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } else {
                true // На старых версиях проверка невозможна
            }
            
            // Сохраняем в кэш
            permissionCheckCache = Pair(currentTime, result)
            result
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получить описание текущего статуса разрешения камеры.
     */
    fun getCameraPermissionStatus(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appOpsManager = ContextCompat.getSystemService(context, AppOpsManager::class.java)
                val mode = appOpsManager?.checkOpNoThrow(
                    "android:camera",
                    android.os.Process.myUid(),
                    context.packageName
                )
                when (mode) {
                    AppOpsManager.MODE_ALLOWED -> "[ОК] Разрешено постоянно"
                    AppOpsManager.MODE_IGNORED -> "[БЛОК] Заблокировано системой"
                    AppOpsManager.MODE_ERRORED -> "[ОШИБКА] Ошибка доступа"
                    AppOpsManager.MODE_DEFAULT -> "[ТРЕБУЕТСЯ] Требуется разрешение"
                    else -> "[НЕИЗВЕСТНО] Неизвестный статус ($mode)"
                }
            } else {
                "Android ${Build.VERSION.SDK_INT}: проверка невозможна"
            }
        } catch (e: Exception) {
            "Ошибка проверки: ${e.localizedMessage}"
        }
    }

    /**
     * Очистить кэш разрешения (вызывается при переходе в настройки приложения)
     */
    fun clearPermissionCache() {
        permissionCheckCache = null
    }
}
