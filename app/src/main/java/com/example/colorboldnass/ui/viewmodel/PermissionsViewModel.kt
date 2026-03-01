package com.example.colorboldnass.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel для управления логикой запроса разрешений
class PermissionsViewModel : ViewModel() {

    // Состояние, указывающее, предоставлены ли все необходимые разрешения
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted = _permissionsGranted.asStateFlow()

    // Функция для обновления состояния разрешений
    fun onPermissionsResult(granted: Boolean) {
        _permissionsGranted.value = granted
    }
}
