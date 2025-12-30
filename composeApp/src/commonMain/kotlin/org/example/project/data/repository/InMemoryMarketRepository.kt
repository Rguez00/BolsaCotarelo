package org.example.project.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.example.project.core.market.MarketSchedule
import org.example.project.core.util.ThreadSafeMap
import org.example.project.domain.model.*
import org.example.project.presentation.state.MarketState

class InMemoryMarketRepository(
    initialStocks: List<Stock>
) : MarketRepository {

    // =========================
    // Storage thread-safe
    // =========================
    private val snapshots = ThreadSafeMap<String, StockSnapshot>()
    private val sectorBiasPercent = ThreadSafeMap<String, Double>() // key = Sector.name

    private val orderedTickers: List<String> =
        initialStocks.map { normalizeTicker(it.ticker) }.distinct().sorted()

    @Volatile
    private var trendBiasPercent: Double = 0.0

    private val publishLock = Any()

    @Volatile
    private var lastPublishedStocks: List<StockSnapshot> = emptyList()

    // =========================
    // Flow + StateFlow
    // =========================
    private val _marketState = MutableStateFlow(
        MarketState(
            isOpen = true,
            isPaused = false,
            simSpeed = 1.0,
            trend = MarketTrend.NEUTRAL,
            marketTimeMillis = 0L,
            autoScheduleEnabled = false,
            // ✅ override fields ya existen en tu MarketState actual
            manualOverrideActive = false,
            manualOverrideUntilEpochMs = 0L,
            stocks = emptyList(),
            news = emptyList()
        )
    )
    override val marketState: StateFlow<MarketState> = _marketState.asStateFlow()

    private val _priceUpdates = MutableSharedFlow<StockSnapshot>(
        replay = 0,
        extraBufferCapacity = 128
    )
    override val priceUpdates: Flow<StockSnapshot> = _priceUpdates.asSharedFlow()

    // =========================
    // Init
    // =========================
    init {
        initialStocks.forEach { stock ->
            val t = normalizeTicker(stock.ticker)
            val price = stock.initialPrice

            snapshots.put(
                t,
                StockSnapshot(
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
            )
        }

        Sector.values().forEach { sector ->
            sectorBiasPercent.put(sector.name, 0.0)
        }

        publish()
    }

    // =========================
    // MarketRepository
    // =========================
    override fun getSnapshot(ticker: String): StockSnapshot? =
        snapshots.get(normalizeTicker(ticker))

    override fun updateSnapshot(ticker: String, newSnapshot: StockSnapshot) {
        val t = normalizeTicker(ticker)
        val safeSnapshot = if (newSnapshot.ticker == t) newSnapshot else newSnapshot.copy(ticker = t)

        snapshots.put(t, safeSnapshot)
        _priceUpdates.tryEmit(safeSnapshot)

        publish()
    }

    override fun setMarketOpen(isOpen: Boolean) {
        println("MarketRepo(${this.hashCode()}).setMarketOpen($isOpen)")
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
    // Horario automático
    // =========================
    override fun setAutoScheduleEnabled(enabled: Boolean) {
        println("MarketRepo.setAutoScheduleEnabled($enabled)")

        _marketState.update { st ->
            if (enabled) {
                // ✅ Si activas AUTO, cancelamos cualquier override manual
                st.copy(
                    autoScheduleEnabled = true,
                    manualOverrideActive = false,
                    manualOverrideUntilEpochMs = 0L
                )
            } else {
                st.copy(autoScheduleEnabled = false)
            }
        }
    }

    override fun setMarketSchedule(schedule: MarketSchedule) {
        println("MarketRepo.setMarketSchedule(open=${schedule.openTime} close=${schedule.closeTime})")
        _marketState.update { it.copy(schedule = schedule) }
    }

    // =========================
    // Override manual (NUEVO)
    // =========================
    override fun setManualOverride(forcedOpen: Boolean, untilEpochMs: Long) {
        println("MarketRepo.setManualOverride(forcedOpen=$forcedOpen until=$untilEpochMs)")

        _marketState.update { st ->
            st.copy(
                // ✅ efecto inmediato
                isOpen = forcedOpen,

                manualOverrideActive = true,
                manualOverrideUntilEpochMs = untilEpochMs
            )
        }
    }

    override fun clearManualOverride() {
        println("MarketRepo.clearManualOverride()")

        _marketState.update { st ->
            st.copy(
                manualOverrideActive = false,
                manualOverrideUntilEpochMs = 0L
            )
        }
        // ✅ Importante: no “recalculamos” aquí el horario.
        // Eso lo hará MarketClock en el siguiente tick o con checkNow().
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
    // Publish (optimizado)
    // =========================
    override fun publish() {
        val list = synchronized(publishLock) { buildStocksList() }

        if (list == lastPublishedStocks) return
        lastPublishedStocks = list

        _marketState.update { st ->
            st.copy(stocks = list)
        }
    }

    private fun buildStocksList(): List<StockSnapshot> {
        val result = ArrayList<StockSnapshot>(orderedTickers.size)
        for (t in orderedTickers) {
            snapshots.get(t)?.let(result::add)
        }
        return result
    }

    // =========================
    // Helper opcional
    // =========================
    suspend fun expireSectorBiasLater(sector: Sector, delayMs: Long) {
        if (delayMs <= 0) return
        val expected = getSectorBiasPercent(sector)
        delay(delayMs)
        if (getSectorBiasPercent(sector) == expected) {
            setSectorBias(sector, 0.0)
        }
    }

    private fun normalizeTicker(raw: String): String = raw.trim().uppercase()
}
