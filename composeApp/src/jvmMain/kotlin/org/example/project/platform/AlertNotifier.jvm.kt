package org.example.project.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit

@Composable
actual fun rememberAlertNotifier(): AlertNotifier {
    return remember {
        object : AlertNotifier {
            override fun notifyPriceAlert(title: String, message: String) {
                // Mínimo exigible: sonido.
                // Si quieres “popup” real, lo hacemos luego con TrayIcon/SystemTray.
                beep()
            }

            override fun beep() {
                runCatching { Toolkit.getDefaultToolkit().beep() }
            }
        }
    }
}
