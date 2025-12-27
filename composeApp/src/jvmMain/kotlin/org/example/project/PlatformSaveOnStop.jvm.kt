package org.example.project

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSaveOnStop(
    enabled: Boolean,
    onSave: suspend () -> Unit
) {
    // Desktop: no lifecycle -> no-op
}
