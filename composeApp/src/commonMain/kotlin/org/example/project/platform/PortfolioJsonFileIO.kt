package org.example.project.platform

import androidx.compose.runtime.Composable

interface PortfolioJsonFileIO {
    fun saveJson(defaultFileName: String, json: String, onResult: (ok: Boolean, message: String?) -> Unit)
    fun openJson(onResult: (ok: Boolean, json: String?, message: String?) -> Unit)
}

@Composable
expect fun rememberPortfolioJsonFileIO(): PortfolioJsonFileIO
