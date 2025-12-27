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

    // Crea canal al instanciar
    remember(context) { ensureChannel(context) }

    return remember(context) {
        object : AlertNotifier {
            override fun notifyPriceAlert(title: String, message: String) {
                // Android 13+ requiere permiso runtime; si no está concedido, no rompe: simplemente no notifica
                if (!hasPostNotificationsPermission(context)) return

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_more) // simple, luego lo cambiamos por tu icono
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context).notify(nextId(), notif)
            }

            override fun beep() {
                // En Android, la notificación ya puede sonar por defecto.
                // Si quieres beep extra, lo añadimos con ToneGenerator.
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
    } else true
}

@Volatile private var _id = 1000
private fun nextId(): Int = synchronized(Unit) { _id++ }
