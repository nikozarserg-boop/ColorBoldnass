package com.example.colorboldnass.domain.camera

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.colorboldnass.data.model.ColorBlindnessMode
import com.example.colorboldnass.domain.filter.ColorBlindnessSurfaceProcessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.core.util.Consumer

private const val TAG = "CameraManager"

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recorder: Recorder? = null
    private var camera: Camera? = null
    
    private val glExecutor = Executors.newSingleThreadExecutor()
    private var surfaceProcessor: ColorBlindnessSurfaceProcessor? = null
    
    var currentColorBlindnessMode: ColorBlindnessMode = ColorBlindnessMode.NORMAL
        set(value) {
            field = value
            surfaceProcessor?.setColorMode(value)
        }

    private class ColorBlindnessEffect(
        targets: Int,
        executor: Executor,
        surfaceProcessor: ColorBlindnessSurfaceProcessor,
        errorListener: Consumer<Throwable>
    ) : CameraEffect(targets, executor, surfaceProcessor, errorListener)

    @RequiresApi(Build.VERSION_CODES.O)
    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewSurfaceProvider: Preview.SurfaceProvider,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Рассчитываем соотношение сторон экрана
                val aspectRatio = screenAspectRatio()
                
                // Настраиваем ResolutionSelector для высокого разрешения
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            aspectRatio,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .apply {
                        setSurfaceProvider(previewSurfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.fromOrderedList(
                            listOf(Quality.UHD, Quality.FHD, Quality.HD)
                        )
                    )
                    .build()
                videoCapture = VideoCapture.withOutput(recorder!!)

                // Инициализируем GPU процессор
                surfaceProcessor = ColorBlindnessSurfaceProcessor(glExecutor)
                surfaceProcessor?.setColorMode(currentColorBlindnessMode)

                val effect = ColorBlindnessEffect(
                    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
                    glExecutor,
                    surfaceProcessor!!
                ) { Log.e(TAG, "Эффект камеры ошибка", it) }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                val useCaseGroup = androidx.camera.core.UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture!!)
                    .addUseCase(videoCapture!!)
                    .addEffect(effect)
                    .build()

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    useCaseGroup
                )

                onSuccess()
            } catch (exc: Exception) {
                onError(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Метод для управления зумом
    fun setZoom(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    // Метод для получения текущего состояния зума
    fun getZoomState() = camera?.cameraInfo?.zoomState

    private fun screenAspectRatio(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            width = metrics.bounds.width()
            height = metrics.bounds.height()
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
        }
        val ratio = maxOf(width, height).toDouble() / minOf(width, height).toDouble()
        return if (Math.abs(ratio - 4.0 / 3.0) <= Math.abs(ratio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    fun takePhoto(outputFile: File, executor: Executor, onSuccess: (File) -> Unit, onError: (ImageCaptureException) -> Unit) {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(outputFile).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) { onSuccess(outputFile) }
                override fun onError(exception: ImageCaptureException) { onError(exception) }
            }
        )
    }

    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture
    fun getOutputFileName() = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    fun getVideoFileName() = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"

    fun getOutputDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ColorBoldnass")
        dir.mkdirs()
        return dir
    }
    
    fun getVideoOutputDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ColorBoldnass")
        dir.mkdirs()
        return dir
    }

    fun releaseCamera() {
        cameraProvider?.unbindAll()
        surfaceProcessor?.release()
        glExecutor.shutdown()
        camera = null
    }
}
