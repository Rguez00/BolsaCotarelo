package org.example.project.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val CHANNEL_ID = "price_alerts"
private const val CHANNEL_NAME = "Alertas de precio"

@Composable
actual fun rememberAlertNotifier(): AlertNotifier {
    val context = LocalContext.current

    // ✅ FIX 1: remember debe devolver algo, no Unit
    return remember(context) {
        // Crear canal aquí dentro
        ensureChannel(context)

        // Devolver el objeto AlertNotifier
        object : AlertNotifier {
            override fun notifyPriceAlert(title: String, message: String) {
                // ✅ FIX 2: Verificar permiso explícitamente
                if (!hasPostNotificationsPermission(context)) return

                try {
                    val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_notify_more)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()

                    NotificationManagerCompat.from(context).notify(nextId(), notif)
                } catch (e: SecurityException) {
                    // Manejar el caso donde el permiso fue revocado
                    e.printStackTrace()
                }
            }

            override fun beep() {
                // La notificación ya suena por defecto en Android
            }
        }
    }
}

private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = mgr.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se disparan alertas de precios"
            }
            mgr.createNotificationChannel(channel)
        }
    }
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    } else {
        // En versiones anteriores a Android 13, no se necesita permiso runtime
        true
    }
}

@Volatile private var _id = 1000
private fun nextId(): Int = synchronized(Unit) { _id++ }