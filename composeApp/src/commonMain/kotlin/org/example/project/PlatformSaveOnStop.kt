package org.example.project

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSaveOnStop(
    enabled: Boolean,
    onSave: suspend () -> Unit
)
