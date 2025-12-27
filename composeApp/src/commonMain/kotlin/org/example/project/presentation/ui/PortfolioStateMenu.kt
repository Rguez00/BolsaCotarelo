package org.example.project.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun PortfolioStateMenuButton(
    onSaveNow: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onReset: () -> Unit,
    onSaveAsJsonFile: () -> Unit,
    onOpenJsonFile: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menú portfolio")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            // ===== Guardado =====
            DropdownMenuItem(
                text = { Text("Guardar portfolio") },
                onClick = {
                    expanded = false
                    onSaveNow()
                }
            )

            DropdownMenuItem(
                text = { Text("Guardar como… (JSON)") },
                onClick = {
                    expanded = false
                    onSaveAsJsonFile()
                }
            )

            Divider()

            // ===== Import / Export =====
            DropdownMenuItem(
                text = { Text("Exportar JSON") },
                onClick = {
                    expanded = false
                    onExportJson()
                }
            )

            DropdownMenuItem(
                text = { Text("Importar JSON") },
                onClick = {
                    expanded = false
                    onImportJson()
                }
            )

            DropdownMenuItem(
                text = { Text("Abrir archivo… (JSON)") },
                onClick = {
                    expanded = false
                    onOpenJsonFile()
                }
            )

            Divider()

            // ===== Reset =====
            DropdownMenuItem(
                text = { Text("Reset portfolio") },
                onClick = {
                    expanded = false
                    onReset()
                }
            )
        }
    }
}
