package com.example.colorboldnass.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Утилиты для адаптивного дизайна
object ResponsiveUtils {

    // Получить ширину экрана в dp
    @Composable
    fun getScreenWidth(): Dp {
        return LocalConfiguration.current.screenWidthDp.dp
    }

    // Получить высоту экрана в dp
    @Composable
    fun getScreenHeight(): Dp {
        return LocalConfiguration.current.screenHeightDp.dp
    }

    // Проверить, является ли устройство планшетом
    @Composable
    fun isTablet(): Boolean {
        val screenWidth = getScreenWidth().value
        return screenWidth >= 600f
    }

    // Получить размер padding в зависимости от ширины экрана
    @Composable
    fun getResponsivePadding(): Dp {
        return if (isTablet()) 24.dp else 16.dp
    }

    // Получить размер шрифта в зависимости от ширины экрана
    @Composable
    fun getResponsiveTextSize(): Dp {
        return if (isTablet()) 18.dp else 14.dp
    }

    // Получить размер кнопки в зависимости от ширины экрана
    @Composable
    fun getResponsiveButtonHeight(): Dp {
        return if (isTablet()) 56.dp else 48.dp
    }

    // Получить размер иконки
    @Composable
    fun getResponsiveIconSize(): Dp {
        return if (isTablet()) 32.dp else 24.dp
    }

    // Получить высоту предпросмотра камеры
    @Composable
    fun getCameraPreviewHeight(): Dp {
        val screenHeight = getScreenHeight()
        return if (isTablet()) screenHeight * 0.7f else screenHeight * 0.6f
    }
}
