package org.example.project.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
actual fun rememberPortfolioJsonFileIO(): PortfolioJsonFileIO {
    val context = LocalContext.current

    var pendingSave by remember { mutableStateOf<Pair<String, String>?>(null) }
    var saveCallback by remember { mutableStateOf<((Boolean, String?) -> Unit)?>(null) }

    var openCallback by remember { mutableStateOf<((Boolean, String?, String?) -> Unit)?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val cb = saveCallback
        val (fileName, json) = pendingSave ?: ("" to "")
        pendingSave = null
        saveCallback = null

        if (uri == null) {
            cb?.invoke(false, "Cancelado")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            writeTextToUri(context, uri, json)
        }.onSuccess {
            cb?.invoke(true, null)
        }.onFailure { e ->
            cb?.invoke(false, e.message ?: "Error guardando")
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val cb = openCallback
        openCallback = null

        if (uri == null) {
            cb?.invoke(false, null, "Cancelado")
            return@rememberLauncherForActivityResult
        }

        runCatching {
            readTextFromUri(context, uri)
        }.onSuccess { text ->
            cb?.invoke(true, text, null)
        }.onFailure { e ->
            cb?.invoke(false, null, e.message ?: "Error leyendo")
        }
    }

    return remember {
        object : PortfolioJsonFileIO {

            override fun saveJson(defaultFileName: String, json: String, onResult: (Boolean, String?) -> Unit) {
                pendingSave = defaultFileName to json
                saveCallback = onResult
                createDocLauncher.launch(defaultFileName)
            }

            override fun openJson(onResult: (Boolean, String?, String?) -> Unit) {
                openCallback = onResult
                openDocLauncher.launch(arrayOf("application/json", "text/*"))
            }
        }
    }
}

private fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { os ->
        OutputStreamWriter(os).use { it.write(text) }
    } ?: error("No se pudo abrir OutputStream")
}

private fun readTextFromUri(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri)?.use { ins ->
        BufferedReader(InputStreamReader(ins)).use { return it.readText() }
    } ?: error("No se pudo abrir InputStream")
}
