package com.example.ardefectscanner.camera

import android.os.SystemClock
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@androidx.annotation.OptIn(androidx.camera.view.TransformExperimental::class)
@Composable
fun QrCamera(modifier: Modifier = Modifier, onQr: (String) -> Unit) {
    val decoder = remember { QrDecoder() }
    var error by remember { mutableStateOf<String?>(null) }
    var lastCode by remember { mutableStateOf<String?>(null) }
    var lastTime by remember { mutableLongStateOf(0L) }
    CameraFeed(
        modifier = modifier,
        process = { image ->
            val plane = image.planes[0]
            decoder.decode(plane.buffer, image.width, image.height, plane.rowStride, plane.pixelStride)
        },
        onFrame = { raw, _ ->
            val now = SystemClock.elapsedRealtime()
            if (raw != null && (raw != lastCode || now - lastTime > 2500)) {
                lastCode = raw
                lastTime = now
                onQr(raw)
            }
        },
        onError = { error = it }
    )
    error?.let {
        Surface(Modifier.padding(20.dp), color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.large) { Text(it, Modifier.padding(16.dp)) }
    }
}
