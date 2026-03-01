package com.example.colorboldnass.domain.filter

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.example.colorboldnass.data.model.ColorBlindnessMode
import java.util.concurrent.Executor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.RejectedExecutionException

private const val TAG = "ColorBlindnessProcessor"

class ColorBlindnessSurfaceProcessor(private val glExecutor: Executor) : SurfaceProcessor, SurfaceTexture.OnFrameAvailableListener {

    private val handlerThread = HandlerThread("GLThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    
    private var inputSurfaceTexture: SurfaceTexture? = null
    private var inputTextureId = -1
    private var program = -1
    
    @Volatile
    private var isReleased = false
    
    // Безопасная обертка для Executor, которая не кидает RejectedExecutionException
    private val safeGlExecutor = Executor { command ->
        if (!isReleased) {
            try {
                glExecutor.execute(command)
            } catch (e: RejectedExecutionException) {
                Log.w(TAG, "Задача отклонена: исполнитель завершает работу")
            }
        }
    }

    private var currentColorMode = ColorBlindnessMode.NORMAL
    private val outputSurfaces = mutableMapOf<SurfaceOutput, EGLSurface>()

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
        .put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).position(0) as FloatBuffer
    
    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
        .put(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)).position(0) as FloatBuffer

    init {
        safeExecute {
            initEGL()
            initGL()
        }
    }

    private fun safeExecute(block: () -> Unit) {
        safeGlExecutor.execute(block)
    }

    fun setColorMode(mode: ColorBlindnessMode) {
        currentColorMode = mode
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        safeExecute {
            if (isReleased) return@safeExecute
            inputTextureId = createExternalTexture()
            inputSurfaceTexture = SurfaceTexture(inputTextureId)
            inputSurfaceTexture?.setOnFrameAvailableListener(this, handler)
            
            val surface = Surface(inputSurfaceTexture)
            inputSurfaceTexture?.setDefaultBufferSize(surfaceRequest.resolution.width, surfaceRequest.resolution.height)
            
            // Передаем safeGlExecutor для слушателя очистки
            surfaceRequest.provideSurface(surface, safeGlExecutor) {
                safeExecute {
                    surface.release()
                    inputSurfaceTexture?.release()
                    inputSurfaceTexture = null
                    if (inputTextureId != -1) {
                        GLES20.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
                        inputTextureId = -1
                    }
                }
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        safeExecute {
            if (isReleased) return@safeExecute
            // Используем safeGlExecutor для получения поверхности и слушателя очистки
            val surface = surfaceOutput.getSurface(safeGlExecutor) {
                safeExecute {
                    val eglSurface = outputSurfaces.remove(surfaceOutput)
                    if (eglSurface != null && eglDisplay != EGL14.EGL_NO_DISPLAY) {
                        EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    }
                }
            }
            val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0)
            outputSurfaces[surfaceOutput] = eglSurface
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        if (isReleased) return
        safeExecute {
            if (!isReleased) render()
        }
    }

    private fun initEGL() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        eglConfig = configs[0]

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        
        val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val dummySurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, dummySurface, dummySurface, eglContext)
    }

    private fun initGL() {
        program = createProgram()
    }

    private fun render() {
        val st = inputSurfaceTexture ?: return
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglContext == EGL14.EGL_NO_CONTEXT) return

        try {
            st.updateTexImage()
        } catch (e: Exception) {
            return
        }

        val stMatrix = FloatArray(16)
        st.getTransformMatrix(stMatrix)

        for ((output, eglSurface) in outputSurfaces) {
            if (eglSurface == EGL14.EGL_NO_SURFACE) continue
            
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            
            val finalMatrix = FloatArray(16)
            output.updateTransformMatrix(finalMatrix, stMatrix)
            
            GLES20.glViewport(0, 0, output.size.width, output.size.height)
            GLES20.glUseProgram(program)
            
            val uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
            GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, finalMatrix, 0)
            
            val uColorMode = GLES20.glGetUniformLocation(program, "uColorMode")
            GLES20.glUniform1i(uColorMode, currentColorMode.ordinal)

            val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
            val texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
            
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            
            GLES20.glEnableVertexAttribArray(texLoc)
            GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
            
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTextureId)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        return textures[0]
    }

    private fun createProgram(): Int {
        val vertexSource = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            uniform mat4 uTexMatrix;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
            }
        """.trimIndent()

        val fragmentSource = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            uniform int uColorMode;
            vec3 applyFilter(vec3 rgb, int mode) {
                if (mode == 1) return mat3(0.56667, 0.55833, 0.0, 0.43333, 0.44167, 0.24167, 0.0, 0.0, 0.75833) * rgb;
                if (mode == 2) return mat3(0.625, 0.70, 0.0, 0.375, 0.30, 0.30, 0.0, 0.0, 0.70) * rgb;
                if (mode == 3) return mat3(0.95, 0.0, 0.0, 0.05, 0.43333, 0.475, 0.0, 0.56667, 0.525) * rgb;
                if (mode == 4) return vec3(dot(rgb, vec3(0.299, 0.587, 0.114)));
                return rgb;
            }
            void main() {
                vec4 color = texture2D(sTexture, vTexCoord);
                gl_FragColor = vec4(applyFilter(color.rgb, uColorMode), color.a);
            }
        """.trimIndent()

        val vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).apply {
            GLES20.glShaderSource(this, vertexSource)
            GLES20.glCompileShader(this)
        }
        val fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).apply {
            GLES20.glShaderSource(this, fragmentSource)
            GLES20.glCompileShader(this)
        }
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vs)
            GLES20.glAttachShader(this, fs)
            GLES20.glLinkProgram(this)
        }
    }
    
    fun release() {
        isReleased = true
        handlerThread.quitSafely()
        // Используем безопасный исполнитель для финальной очистки
        safeExecute {
            outputSurfaces.values.forEach { EGL14.eglDestroySurface(eglDisplay, it) }
            outputSurfaces.clear()
            if (program != -1) {
                GLES20.glDeleteProgram(program)
                program = -1
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }
    }
}
