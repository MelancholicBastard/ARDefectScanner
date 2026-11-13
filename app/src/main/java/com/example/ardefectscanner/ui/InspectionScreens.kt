package com.example.ardefectscanner.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ardefectscanner.ar.*
import com.example.ardefectscanner.camera.CameraAccess
import com.example.ardefectscanner.ui.theme.Forest
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** Keeps the last processed frame when the camera is closed or the window is recreated. */
class InspectionSession {
    var frame by mutableStateOf<DetectionFrame?>(null)
    var failure by mutableStateOf<String?>(null)
    var capturedAt by mutableLongStateOf(0L)

    fun accept(value: DetectionFrame?) {
        if (value == null) return
        frame = value
        failure = null
        capturedAt = System.currentTimeMillis()
    }

    companion object {
        val Saver = Saver<InspectionSession, Bundle>(
            save = { session ->
                Bundle().apply {
                    putString("failure", session.failure)
                    putLong("time", session.capturedAt)
                    session.frame?.let { frame ->
                        putInt("width", frame.imageWidth)
                        putInt("height", frame.imageHeight)
                        putBoolean("available", frame.modelAvailable)
                        putInt("count", frame.detections.size)
                        frame.detections.forEachIndexed { index, detection ->
                            putBundle("detection$index", Bundle().apply {
                                putString("id", detection.id)
                                putString("kind", detection.kind.name)
                                putString("label", detection.label)
                                putFloatArray("box", floatArrayOf(detection.box.left, detection.box.top,
                                    detection.box.right, detection.box.bottom))
                                detection.confidence?.let { putFloat("confidence", it) }
                            })
                        }
                    }
                }
            },
            restore = { saved ->
                InspectionSession().apply {
                    failure = saved.getString("failure")
                    capturedAt = saved.getLong("time")
                    if (saved.containsKey("width")) {
                        frame = DetectionFrame(saved.getInt("width"), saved.getInt("height"),
                            List(saved.getInt("count")) { index ->
                                val item = requireNotNull(saved.getBundle("detection$index"))
                                val box = requireNotNull(item.getFloatArray("box"))
                                Detection(requireNotNull(item.getString("id")),
                                    DetectionKind.valueOf(requireNotNull(item.getString("kind"))),
                                    requireNotNull(item.getString("label")),
                                    DetectionBox(box[0], box[1], box[2], box[3]),
                                    if (item.containsKey("confidence")) item.getFloat("confidence") else null)
                            }, saved.getBoolean("available"))
                    }
                }
            }
        )
    }
}

@Composable
fun InspectionScreen(session: InspectionSession, onResults: () -> Unit, onQr: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("ARDScan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Наведите камеру на объект",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Forest), contentAlignment = Alignment.Center) {
            CameraAccess {
                val detector = remember { UnavailableDetector() }
                InspectionCamera(
                    modifier = Modifier.fillMaxSize(),
                    detector = detector,
                    onFrame = session::accept,
                    onError = { session.failure = it }
                )
            }
        }
        Text(session.failure ?: when {
            session.frame == null -> "Ожидание кадров камеры"
            session.frame?.modelAvailable != true -> "Модель распознавания не подключена"
            else -> "Результаты анализа доступны на отдельной вкладке"
        }, style = MaterialTheme.typography.bodySmall,
            color = if (session.failure != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onResults, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Информация об объекте")
        }
        TextButton(onClick = onQr, modifier = Modifier.fillMaxWidth()) {
            Text("Найти карточку по QR-коду")
        }
    }
}

@Composable
fun InspectionResults(session: InspectionSession, onScan: () -> Unit) {
    val frame = session.frame
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Результаты", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Информация, найденная моделью",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (session.failure != null) {
            Text(session.failure!!, color = MaterialTheme.colorScheme.error)
        }
        when {
            frame == null -> ResultMessage("Начните сканирование",
                "Откройте сканер и наведите камеру на объект. Здесь появятся результаты анализа.")
            !frame.modelAvailable -> ResultMessage("Модель пока не подключена",
                "Камера работает, но распознавание объектов и дефектов ещё недоступно.")
            else -> {
                Text("Последний обработанный кадр · " +
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                        .format(Date(session.capturedAt)),
                    style = MaterialTheme.typography.labelMedium)
                if (frame.detections.isEmpty()) {
                    ResultMessage("Ничего не обнаружено",
                        "В последнем кадре модель не нашла объектов или признаков дефектов.")
                }
                frame.detections.forEach { detection ->
                    Surface(color = if (detection.kind == DetectionKind.OBJECT)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (detection.kind == DetectionKind.OBJECT) "Объект" else "Признак дефекта",
                                style = MaterialTheme.typography.labelLarge)
                            Text(detection.label, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            detection.confidence?.let {
                                Text("Уверенность модели: ${(it * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Text("Результаты относятся к последнему обработанному кадру. Отсутствие признаков дефектов не подтверждает исправность оборудования.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Открыть сканер")
        }
    }
}

@Composable
private fun ResultMessage(title: String, description: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
