package org.example.project

import androidx.compose.runtime.Composable

interface JsonStore {
    suspend fun read(): String?
    suspend fun write(text: String)
}

@Composable
expect fun rememberJsonStore(fileName: String): JsonStore
