package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberCsvFileSaver(): CsvFileSaver {
    // Frame “invisible” para poder abrir FileDialog en Desktop
    val frame = remember { Frame() }
    DisposableEffect(Unit) { onDispose { frame.dispose() } }

    return remember {
        object : CsvFileSaver {
            override fun saveCsv(
                suggestedFileName: String,
                csvText: String,
                onResult: (Boolean, String?) -> Unit
            ) {
                try {
                    // ✅ Asegura extensión .csv
                    val name = if (suggestedFileName.endsWith(".csv", ignoreCase = true)) {
                        suggestedFileName
                    } else {
                        "$suggestedFileName.csv"
                    }

                    val dialog = FileDialog(frame, "Guardar CSV", FileDialog.SAVE)
                    dialog.file = name

                    // ✅ En AWT es más estable usar setVisible(true)
                    dialog.isVisible = true
                    // dialog.setVisible(true) // alternativa equivalente

                    val dir = dialog.directory
                    val fileName = dialog.file
                    if (dir.isNullOrBlank() || fileName.isNullOrBlank()) {
                        onResult(false, "Export cancelado")
                        return
                    }

                    File(dir, fileName).writeText(csvText, Charsets.UTF_8)
                    onResult(true, null)
                } catch (e: Exception) {
                    onResult(false, e.message ?: "Error al guardar")
                }
            }
        }
    }
}
