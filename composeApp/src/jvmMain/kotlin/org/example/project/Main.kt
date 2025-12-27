package org.example.project

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Bolsa Cotarelo",
        state = state,
        resizable = true
    ) {
        // Evita que el layout “desktop” se rompa al hacer la ventana muy pequeña
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(1100, 700)
        }

        App()
    }
}
