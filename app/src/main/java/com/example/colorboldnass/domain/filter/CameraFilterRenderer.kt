package com.example.colorboldnass.domain.filter

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.colorboldnass.data.model.ColorBlindnessMode
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "CameraFilterRenderer"

/**
 * Правильный способ применения фильтров к видеопотоку камеры.
 * 
 * Фильтры к ImageProxy нельзя применять напрямую т.к. это read-only буферы.
 * Вместо этого используем OpenGL для GPU-ускоренной обработки.
 */
class CameraFilterRenderer : GLSurfaceView.Renderer {
    
    private var currentColorMode: ColorBlindnessMode = ColorBlindnessMode.NORMAL
    private var shaderProgram: Int = 0
    
    fun setColorBlindnessMode(mode: ColorBlindnessMode) {
        currentColorMode = mode
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "Surface создана")
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        
        // Компилируем шейдеры при первой инициализации
        try {
            shaderProgram = createShaderProgram()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании шейдер-программы: ${e.message}", e)
        }
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "Surface изменена: ${width}x${height}")
        GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        if (currentColorMode != ColorBlindnessMode.NORMAL) {
            GLES20.glUseProgram(shaderProgram)
            // Устанавливаем uniform для текущего режима фильтра
            val modeUniform = GLES20.glGetUniformLocation(shaderProgram, "colorMode")
            GLES20.glUniform1i(modeUniform, currentColorMode.ordinal)
        }
    }
    
    private fun createShaderProgram(): Int {
        val vertexShader = compileShader(
            GLES20.GL_VERTEX_SHADER,
            """
                attribute vec4 position;
                attribute vec2 texCoord;
                varying vec2 vTexCoord;
                
                void main() {
                    gl_Position = position;
                    vTexCoord = texCoord;
                }
            """.trimIndent()
        )
        
        val fragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            """
                precision mediump float;
                uniform sampler2D textureSampler;
                uniform int colorMode;
                varying vec2 vTexCoord;
                
                vec3 filterProtanopia(vec3 rgb) {
                    // Матрица для протанопии
                    mat3 transform = mat3(
                        0.170556992, 0.170556991, -0.004517144,
                        0.829443014, 0.829443008, 0.004517144,
                        0.0, 0.0, 1.0
                    );
                    return transform * rgb;
                }
                
                vec3 filterDeuteranopia(vec3 rgb) {
                    // Матрица для дейтеранопии
                    mat3 transform = mat3(
                        0.33066007, 0.33066007, -0.02785538,
                        0.66933993, 0.66933993, 0.02785538,
                        0.0, 0.0, 1.0
                    );
                    return transform * rgb;
                }
                
                vec3 filterTritanopia(vec3 rgb) {
                    // Матрица для тританопии (Viénot et al.)
                    return vec3(
                        rgb.r + 0.1273989 * rgb.g - 0.1273989 * rgb.b,
                        rgb.g * 0.8739093 + rgb.b * 0.1260907,
                        rgb.g * 0.8739093 + rgb.b * 0.1260907
                    );
                }
                
                vec3 filterAchromatopsia(vec3 rgb) {
                    // Черно-белый фильтр
                    float gray = 0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b;
                    return vec3(gray, gray, gray);
                }
                
                void main() {
                    vec4 color = texture2D(textureSampler, vTexCoord);
                    vec3 rgb = color.rgb;
                    
                    if (colorMode == 1) {
                        rgb = filterProtanopia(rgb);
                    } else if (colorMode == 2) {
                        rgb = filterDeuteranopia(rgb);
                    } else if (colorMode == 3) {
                        rgb = filterTritanopia(rgb);
                    } else if (colorMode == 4) {
                        rgb = filterAchromatopsia(rgb);
                    }
                    
                    gl_FragColor = vec4(rgb, color.a);
                }
            """.trimIndent()
        )
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        
        if (linkStatus[0] == GLES20.GL_FALSE) {
            val error = GLES20.glGetProgramInfoLog(program)
            throw RuntimeException("Ошибка линкования шейдер-программы: $error")
        }
        
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        
        return program
    }
    
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        
        if (compiled[0] == GLES20.GL_FALSE) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Ошибка компиляции шейдера: $error")
        }
        
        return shader
    }
}
