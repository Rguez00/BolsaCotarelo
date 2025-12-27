package org.example.project.platform

import androidx.compose.runtime.Composable

interface AlertNotifier {
    fun notifyPriceAlert(title: String, message: String)
    fun beep()
}

@Composable
expect fun rememberAlertNotifier(): AlertNotifier
