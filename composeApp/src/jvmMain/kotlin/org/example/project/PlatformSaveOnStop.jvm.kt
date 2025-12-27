package org.example.project

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSaveOnStop(enabled: Boolean, onStop: suspend () -> Unit) {
    // Desktop: no-op
}
