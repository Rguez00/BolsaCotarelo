package org.example.project

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberPortfolioStorage(fileName: String): PortfolioStorage {
    val context = LocalContext.current.applicationContext

    return remember(fileName) {
        object : PortfolioStorage {

            private fun targetFile(): File = File(context.filesDir, fileName)
            private fun tmpFile(): File = File(context.filesDir, "$fileName.tmp")

            override suspend fun loadJsonOrNull(): String? = withContext(Dispatchers.IO) {
                val f = targetFile()
                if (!f.exists()) return@withContext null
                runCatching { f.readText(Charsets.UTF_8) }.getOrNull()
            }

            override suspend fun saveJson(json: String) = withContext(Dispatchers.IO) {
                val target = targetFile()
                val tmp = tmpFile()

                // 1) Escribimos al tmp
                tmp.writeText(json, Charsets.UTF_8)

                // 2) Reemplazo best-effort
                if (target.exists()) target.delete()

                // 3) renameTo devuelve Boolean -> NO lo devolvemos
                tmp.renameTo(target)

                // ✅ Fuerza Unit como retorno (por si el compilador intenta inferir otra cosa)
                Unit
            }
        }
    }
}
