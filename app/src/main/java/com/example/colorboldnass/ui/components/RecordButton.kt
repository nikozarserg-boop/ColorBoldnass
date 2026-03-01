package com.example.colorboldnass.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Красивая кнопка записи видео с пульсирующим кольцом и анимациями
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scale = remember { Animatable(1f) }
    val ringScale = remember { Animatable(1f) }
    val ringAlpha = remember { Animatable(1f) }
    
    // Пульсирующее внешнее кольцо при записи
    LaunchedEffect(isRecording) {
        if (isRecording) {
            ringScale.animateTo(
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            ringScale.animateTo(1f)
        }
    }
    
    // Мигание кольца при записи
    LaunchedEffect(isRecording) {
        if (isRecording) {
            ringAlpha.animateTo(
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            ringAlpha.animateTo(1f)
        }
    }
    
    // Лёгкое масштабирование кнопки при нажатии
    LaunchedEffect(Unit) {
        // Анимация при нажатии (можно добавить через pointerInput)
    }
    
    val backgroundColor = if (isRecording) {
        Color(0xFFCC0000) // Яркий красный при записи
    } else {
        Color(0xFFFF6B35) // Оранжевый в обычном состоянии (как фото)
    }
    
    Box(
        modifier = modifier
            .size(90.dp)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внешнее пульсирующее кольцо (видно при записи)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(ringScale.value)
                    .clip(CircleShape)
                    .background(
                        backgroundColor.copy(alpha = 0.3f * ringAlpha.value)
                    )
            )
        }
        
        // Основная кнопка с тенью эффектом
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Белая внутренняя точка
            Box(
                modifier = Modifier
                    .size(
                        if (isRecording) 20.dp else 24.dp
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .scale(scale.value)
            )
        }
    }
}

/**
 * Кнопка для фото с красивой анимацией (идентична RecordButton но без пульса)
 */
@Composable
fun PhotoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(90.dp)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        // Основная кнопка
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B35))
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Белая внутренняя точка
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
