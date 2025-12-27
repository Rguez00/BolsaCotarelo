package org.example.project.presentation.state

import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.StockSnapshot
import org.example.project.core.market.MarketSchedule
import kotlinx.datetime.LocalTime  // ← CAMBIO: kotlinx.datetime, NO java.time

data class MarketState(
    val isOpen: Boolean = true,
    val isPaused: Boolean = false,

    val simSpeed: Double = 1.0,
    val trend: MarketTrend = MarketTrend.NEUTRAL,
    val marketTimeMillis: Long = 0L,

    val schedule: MarketSchedule = MarketSchedule(
        openTime = LocalTime(0, 0),   // Dentro de 1 minuto
        closeTime = LocalTime(0, 5)   // Cierre en 3 minutos
    ),

    val stocks: List<StockSnapshot> = emptyList(),
    val news: List<NewsEvent> = emptyList()
)