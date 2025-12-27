package org.example.project

import androidx.compose.runtime.Composable

interface PortfolioStorage {
    suspend fun loadJsonOrNull(): String?
    suspend fun saveJson(json: String)
}

@Composable
expect fun rememberPortfolioStorage(
    fileName: String = "portfolio.json"
): PortfolioStorage
