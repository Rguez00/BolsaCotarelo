package org.example.project.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Composable
fun PortfolioStateMenuButton(
    onSaveAsJsonFile: () -> Unit,
    onOpenJsonFile: () -> Unit,
    containerColor: Color,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Menú portfolio",
                tint = textColor
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = containerColor
        ) {

            DropdownMenuItem(
                text = { Text("Guardar como… (JSON)", color = textColor) },
                onClick = {
                    expanded = false
                    onSaveAsJsonFile()
                }
            )

            DropdownMenuItem(
                text = { Text("Abrir archivo… (JSON)", color = textColor) },
                onClick = {
                    expanded = false
                    onOpenJsonFile()
                }
            )
        }
    }
}
