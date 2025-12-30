package org.example.project.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeController(
    initial: ThemeMode = ThemeMode.SYSTEM
) {
    private val _mode = MutableStateFlow(initial)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }

    fun toggleLightDark() {
        _mode.value = when (_mode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK // decisión: SYSTEM -> DARK al tocar toggle
        }
    }
}
