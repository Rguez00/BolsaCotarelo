package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberPortfolioStorage(fileName: String): PortfolioStorage {
    val baseDir = remember {
        File(System.getProperty("user.home"), ".bolsa-kmp").apply { mkdirs() }
    }

    return remember(fileName) {
        object : PortfolioStorage {
            private fun target(): File = File(baseDir, fileName)
            private fun tmp(): File = File(baseDir, "$fileName.tmp")

            override suspend fun loadJsonOrNull(): String? = withContext(Dispatchers.IO) {
                val f = target()
                if (!f.exists()) return@withContext null
                runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
            }

            override suspend fun saveJson(json: String) = withContext(Dispatchers.IO) {
                val t = target()
                val tmpFile = tmp()
                tmpFile.writeText(json, Charsets.UTF_8)
                if (t.exists()) t.delete()
                tmpFile.renameTo(t)
                Unit
            }
        }
    }
}
