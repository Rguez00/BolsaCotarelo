package org.example.project

import androidx.compose.runtime.Composable

/**
 * Guardado real de CSV a fichero:
 * - Android: SAF (CreateDocument)
 * - Desktop: FileDialog
 */
interface CsvFileSaver {
    fun saveCsv(
        suggestedFileName: String,
        csvText: String,
        onResult: (success: Boolean, message: String?) -> Unit
    )
}

@Composable
expect fun rememberCsvFileSaver(): CsvFileSaver
