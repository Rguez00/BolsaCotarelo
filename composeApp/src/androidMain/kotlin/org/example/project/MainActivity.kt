package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.platform.RequestNotificationsPermissionOnce

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // ✅ pide permiso en Android 13+
            RequestNotificationsPermissionOnce()

            // ✅ arranca la app real con alertas, JSON, etc.
            AppRoot()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // En preview NO pidas permisos (se rompe a veces)
    AppRoot()
}
