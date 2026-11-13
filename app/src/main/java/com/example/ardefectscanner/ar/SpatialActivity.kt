package com.example.ardefectscanner.ar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ardefectscanner.camera.CameraAccess
import com.example.ardefectscanner.data.EquipmentCatalog
import com.example.ardefectscanner.ui.theme.ARDefectScannerTheme

class SpatialActivity : ComponentActivity() {
    companion object { const val EQUIPMENT_ID = "equipment_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val equipment = EquipmentCatalog.find(intent.getStringExtra(EQUIPMENT_ID))
        if (equipment == null) { finish(); return }
        setContent {
            ARDefectScannerTheme {
                var result by remember { mutableStateOf<DetectionFrame?>(null) }
                var failure by remember { mutableStateOf<String?>(null) }
                Box(Modifier.fillMaxSize().background(Color(0xFF10251D))) {
                    CameraAccess {
                        val detector = remember { UnavailableDetector() }
                        InspectionCamera(Modifier.fillMaxSize(), detector,
                            onFrame = { result = it; if (it != null) failure = null },
                            onError = { failure = it })
                    }
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { finish() }) { Text("← Карточка") }
                            Surface(color = Color(0xDD183F32), contentColor = Color.White,
                                shape = RoundedCornerShape(50)) {
                                Text("Осмотр", Modifier.padding(14.dp))
                            }
                        }
                        Surface(color = Color(0xEE183F32), contentColor = Color.White,
                            shape = RoundedCornerShape(28.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(equipment.name, style = MaterialTheme.typography.titleLarge)
                                Text(failure ?: when {
                                    result == null -> "Ожидание кадров камеры"
                                    result?.modelAvailable != true -> "Модель распознавания не подключена"
                                    result!!.detections.isEmpty() -> "В этом кадре объекты и признаки дефектов не обнаружены"
                                    else -> "Объектов: ${result!!.detections.count { it.kind == DetectionKind.OBJECT }} · Дефектов: ${result!!.detections.count { it.kind == DetectionKind.DEFECT }}"
                                })
                                Text("Режим анализа: объект и дефект. Найденные области будут выделены рамками с названиями.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4E4D7))
                                Text("Отсутствие обнаруженных признаков не подтверждает исправность оборудования.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4E4D7))
                            }
                        }
                    }
                }
            }
        }
    }
}
