package org.example.project.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Composable
fun PortfolioStateMenuButton(
    onSaveNow: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onReset: () -> Unit,
    onSaveAsJsonFile: () -> Unit,
    onOpenJsonFile: () -> Unit,
    containerColor: Color,
    textColor: Color,
    dividerColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menú portfolio")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = containerColor
        ) {

            DropdownMenuItem(
                text = { Text("Guardar portfolio", color = textColor) },
                onClick = {
                    expanded = false
                    onSaveNow()
                }
            )

            DropdownMenuItem(
                text = { Text("Guardar como… (JSON)", color = textColor) },
                onClick = {
                    expanded = false
                    onSaveAsJsonFile()
                }
            )

            HorizontalDivider(color = dividerColor)

            DropdownMenuItem(
                text = { Text("Exportar JSON", color = textColor) },
                onClick = {
                    expanded = false
                    onExportJson()
                }
            )

            DropdownMenuItem(
                text = { Text("Importar JSON", color = textColor) },
                onClick = {
                    expanded = false
                    onImportJson()
                }
            )

            DropdownMenuItem(
                text = { Text("Abrir archivo… (JSON)", color = textColor) },
                onClick = {
                    expanded = false
                    onOpenJsonFile()
                }
            )

            HorizontalDivider(color = dividerColor)

            DropdownMenuItem(
                text = { Text("Reset portfolio", color = textColor) },
                onClick = {
                    expanded = false
                    onReset()
                }
            )
        }
    }
}