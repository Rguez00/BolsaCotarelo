package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberJsonStore(fileName: String): JsonStore {
    val baseDir = remember {
        File(System.getProperty("user.home"), ".bolsa-kmp").apply { mkdirs() }
    }

    return remember(fileName) {
        DesktopJsonStore(baseDir, fileName)
    }
}

private class DesktopJsonStore(
    private val baseDir: File,
    private val fileName: String
) : JsonStore {

    private fun targetFile(): File = File(baseDir, fileName)
    private fun tmpFile(): File = File(baseDir, "$fileName.tmp")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        val f = targetFile()
        if (!f.exists()) return@withContext null
        runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val target = targetFile()
        val tmp = tmpFile()

        tmp.writeText(text, Charsets.UTF_8)
        if (target.exists()) target.delete()
        tmp.renameTo(target)

        Unit
    }
}
