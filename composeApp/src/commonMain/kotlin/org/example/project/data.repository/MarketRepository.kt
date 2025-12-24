package org.example.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
 */
interface MarketRepository {

    /** Estado global para UI (Compose). */
    val marketState: StateFlow<MarketState>

    /**
     * Flow que emite snapshots cuando cambia el precio de una acción.
     * Útil para gráficos, animaciones y repos dependientes (portfolio).
     */
    val priceUpdates: Flow<StockSnapshot>

    /** Snapshot actual (o null si no existe el ticker). */
    fun getSnapshot(ticker: String): StockSnapshot?

    /** Actualiza el snapshot de un ticker (y debe publicar / emitir updates). */
    fun updateSnapshot(ticker: String, newSnapshot: StockSnapshot)

    /** Control del estado del mercado. */
    fun setMarketOpen(isOpen: Boolean)

    /** Pausa/reanuda la simulación (la lógica real se aplica en el Engine). */
    fun setPaused(isPaused: Boolean)

    /**
     * Multiplicador de velocidad de simulación.
     * Recomendado: [0.25 .. 10.0]
     */
    fun setSimSpeed(speed: Double)

    /**
     * Sesgo global por “tick” (en porcentaje).
     * Ejemplo: +0.10 => +0.10% extra por tick (aprox).
     */
    fun getTrendBiasPercent(): Double
    fun setTrend(trend: MarketTrend, biasPercent: Double)

    /**
     * Sesgo por sector (en porcentaje) aplicado temporalmente por noticias.
     */
    fun getSectorBiasPercent(sector: Sector): Double
    fun setSectorBias(sector: Sector, biasPercent: Double)

    /** Añade una noticia al panel (histórico acotado en implementación). */
    fun pushNews(event: NewsEvent)

    /**
     * Publica el listado consolidado para UI.
     * (Ej: refrescar MarketState.stocks)
     */
    fun publish()

    // -------------------------
    // Conveniencia (NO rompe nada)
    // -------------------------
    /** Precio actual directo (si existe el snapshot). */
    fun getStockPrice(ticker: String): Double? = getSnapshot(ticker)?.currentPrice
}
