package org.example.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import org.example.project.core.market.MarketSchedule
import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.Sector
import org.example.project.domain.model.StockSnapshot
import org.example.project.presentation.state.MarketState

/**
 * Repositorio de mercado.
 *
 * Responsabilidades:
 * - Mantener el estado global del mercado (StateFlow).
 * - Emitir actualizaciones en tiempo real de acciones (Flow).
 * - Control de mercado abierto/cerrado, pausa y velocidad de simulación.
 * - Exponer sesgos (trend y sector) que el Engine aplicará al calcular precios.
 * - Configurar si el mercado se abre/cierra automáticamente por horario y cuál es ese horario.
 * - Permitir override manual temporal aunque el auto esté activo.
 */
interface MarketRepository {

    /** Estado global para UI (Compose). */
    val marketState: StateFlow<MarketState>

    /** Updates de precio (para gráficos / estrategia / etc.) */
    val priceUpdates: Flow<StockSnapshot>

    fun getSnapshot(ticker: String): StockSnapshot?
    fun updateSnapshot(ticker: String, newSnapshot: StockSnapshot)

    /** Control del estado del mercado. */
    fun setMarketOpen(isOpen: Boolean)

    /** Pausa/reanuda la simulación (lógica real se aplica en Engine). */
    fun setPaused(isPaused: Boolean)

    /** Multiplicador de velocidad [0.25 .. 10.0] recomendado. */
    fun setSimSpeed(speed: Double)

    /** Sesgo global. */
    fun getTrendBiasPercent(): Double
    fun setTrend(trend: MarketTrend, biasPercent: Double)

    /** Sesgo por sector (noticias). */
    fun getSectorBiasPercent(sector: Sector): Double
    fun setSectorBias(sector: Sector, biasPercent: Double)

    /** Noticias. */
    fun pushNews(event: NewsEvent)

    /** Publicar lista consolidada para UI (MarketState.stocks). */
    fun publish()

    // -------------------------
    // Horario (AUTO)
    // -------------------------

    fun setAutoScheduleEnabled(enabled: Boolean)
    fun setMarketSchedule(schedule: MarketSchedule)

    // -------------------------
    // Override manual (NUEVO)
    // -------------------------

    /**
     * Activa un override manual hasta un instante dado.
     * - forcedOpen: estado forzado (ABRIR/CERRAR)
     * - untilEpochMs: hasta cuándo dura el override (epoch millis)
     *
     * Importante:
     * - Debe reflejarse en MarketState (manualOverrideActive + until)
     * - Y normalmente también en isOpen para que UI/Engine reaccionen al instante.
     */
    fun setManualOverride(forcedOpen: Boolean, untilEpochMs: Long)

    /**
     * Cancela el override manual (deja que AUTO vuelva a decidir).
     */
    fun clearManualOverride()

    // -------------------------
    // Conveniencia
    // -------------------------

    fun getStockPrice(ticker: String): Double? = getSnapshot(ticker)?.currentPrice

    fun isAutoScheduleEnabled(): Boolean = marketState.value.autoScheduleEnabled
    fun getMarketSchedule(): MarketSchedule = marketState.value.schedule

    fun isManualOverrideActive(nowEpochMs: Long): Boolean {
        val st = marketState.value
        return st.manualOverrideActive && st.manualOverrideUntilEpochMs > nowEpochMs
    }

    fun isManualOverrideActiveNow(): Boolean =
        isManualOverrideActive(Clock.System.now().toEpochMilliseconds())
}
