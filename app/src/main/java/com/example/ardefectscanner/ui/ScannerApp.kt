package com.example.ardefectscanner.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ardefectscanner.camera.CameraAccess
import com.example.ardefectscanner.camera.QrCamera
import com.example.ardefectscanner.data.Equipment
import com.example.ardefectscanner.ui.theme.Cookie
import com.example.ardefectscanner.ui.theme.Forest
import com.example.ardefectscanner.ui.theme.Lime
import com.example.ardefectscanner.data.EquipmentCatalog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun ScannerApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCode by rememberSaveable { mutableStateOf(false) }
    var showQrScanner by rememberSaveable { mutableStateOf(false) }
    val inspection = rememberSaveable(saver = InspectionSession.Saver) { InspectionSession() }
    val selected = EquipmentCatalog.find(selectedId)
    BackHandler(enabled = selected != null || showQrScanner || tab != 0) {
        when {
            showCode -> showCode = false
            selected != null -> selectedId = null
            showQrScanner -> showQrScanner = false
            else -> tab = 0
        }
    }
    Scaffold(
        bottomBar = {
            if (selected == null) NavigationBar {
                listOf("Сканер", "Результаты", "Каталог").forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index; showQrScanner = false },
                        icon = { Glyph(listOf("qr", "results", "equipment")[index]) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when {
                selected != null -> EquipmentDetails(
                    selected,
                    onBack = { selectedId = null },
                    onQr = { showCode = true },
                    onInspect = { selectedId = null; showQrScanner = false; tab = 0 }
                )
                tab == 0 && showQrScanner -> ScannerScreen { equipment ->
                    selectedId = equipment.id
                    showQrScanner = false
                }
                tab == 0 -> InspectionScreen(inspection,
                    onResults = { tab = 1 }, onQr = { showQrScanner = true })
                tab == 1 -> InspectionResults(inspection, onScan = { tab = 0 })
                else -> CatalogScreen(onEquipment = { selectedId = it.id })
            }
        }
    }
    if (showCode && selected != null) EquipmentQrDialog(selected) { showCode = false }
}

@Composable
private fun CatalogScreen(onEquipment: (Equipment) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(Cookie).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) { Glyph("equipment") }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("ARDScan", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Text("ИНЖЕНЕРНЫЙ АССИСТЕНТ", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                Text("● Локально", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("Каталог оборудования", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Демонстрационный каталог", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("02", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        EquipmentCatalog.equipment.forEach { EquipmentRow(it) { onEquipment(it) } }
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(22.dp)) {
            Text("Данные примеров хранятся на устройстве. Подключение к серверу предприятия будет добавлено отдельно.",
                Modifier.padding(18.dp), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EquipmentRow(equipment: Equipment, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Glyph("equipment")
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(equipment.name, fontWeight = FontWeight.SemiBold)
                Text(equipment.model, style = MaterialTheme.typography.bodyMedium)
                Text(equipment.id, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("↗", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun ScannerScreen(onFound: (Equipment) -> Unit) {
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var manual by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Найти оборудование", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Поместите QR-код в поле камеры",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Forest), contentAlignment = Alignment.Center) {
            CameraAccess {
                QrCamera(Modifier.fillMaxSize()) { raw ->
                    val equipment = EquipmentCatalog.resolveQr(raw)
                    if (equipment == null) error = "Код не найден в локальном каталоге. Выберите оборудование вручную."
                    else onFound(equipment)
                }
                Box(Modifier.size(210.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val length = 38.dp.toPx()
                        val stroke = 4.dp.toPx()
                        val color = Lime
                        listOf(
                            Triple(0f, 0f, 1f to 1f),
                            Triple(size.width, 0f, -1f to 1f),
                            Triple(0f, size.height, 1f to -1f),
                            Triple(size.width, size.height, -1f to -1f)
                        ).forEach { (x, y, direction) ->
                            drawLine(color, androidx.compose.ui.geometry.Offset(x, y),
                                androidx.compose.ui.geometry.Offset(x + length * direction.first, y), stroke)
                            drawLine(color, androidx.compose.ui.geometry.Offset(x, y),
                                androidx.compose.ui.geometry.Offset(x, y + length * direction.second), stroke)
                        }
                    }
                }
            }
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        Text("QR содержит только идентификатор. Информация открывается с устройства.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Выбрать из каталога")
        }
        Spacer(Modifier.height(4.dp))
    }
    if (manual) AlertDialog(
        onDismissRequest = { manual = false },
        title = { Text("Выберите оборудование") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EquipmentCatalog.equipment.forEach { equipment ->
                    EquipmentRow(equipment) { manual = false; onFound(equipment) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { manual = false }) { Text("Закрыть") } }
    )
}

@Composable
private fun EquipmentDetails(equipment: Equipment, onBack: () -> Unit, onQr: () -> Unit, onInspect: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            TextButton(onClick = onQr) { Glyph("qr"); Spacer(Modifier.width(8.dp)); Text("QR-код") }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
            Text("ЦИФРОВАЯ КАРТОЧКА · ${equipment.id}",
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.labelSmall)
        }
        Text(equipment.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(equipment.model, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(equipment.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Демонстрационные данные", fontWeight = FontWeight.SemiBold)
                Text("Текущие измерения и результаты осмотра отсутствуют. Техническое состояние не определено.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            onClick = onInspect,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
        ) { Text("Осмотр: объект и дефект", fontWeight = FontWeight.Bold) }
        Text("Паспорт оборудования", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        InfoRow("Серийный номер", equipment.serial)
        InfoRow("Дата установки", equipment.installed)
        equipment.parameters.forEach { parameter ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(parameter.name, parameter.value)
                Text(parameter.reference, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Справочный состав оборудования", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        equipment.components.forEachIndexed { index, component ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("0${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(component.name, fontWeight = FontWeight.SemiBold)
                        Text(component.description, style = MaterialTheme.typography.bodyMedium)
                        Text("Ресурс: нет исходных данных", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("История осмотров", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Осмотры ещё не зарегистрированы.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EquipmentQrDialog(equipment: Equipment, onDismiss: () -> Unit) {
    val payload = EquipmentCatalog.qrPayload(equipment)
    val bitmap = remember(payload) {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512)
        Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(512 * 512) { index ->
                if (matrix[index % 512, index / 512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
            setPixels(pixels, 0, 512, 0, 0, 512, 512)
        }.asImageBitmap()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR оборудования") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(bitmap, contentDescription = "QR-код $payload",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White))
                Text(payload, fontWeight = FontWeight.Bold)
                Text("Откройте этот код на другом устройстве для проверки сканера.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun Glyph(kind: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.width * 0.075f)
        if (kind == "qr") {
            listOf(0.08f to 0.08f, 0.58f to 0.08f, 0.08f to 0.58f).forEach { (x, y) ->
                drawRect(color, androidx.compose.ui.geometry.Offset(size.width * x, size.height * y),
                    androidx.compose.ui.geometry.Size(size.width * .30f, size.height * .30f), style = stroke)
            }
            drawRect(color, androidx.compose.ui.geometry.Offset(size.width * .59f, size.height * .59f),
                androidx.compose.ui.geometry.Size(size.width * .13f, size.height * .13f))
            drawRect(color, androidx.compose.ui.geometry.Offset(size.width * .77f, size.height * .77f),
                androidx.compose.ui.geometry.Size(size.width * .13f, size.height * .13f))
        } else if (kind == "results") {
            for (x in 0..1) for (y in 0..1) {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(size.width * (.1f + x * .45f), size.height * (.1f + y * .45f)),
                    androidx.compose.ui.geometry.Size(size.width * .32f, size.height * .32f),
                    androidx.compose.ui.geometry.CornerRadius(size.width * .08f), style = stroke)
            }
        } else {
            drawRoundRect(color, androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .25f),
                androidx.compose.ui.geometry.Size(size.width * .65f, size.height * .5f),
                androidx.compose.ui.geometry.CornerRadius(size.width * .12f), style = stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .77f, size.height * .5f),
                androidx.compose.ui.geometry.Offset(size.width * .97f, size.height * .5f), stroke.width)
            drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .87f),
                androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .87f), stroke.width)
        }
    }
}
