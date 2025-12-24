package org.example.project.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.example.project.core.util.ThreadSafeMap
import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.Sector
import org.example.project.domain.model.Stock
import org.example.project.domain.model.StockSnapshot
import org.example.project.presentation.state.MarketState

class InMemoryMarketRepository(
    initialStocks: List<Stock>
) : MarketRepository {

    // =========================
    // Storage thread-safe
    // =========================
    private val snapshots = ThreadSafeMap<String, StockSnapshot>()
    private val sectorBiasPercent = ThreadSafeMap<String, Double>() // key = Sector.name

    // Orden estable para UI (evita sort en cada publish)
    private val orderedTickers: List<String> =
        initialStocks
            .map { normalizeTicker(it.ticker) }
            .distinct()
            .sorted()

    // Bias global por tick (% aprox). Ej: +0.10 => +0.10% por tick
    @Volatile
    private var trendBiasPercent: Double = 0.0

    // Evita que múltiples tickers reconstruyan/publquen la lista a la vez
    private val publishLock = Any()

    // =========================
    // Flow + StateFlow (requisitos)
    // =========================
    private val _marketState = MutableStateFlow(
        MarketState(
            isOpen = true,
            isPaused = false,
            simSpeed = 1.0,
            trend = MarketTrend.NEUTRAL,
            stocks = emptyList(),
            news = emptyList()
        )
    )
    override val marketState: StateFlow<MarketState> = _marketState

    private val _priceUpdates = MutableSharedFlow<StockSnapshot>(
        replay = 0,
        extraBufferCapacity = 128
    )
    override val priceUpdates: Flow<StockSnapshot> = _priceUpdates

    // =========================
    // Init
    // =========================
    init {
        initialStocks.forEach { stock ->
            val t = normalizeTicker(stock.ticker)
            val price = stock.initialPrice

            val snap = StockSnapshot(
                name = stock.name,
                ticker = t,
                sector = stock.sector,
                volatility = stock.volatility,

                currentPrice = price,
                openPrice = price,
                highPrice = price,
                lowPrice = price,

                changeEuro = 0.0,
                changePercent = 0.0,

                volume = 0L,
                priceHistory = listOf(price)
            )

            snapshots.put(t, snap)
        }

        // Inicializamos bias de sectores a 0
        Sector.values().forEach { sector ->
            sectorBiasPercent.put(sector.name, 0.0)
        }

        publish()
    }

    // =========================
    // MarketRepository implementation
    // =========================
    override fun getSnapshot(ticker: String): StockSnapshot? =
        snapshots.get(normalizeTicker(ticker))

    override fun updateSnapshot(ticker: String, newSnapshot: StockSnapshot) {
        val t = normalizeTicker(ticker)

        // Guardamos snapshot (forzamos ticker normalizado por seguridad)
        val safeSnapshot = if (newSnapshot.ticker == t) newSnapshot else newSnapshot.copy(ticker = t)
        snapshots.put(t, safeSnapshot)

        // Flow de cambios de precios (requisito)
        _priceUpdates.tryEmit(safeSnapshot)

        // Publicación consolidada para UI (lista estable)
        publish()
    }

    override fun setMarketOpen(isOpen: Boolean) {
        _marketState.update { it.copy(isOpen = isOpen) }
    }

    override fun setPaused(isPaused: Boolean) {
        _marketState.update { it.copy(isPaused = isPaused) }
    }

    override fun setSimSpeed(speed: Double) {
        val safe = speed.coerceIn(0.25, 10.0)
        _marketState.update { it.copy(simSpeed = safe) }
    }

    // =========================
    // Trend global
    // =========================
    override fun getTrendBiasPercent(): Double = trendBiasPercent

    override fun setTrend(trend: MarketTrend, biasPercent: Double) {
        trendBiasPercent = biasPercent
        _marketState.update { it.copy(trend = trend) }
    }

    // =========================
    // Bias por sector
    // =========================
    override fun getSectorBiasPercent(sector: Sector): Double =
        sectorBiasPercent.get(sector.name) ?: 0.0

    override fun setSectorBias(sector: Sector, biasPercent: Double) {
        sectorBiasPercent.put(sector.name, biasPercent)
    }

    // =========================
    // Noticias
    // =========================
    override fun pushNews(event: NewsEvent) {
        _marketState.update { current ->
            val updated = (current.news + event).takeLast(20)
            current.copy(news = updated)
        }
    }

    // =========================
    // Publish consolidado
    // =========================
    override fun publish() {
        // Evitamos “pisarnos” si varios tickers publican a la vez
        val list = synchronized(publishLock) { buildStocksList() }
        _marketState.update { it.copy(stocks = list) }
    }

    private fun buildStocksList(): List<StockSnapshot> {
        // Orden estable sin hacer sort cada tick
        val result = ArrayList<StockSnapshot>(orderedTickers.size)
        for (t in orderedTickers) {
            val snap = snapshots.get(t)
            if (snap != null) result.add(snap)
        }
        return result
    }

    // =========================
    // Helper opcional (seguro)
    // =========================
    suspend fun expireSectorBiasLater(sector: Sector, delayMs: Long) {
        if (delayMs <= 0) return
        // Capturamos el bias actual para no borrar uno “nuevo” posterior
        val expected = getSectorBiasPercent(sector)
        delay(delayMs)
        if (getSectorBiasPercent(sector) == expected) {
            setSectorBias(sector, 0.0)
        }
    }

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()
}
