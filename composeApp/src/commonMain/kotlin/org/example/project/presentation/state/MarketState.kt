package org.example.project.presentation.state

import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.StockSnapshot

data class MarketState(
    val isOpen: Boolean = true,
    val isPaused: Boolean = false,

    // Velocidad de simulación: 1.0 = normal, 2.0 = el doble de rápido, 5.0 = modo rápido...
    val simSpeed: Double = 1.0,

    // Tendencia global del mercado (alcista/bajista/neutra)
    val trend: MarketTrend = MarketTrend.NEUTRAL,

    // "Reloj" interno opcional para soportar horario de mercado y modo rápido.
    // (Puede representar "tiempo simulado" o "tiempo del mercado" según implementación.)
    val marketTimeMillis: Long = 0L,

    val stocks: List<StockSnapshot> = emptyList(),
    val news: List<NewsEvent> = emptyList()
)
