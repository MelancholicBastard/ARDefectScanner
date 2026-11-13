package com.example.ardefectscanner.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private tailrec fun Context.activity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> error("Activity context required")
}

@Composable
fun CameraAccess(content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var rationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        asked = true
        revision++
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val granted = remember(revision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (granted) content() else {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Нужен доступ к камере", color = Color.White,
                    style = MaterialTheme.typography.titleLarge)
                Text("Камера используется для QR-кодов и осмотра объектов и дефектов.",
                    color = Color(0xFFD4E4D7))
                Button(onClick = {
                    when {
                        ActivityCompat.shouldShowRequestPermissionRationale(context.activity(), Manifest.permission.CAMERA) ->
                            rationale = true
                        asked -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                        else -> launcher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text(if (asked) "Настроить доступ" else "Разрешить камеру") }
            }
        }
    }
    if (rationale) AlertDialog(
        onDismissRequest = { rationale = false },
        title = { Text("Доступ к камере") },
        text = { Text("Разрешите камеру для чтения QR и осмотра оборудования. Каталог доступен и без разрешения.") },
        confirmButton = {
            TextButton(onClick = { rationale = false; launcher.launch(Manifest.permission.CAMERA) }) { Text("Продолжить") }
        },
        dismissButton = { TextButton(onClick = { rationale = false }) { Text("Позже") } }
    )
}
