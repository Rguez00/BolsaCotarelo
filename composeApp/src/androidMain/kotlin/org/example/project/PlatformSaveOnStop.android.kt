package org.example.project

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
actual fun PlatformSaveOnStop(
    enabled: Boolean,
    onSave: suspend () -> Unit
) {
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val latestOnSave by rememberUpdatedState(onSave)

    DisposableEffect(owner, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch { latestOnSave() }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}
