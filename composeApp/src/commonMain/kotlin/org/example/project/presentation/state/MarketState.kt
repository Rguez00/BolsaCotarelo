package org.example.project.presentation.state

import kotlinx.datetime.LocalTime
import org.example.project.core.market.MarketSchedule
import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.StockSnapshot

data class MarketState(
    val isOpen: Boolean = true,
    val isPaused: Boolean = false,

    val simSpeed: Double = 1.0,
    val trend: MarketTrend = MarketTrend.NEUTRAL,
    val marketTimeMillis: Long = 0L,

    /**
     * Si está activo, el MarketClock puede abrir/cerrar el mercado según "schedule".
     * Si está desactivado, el mercado se controla manualmente (botón ABRIR/CERRAR).
     */
    val autoScheduleEnabled: Boolean = false,

    /**
     * Override manual aunque el autoSchedule esté activo.
     * Si está activo, el usuario puede forzar ABRIR/CERRAR durante un tiempo.
     */
    val manualOverrideActive: Boolean = false,

    /**
     * Momento (epochMillis) hasta el que dura el override manual.
     * Si es 0 o si ya pasó, el override se considera expirado.
     */
    val manualOverrideUntilEpochMs: Long = 0L,

    // ✅ Default razonable para que “auto” funcione cuando lo pruebas
    val schedule: MarketSchedule = MarketSchedule(
        openTime = LocalTime(9, 0),
        closeTime = LocalTime(17, 0)
    ),

    val stocks: List<StockSnapshot> = emptyList(),
    val news: List<NewsEvent> = emptyList()
) {
    /**
     * Devuelve true si el override manual está activo y NO ha expirado.
     * Útil para que MarketClock no pise el estado mientras dura el override.
     */
    fun isManualOverrideActive(nowEpochMs: Long): Boolean {
        if (!manualOverrideActive) return false
        val until = manualOverrideUntilEpochMs
        if (until <= 0L) return false
        return nowEpochMs < until
    }

    companion object {
        /**
         * Duración por defecto del override manual cuando autoSchedule está ON.
         * (ej: 2 minutos). Ajustaremos si quieres otra.
         */
        const val DEFAULT_MANUAL_OVERRIDE_MS: Long = 2 * 60 * 1000L
    }
}
