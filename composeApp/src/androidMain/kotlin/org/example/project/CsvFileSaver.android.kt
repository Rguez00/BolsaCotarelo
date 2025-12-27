package org.example.project

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

@Composable
actual fun rememberCsvFileSaver(): CsvFileSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    data class Pending(
        val fileName: String,
        val csv: String,
        val onResult: (Boolean, String?) -> Unit
    )

    var pending by remember { mutableStateOf<Pending?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val p = pending
        pending = null
        if (p == null) return@rememberLauncherForActivityResult

        if (uri == null) {
            p.onResult(false, "Export cancelado")
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val (ok, msg) = withContext(Dispatchers.IO) {
                writeToUri(context, uri, p.csv)
            }
            p.onResult(ok, msg)
        }
    }

    return remember {
        object : CsvFileSaver {
            override fun saveCsv(
                suggestedFileName: String,
                csvText: String,
                onResult: (Boolean, String?) -> Unit
            ) {
                // ✅ Asegura extensión .csv (Android suele crear sin extensión si no la pasas)
                val name = if (suggestedFileName.endsWith(".csv", ignoreCase = true)) {
                    suggestedFileName
                } else {
                    "$suggestedFileName.csv"
                }

                pending = Pending(name, csvText, onResult)
                launcher.launch(name) // inputName para CreateDocument
            }
        }
    }
}

private fun writeToUri(context: Context, uri: Uri, text: String): Pair<Boolean, String?> {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write(text)
            }
        } ?: return false to "No se pudo abrir el destino"

        true to null
    } catch (e: Exception) {
        false to (e.message ?: "Error al escribir el archivo")
    }
}
