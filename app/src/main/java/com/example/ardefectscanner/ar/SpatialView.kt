package com.example.ardefectscanner.ar

import android.graphics.RectF
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.view.TransformExperimental
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ardefectscanner.camera.CameraFeed
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class ScreenDetection(val detection: Detection, val bounds: RectF)

@OptIn(TransformExperimental::class)
@Composable
fun InspectionCamera(
    modifier: Modifier = Modifier,
    detector: FrameDetector,
    onFrame: (DetectionFrame?) -> Unit,
    onError: (String) -> Unit
) {
    var visible by remember { mutableStateOf<List<ScreenDetection>>(emptyList()) }
    var lastFrame by remember { mutableLongStateOf(0L) }
    val currentFrame by rememberUpdatedState(onFrame)
    CameraFeed(
        modifier = modifier,
        process = { image -> detector.detect(image) },
        onFrame = { frame, transform ->
            lastFrame = SystemClock.elapsedRealtime()
            visible = if (transform == null) emptyList() else frame.detections.map { detection ->
                val box = detection.box
                val bounds = RectF(box.left * frame.imageWidth, box.top * frame.imageHeight,
                    box.right * frame.imageWidth, box.bottom * frame.imageHeight)
                transform.mapRect(bounds)
                ScreenDetection(detection, bounds)
            }
            currentFrame(frame)
        },
        onInactive = { visible = emptyList(); currentFrame(null) },
        onError = { visible = emptyList(); currentFrame(null); onError(it) },
        closeProcessor = { detector.close() }
    )
    LaunchedEffect(lastFrame) {
        delay(500)
        visible = emptyList()
        currentFrame(null)
    }
    DetectionOverlay(visible, modifier)
}

@Composable
fun DetectionOverlay(detections: List<ScreenDetection>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            detections.forEach { item ->
                val rect = item.bounds
                if (rect.width() > 0 && rect.height() > 0) {
                    drawRect(detectionColor(item.detection.kind),
                        topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                        size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                        style = Stroke(3.dp.toPx()))
                }
            }
        }
        val density = LocalDensity.current
        val labelHeight = with(density) { 32.dp.roundToPx() }
        val minimumLabelWidth = with(density) { 160.dp.toPx() }
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()
        detections.forEach { item ->
            val rect = item.bounds
            if (rect.right > 0 && rect.bottom > 0 && rect.left < viewportWidth && rect.top < viewportHeight) {
                key(item.detection.id) {
                    val detection = item.detection
                    val kind = if (detection.kind == DetectionKind.OBJECT) "Объект" else "Дефект"
                    val score = detection.confidence?.let { " · ${(it * 100).roundToInt()}%" }.orEmpty()
                    val labelX = rect.left.coerceIn(0f, (viewportWidth - minimumLabelWidth).coerceAtLeast(0f))
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    labelX.roundToInt(),
                                    (rect.top.roundToInt() - labelHeight)
                                        .coerceIn(0, (viewportHeight.toInt() - labelHeight).coerceAtLeast(0))
                                )
                            }
                            .widthIn(max = with(density) { (viewportWidth - labelX).toDp() }),
                        color = detectionColor(detection.kind), contentColor = Color(0xFF122016)
                    ) {
                        Text("$kind: ${detection.label}$score", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun detectionColor(kind: DetectionKind) =
    if (kind == DetectionKind.OBJECT) Color(0xFFD9F4A7) else Color(0xFFFFB4A5)
