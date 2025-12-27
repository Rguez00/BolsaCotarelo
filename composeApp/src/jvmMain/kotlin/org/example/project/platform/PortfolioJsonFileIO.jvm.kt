package org.example.project.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberPortfolioJsonFileIO(): PortfolioJsonFileIO {
    // Frame “invisible” para poder abrir FileDialog
    val frame = remember { Frame().apply { isUndecorated = true; setLocationRelativeTo(null) } }

    return remember {
        object : PortfolioJsonFileIO {

            override fun saveJson(defaultFileName: String, json: String, onResult: (Boolean, String?) -> Unit) {
                try {
                    val dialog = FileDialog(frame, "Guardar portfolio (JSON)", FileDialog.SAVE).apply {
                        file = defaultFileName
                        isVisible = true
                    }

                    val dir = dialog.directory
                    val name = dialog.file
                    if (dir == null || name == null) {
                        onResult(false, "Cancelado")
                        return
                    }

                    val file = File(dir, name)
                    file.writeText(json)
                    onResult(true, null)
                } catch (e: Exception) {
                    onResult(false, e.message ?: "Error guardando archivo")
                }
            }

            override fun openJson(onResult: (Boolean, String?, String?) -> Unit) {
                try {
                    val dialog = FileDialog(frame, "Abrir portfolio (JSON)", FileDialog.LOAD).apply {
                        isVisible = true
                    }

                    val dir = dialog.directory
                    val name = dialog.file
                    if (dir == null || name == null) {
                        onResult(false, null, "Cancelado")
                        return
                    }

                    val file = File(dir, name)
                    val text = file.readText()
                    onResult(true, text, null)
                } catch (e: Exception) {
                    onResult(false, null, e.message ?: "Error leyendo archivo")
                }
            }
        }
    }
}
