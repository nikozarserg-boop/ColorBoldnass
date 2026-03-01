package com.example.colorboldnass.data.model

// Режимы цветовой слепоты
enum class ColorBlindnessMode(val displayName: String) {
    NORMAL("Нормальное зрение"),
    DEUTERANOPIA("Дейтеранопия"),
    PROTANOPIA("Протанопия"),
    TRITANOPIA("Тританопия"),
    ACHROMATOPSIA("Монохроматизм")
}

// Режимы съёмки
enum class CaptureMode {
    PHOTO,
    VIDEO
}
