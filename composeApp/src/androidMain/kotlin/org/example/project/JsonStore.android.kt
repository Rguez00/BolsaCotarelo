package org.example.project

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberJsonStore(fileName: String): JsonStore {
    val context = LocalContext.current.applicationContext
    return remember(fileName) { AndroidJsonStore(context, fileName) }
}

private class AndroidJsonStore(
    private val context: Context,
    private val fileName: String
) : JsonStore {

    private fun targetFile(): File = File(context.filesDir, fileName)
    private fun tmpFile(): File = File(context.filesDir, "$fileName.tmp")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        val f = targetFile()
        if (!f.exists()) return@withContext null
        runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val target = targetFile()
        val tmp = tmpFile()

        // 1) escribe tmp
        tmp.writeText(text, Charsets.UTF_8)

        // 2) intenta rename atómico
        if (tmp.renameTo(target)) return@withContext Unit

        // 3) fallback: copia + replace
        runCatching {
            if (target.exists()) target.delete()
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }.getOrElse {
            // último intento: escribir directo
            target.writeText(text, Charsets.UTF_8)
            tmp.delete()
        }

        Unit
    }
}
