package org.example.project.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.example.project.core.market.MarketClock
import org.example.project.core.market.MarketSchedule
import org.example.project.data.repository.MarketRepository
import org.example.project.data.repository.PortfolioRepository
import org.example.project.domain.strategy.RepoStrategyMarketBridge
import org.example.project.domain.strategy.RepoStrategyPortfolioBridge
import org.example.project.domain.strategy.StrategiesRepository
import org.example.project.domain.strategy.StrategyEngine
import org.example.project.presentation.state.MarketState

class MarketEngine(
    private val marketRepo: MarketRepository,
    private val portfolioRepo: PortfolioRepository,
    private val strategiesRepo: StrategiesRepository,
    externalScope: CoroutineScope? = null
) {
    companion object {
        // Tiempo de “override manual” cuando autoSchedule está activo
        private const val DEFAULT_MANUAL_OVERRIDE_MS: Long = 5 * 60 * 1000L // 5 min
    }

    private val engineJob: Job = SupervisorJob(externalScope?.coroutineContext?.get(Job))
    private val workerScope: CoroutineScope = CoroutineScope(Dispatchers.Default + engineJob)
    private val controlScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default.limitedParallelism(1) + engineJob)

    val marketState: StateFlow<MarketState> = marketRepo.marketState

    private val updaterJobs: MutableMap<String, Job> = mutableMapOf()
    private val marketClock = MarketClock(marketRepo, workerScope)

    private var trendJob: Job? = null
    private var newsJob: Job? = null
    private var strategyEngine: StrategyEngine? = null

    private val updater = SingleStockPriceUpdater(marketRepo)

    private val generator: NewsAndTrendGenerator by lazy {
        NewsAndTrendGenerator(marketRepo, workerScope)
    }

    private var stateSyncJob: Job? = null

    fun start() {
        if (!engineJob.isActive) return

        marketClock.start()

        if (stateSyncJob?.isActive != true) {
            stateSyncJob = controlScope.launch {
                marketState
                    .map { it.isOpen to it.isPaused }
                    .distinctUntilChanged()
                    .collect { (open, paused) ->
                        syncEngineTo(open = open, paused = paused)
                    }
            }
        }

        controlScope.launch {
            val st = marketState.value
            syncEngineTo(open = st.isOpen, paused = st.isPaused)
        }
    }

    fun close() {
        if (!engineJob.isActive) return

        controlScope.launch {
            marketClock.stop()

            stateSyncJob?.cancel()
            stateSyncJob = null

            stopAllTickersLocked()
            stopGlobalGeneratorsLocked()
            stopStrategiesLocked()
        }.invokeOnCompletion {
            engineJob.cancel()
        }
    }

    // ============================================================
    // CONTROLES
    // ============================================================

    fun setPaused(paused: Boolean) {
        marketRepo.setPaused(paused)
        // listener sincroniza
    }

    /**
     * ✅ ABRIR/CERRAR:
     * - AUTO OFF -> manual normal.
     * - AUTO ON  -> override temporal (sin desactivar auto).
     *
     * Importante:
     * - setManualOverride() YA aplica isOpen=open en el repo (efecto inmediato).
     * - MarketClock respeta el override hasta que expire.
     */
    fun setMarketOpen(open: Boolean) {
        val st = marketState.value

        if (!st.autoScheduleEnabled) {
            // Manual puro
            runCatching { marketRepo.clearManualOverride() } // por si venías de override
            marketRepo.setMarketOpen(open)

            if (open && st.isPaused) marketRepo.setPaused(false)
            return
        }

        // AUTO ON: override temporal
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val untilMs = nowMs + DEFAULT_MANUAL_OVERRIDE_MS

        marketRepo.setManualOverride(forcedOpen = open, untilEpochMs = untilMs)

        // Si abres manualmente, reanuda
        if (open && st.isPaused) {
            marketRepo.setPaused(false)
        }
        // ❌ No llamamos checkNow aquí: el clock ya respetará el override.
    }

    fun setSimSpeed(speed: Double) {
        marketRepo.setSimSpeed(speed)
    }

    // ============================================================
    // Horario automático
    // ============================================================

    fun setAutoScheduleEnabled(enabled: Boolean) {
        marketRepo.setAutoScheduleEnabled(enabled)

        // Si desactivas auto, ya no tiene sentido un override
        if (!enabled) {
            runCatching { marketRepo.clearManualOverride() }
        }

        marketClock.checkNow()
    }

    fun setMarketSchedule(schedule: MarketSchedule) {
        marketRepo.setMarketSchedule(schedule)
        marketClock.checkNow()
    }

    // ============================================================
    // LÓGICA INTERNA
    // ============================================================

    private fun syncEngineTo(open: Boolean, paused: Boolean) {
        if (!open || paused) {
            stopAllTickersLocked()
            stopGlobalGeneratorsLocked()
            stopStrategiesLocked()
            return
        }

        startGlobalGeneratorsIfNeededLocked()
        startStrategiesIfNeededLocked()

        val stocks = marketState.value.stocks
        for (s in stocks) {
            startTickerLocked(normalizeTicker(s.ticker))
        }
    }

    private fun startTickerLocked(tickerNorm: String) {
        if (updaterJobs[tickerNorm]?.isActive == true) return

        val job = workerScope.launch { updater.run(tickerNorm) }

        job.invokeOnCompletion {
            controlScope.launch { updaterJobs.remove(tickerNorm) }
        }

        updaterJobs[tickerNorm] = job
    }

    private fun stopAllTickersLocked() {
        val jobs = updaterJobs.values.toList()
        updaterJobs.clear()
        jobs.forEach { it.cancel() }
    }

    private fun startGlobalGeneratorsIfNeededLocked() {
        if (trendJob?.isActive != true) {
            trendJob = workerScope.launch { generator.runTrend() }
        }
        if (newsJob?.isActive != true) {
            newsJob = workerScope.launch { generator.runNews() }
        }
    }

    private fun stopGlobalGeneratorsLocked() {
        trendJob?.cancel()
        newsJob?.cancel()
        trendJob = null
        newsJob = null
    }

    private fun startStrategiesIfNeededLocked() {
        if (strategyEngine != null) return
        strategyEngine = StrategyEngine(
            market = RepoStrategyMarketBridge(marketRepo),
            portfolio = RepoStrategyPortfolioBridge(portfolioRepo),
            strategiesRepo = strategiesRepo
        ).also { it.start() }
    }

    private fun stopStrategiesLocked() {
        strategyEngine?.close()
        strategyEngine = null
    }

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()
}
