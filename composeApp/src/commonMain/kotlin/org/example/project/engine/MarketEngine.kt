package org.example.project.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import org.example.project.data.repository.MarketRepository
import org.example.project.data.repository.PortfolioRepository
import org.example.project.domain.strategy.RepoStrategyMarketBridge
import org.example.project.domain.strategy.RepoStrategyPortfolioBridge
import org.example.project.domain.strategy.StrategiesRepository
import org.example.project.domain.strategy.StrategyEngine
import org.example.project.presentation.state.MarketState

/**
 * Motor de simulación:
 * - 1 coroutine por ticker.
 * - 2 coroutines globales: tendencia + noticias.
 * - + Estrategias automáticas (StrategyEngine) escuchando cambios de precios.
 *
 * Corrección importante:
 * - Acceso thread-safe a updaterJobs y jobs globales (control serializado).
 * - Cuando se PAUSA o se CIERRA el mercado: se paran tickers + news/trend + strategies.
 */
class MarketEngine(
    private val marketRepo: MarketRepository,
    private val portfolioRepo: PortfolioRepository,
    private val strategiesRepo: StrategiesRepository,
    externalScope: CoroutineScope? = null
) {
    // Job del engine:
    private val engineJob: Job = SupervisorJob(externalScope?.coroutineContext?.get(Job))

    // Scope de trabajo pesado (tickers / generators)
    private val workerScope: CoroutineScope = CoroutineScope(Dispatchers.Default + engineJob)

    // Scope de control SERIAL (start/stop jobs, tocar maps) => evita carreras
    private val controlScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default.limitedParallelism(1) + engineJob)

    val marketState: StateFlow<MarketState> = marketRepo.marketState

    // 1 job por ticker (clave: ticker normalizado)
    private val updaterJobs: MutableMap<String, Job> = mutableMapOf()

    // Jobs globales
    private var trendJob: Job? = null
    private var newsJob: Job? = null

    // Estrategias automáticas
    private var strategyEngine: StrategyEngine? = null

    // Reutilizamos updater (no crear objetos en bucle)
    private val updater = SingleStockPriceUpdater(marketRepo)

    // ÚNICO generador global (no recrearlo)
    private val generator: NewsAndTrendGenerator by lazy {
        NewsAndTrendGenerator(marketRepo, workerScope)
    }

    // ============================================================
    // START / STOP
    // ============================================================

    fun startAllTickers() {
        if (!engineJob.isActive) return
        controlScope.launch {
            val st = marketState.value
            syncEngineTo(open = st.isOpen, paused = st.isPaused)
        }
    }

    fun startTicker(ticker: String) {
        if (!engineJob.isActive) return
        val t = normalizeTicker(ticker)

        controlScope.launch {
            val st = marketState.value
            if (!st.isOpen || st.isPaused) return@launch

            startGlobalGeneratorsIfNeededLocked()
            startStrategiesIfNeededLocked()
            startTickerLocked(t)
        }
    }

    fun stopTicker(ticker: String) {
        val t = normalizeTicker(ticker)
        if (!engineJob.isActive) return

        controlScope.launch {
            updaterJobs.remove(t)?.cancel()
        }
    }

    fun stopAllTickers() {
        if (!engineJob.isActive) return
        controlScope.launch {
            stopAllTickersLocked()
        }
    }

    /**
     * Para TODO (tickers + generators + strategies) sin cerrar el engine.
     * Útil si quisieras “resetear” en runtime.
     */
    fun stopAll() {
        if (!engineJob.isActive) return
        controlScope.launch {
            stopAllTickersLocked()
            stopGlobalGeneratorsLocked()
            stopStrategiesLocked()
        }
    }

    /**
     * Cierre final del engine (Desktop onClose / Android onDestroy).
     * Cancela TODO lo del motor, sin tocar scopes externos.
     */
    fun close() {
        if (!engineJob.isActive) return

        controlScope.launch {
            stopAllTickersLocked()
            stopGlobalGeneratorsLocked()
            stopStrategiesLocked()
        }.invokeOnCompletion {
            // cancela todo el árbol de jobs (worker + control)
            engineJob.cancel()
        }
    }

    // ============================================================
    // CONTROLES (enunciado)
    // ============================================================

    fun setPaused(paused: Boolean) {
        marketRepo.setPaused(paused)
        if (!engineJob.isActive) return

        // NO dependemos del marketState “recién emitido”; usamos el parámetro
        controlScope.launch {
            val open = marketState.value.isOpen
            syncEngineTo(open = open, paused = paused)
        }
    }

    fun setMarketOpen(open: Boolean) {
        marketRepo.setMarketOpen(open)
        if (!engineJob.isActive) return

        // Igual: usamos el parámetro 'open' para evitar estado desfasado
        controlScope.launch {
            val paused = marketState.value.isPaused
            syncEngineTo(open = open, paused = paused)
        }
    }

    fun setSimSpeed(speed: Double) {
        marketRepo.setSimSpeed(speed)
        // No reiniciamos jobs:
        // - SingleStockPriceUpdater consulta state.simSpeed.
        // - NewsAndTrendGenerator consulta simSpeed en delays.
    }

    // ============================================================
    // LÓGICA INTERNA (SIEMPRE en controlScope)
    // ============================================================

    private fun syncEngineTo(open: Boolean, paused: Boolean) {
        if (!open || paused) {
            // congelar TODO
            stopAllTickersLocked()
            stopGlobalGeneratorsLocked()
            stopStrategiesLocked()
            return
        }

        // mercado abierto y no pausado => arrancar TODO
        startGlobalGeneratorsIfNeededLocked()
        startStrategiesIfNeededLocked()

        val stocks = marketState.value.stocks
        for (s in stocks) {
            startTickerLocked(normalizeTicker(s.ticker))
        }
    }

    private fun startTickerLocked(tickerNorm: String) {
        if (updaterJobs[tickerNorm]?.isActive == true) return

        val job = workerScope.launch {
            updater.run(tickerNorm)
        }

        // Limpieza automática (pero la mutación del map la hacemos serializada)
        job.invokeOnCompletion {
            controlScope.launch {
                updaterJobs.remove(tickerNorm)
            }
        }

        updaterJobs[tickerNorm] = job
    }

    private fun stopAllTickersLocked() {
        // snapshot para cancelar fuera de iteraciones raras
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

    // ============================================================
    // Estrategias (SIEMPRE en controlScope)
    // ============================================================

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

    // ============================================================
    // Helpers
    // ============================================================

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()
}
