package org.example.project.platform

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

@Composable
fun RequestNotificationsPermissionOnce() {
    // Solo existe en Android 13+ (API 33)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no hace falta manejar el resultado aquí */ }

    LaunchedEffect(Unit) {
        if (!asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
