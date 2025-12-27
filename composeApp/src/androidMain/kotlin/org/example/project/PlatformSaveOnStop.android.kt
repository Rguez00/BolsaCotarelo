package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

@Composable
actual fun PlatformSaveOnStop(
    enabled: Boolean,
    onStop: suspend () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val latestEnabled = rememberUpdatedState(enabled)
    val latestOnStop = rememberUpdatedState(onStop)

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (!latestEnabled.value) return@LifecycleEventObserver

            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    runCatching { latestOnStop.value() }
                        .onFailure { it.printStackTrace() }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
