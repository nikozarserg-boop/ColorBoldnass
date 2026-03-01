package com.example.colorboldnass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.colorboldnass.ui.screen.MainScreen
import com.example.colorboldnass.ui.theme.ColorBoldnassTheme
import com.example.colorboldnass.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Включаем отображение "от края до края" (Edge-to-Edge)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setContent {
            ColorBoldnassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        lifecycleOwner = this@MainActivity,
                        // Добавляем отступы для безопасных зон (вырезы, навигация)
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
            }
        }
    }
}
