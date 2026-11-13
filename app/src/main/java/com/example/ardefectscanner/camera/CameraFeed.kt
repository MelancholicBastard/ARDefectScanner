package com.example.ardefectscanner.camera

import android.os.SystemClock
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Both consumers own only analysis; this layer owns binding, transforms and frame release. */
@androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)
@Composable
fun <T> CameraFeed(
    modifier: Modifier,
    process: (ImageProxy) -> T,
    onFrame: (T, CoordinateTransform?) -> Unit,
    onError: (String) -> Unit,
    onInactive: () -> Unit = {},
    closeProcessor: () -> Unit = {}
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentProcess by rememberUpdatedState(process)
    val currentFrame by rememberUpdatedState(onFrame)
    val currentError by rememberUpdatedState(onError)
    val currentInactive by rememberUpdatedState(onInactive)
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(owner, previewView) {
        val active = AtomicBoolean(true)
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val transformFactory = ImageProxyTransformFactory().apply {
            isUsingCropRect = true
            isUsingRotationDegrees = true
        }
        var lastTime = 0L
        analysis.setAnalyzer(executor) { image ->
            try {
                val now = SystemClock.elapsedRealtime()
                if (active.get() && now - lastTime >= 100) {
                    lastTime = now
                    val result = currentProcess(image)
                    val source = transformFactory.getOutputTransform(image)
                    main.execute {
                        if (active.get() && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (SystemClock.elapsedRealtime() - now > 400) {
                                currentInactive()
                            } else {
                                val target = previewView.outputTransform
                                val transform = target?.let { CoordinateTransform(source, it) }
                                currentFrame(result, transform)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                main.execute {
                    if (active.get()) currentError("Не удалось обработать кадр. Повторите открытие камеры.")
                }
            } catch (_: LinkageError) {
                main.execute {
                    if (active.get()) currentError("Не удалось загрузить модуль распознавания для этого устройства.")
                }
            } finally {
                image.close()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) currentInactive()
        }
        owner.lifecycle.addObserver(observer)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        previewView.doOnLayout {
            future.addListener({
                if (active.get()) {
                    try {
                        provider = future.get()
                        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                        preview.targetRotation = rotation
                        analysis.targetRotation = rotation
                        val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis)
                        previewView.viewPort?.let { group.setViewPort(it) }
                        // One viewport makes detector coordinates and preview crop agree.
                        provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, group.build())
                    } catch (_: Exception) {
                        currentError("Камера недоступна. Проверьте разрешение и закройте другие приложения с камерой.")
                    }
                }
            }, main)
        }
        onDispose {
            active.set(false)
            owner.lifecycle.removeObserver(observer)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            executor.execute(closeProcessor)
            executor.shutdown()
        }
    }
}
