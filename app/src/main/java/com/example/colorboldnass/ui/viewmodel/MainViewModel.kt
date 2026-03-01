package com.example.colorboldnass.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.colorboldnass.data.model.ColorBlindnessMode
import com.example.colorboldnass.data.model.CaptureMode
import kotlinx.coroutines.flow.asStateFlow

// ViewModel для управления основным состоянием приложения
class MainViewModel : ViewModel() {

    // Текущий режим цветовой слепоты
    private val _colorBlindnessMode = MutableStateFlow(ColorBlindnessMode.NORMAL)
    val colorBlindnessMode: StateFlow<ColorBlindnessMode> = _colorBlindnessMode.asStateFlow()

    // Текущий режим съёмки
    private val _captureMode = MutableStateFlow(CaptureMode.PHOTO)
    val captureMode: StateFlow<CaptureMode> = _captureMode.asStateFlow()

    // Флаг для отслеживания текущего экрана
    private val _isOnCameraScreen = MutableStateFlow(true)
    val isOnCameraScreen: StateFlow<Boolean> = _isOnCameraScreen.asStateFlow()

    // Флаг для отслеживания видеозаписи
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Сообщение об ошибке
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Флаг загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Методы для изменения состояния
    fun setColorBlindnessMode(mode: ColorBlindnessMode) {
        _colorBlindnessMode.value = mode
    }

    fun setCaptureMode(mode: CaptureMode) {
        _captureMode.value = mode
    }

    fun setOnCameraScreen(isOnCamera: Boolean) {
        _isOnCameraScreen.value = isOnCamera
    }

    fun setRecording(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    // Показать ошибку с автоматической очисткой через 5 секунд
    fun showErrorTemporary(message: String) {
        setErrorMessage(message)
        // Ошибка будет очищена в UI через debounce или вручную
    }

    // Получить список всех режимов цветовой слепоты
    fun getAllColorBlindnessModes(): List<ColorBlindnessMode> {
        return ColorBlindnessMode.values().toList()
    }
}
