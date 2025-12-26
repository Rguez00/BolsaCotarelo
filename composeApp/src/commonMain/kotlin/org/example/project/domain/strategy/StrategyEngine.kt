package org.example.project.domain.strategy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Engine que ejecuta estrategias automáticas reaccionando a cambios del mercado.
 *
 * - Escucha cambios de precio (por ticker) vía Flow.
 * - Evalúa reglas activas (StrategiesRepository).
 * - Ejecuta buy/sell usando Portfolio bridge (thread-safe en repo).
 * - Emite triggers en tiempo real (SharedFlow) y guarda un historial (StateFlow).
 */
class StrategyEngine(
    private val market: StrategyMarketBridge,
    private val portfolio: StrategyPortfolioBridge,
    private val strategiesRepo: StrategiesRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val lastFiredMutex = Mutex()
    private val lastFiredByRule = mutableMapOf<Int, Long>() // ruleId -> timeMillis

    // Triggers en tiempo real (para UI / logging)
    private val _triggers = MutableSharedFlow<StrategyTrigger>(extraBufferCapacity = 64)
    val triggers: SharedFlow<StrategyTrigger> = _triggers

    // Historial en memoria (para panel/lista)
    private val triggersMutex = Mutex()
    private val _triggerHistory = MutableStateFlow<List<StrategyTrigger>>(emptyList())
    val triggerHistory: StateFlow<List<StrategyTrigger>> = _triggerHistory

    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = scope.launch {
            market.priceUpdates.collectLatest { ticker ->
                val rules = strategiesRepo.rules.value
                    .asSequence()
                    .filter { it.enabled }
                    .filter { it.ticker == null || it.ticker == ticker }
                    .toList()

                if (rules.isEmpty()) return@collectLatest
                if (!market.isMarketOpen()) return@collectLatest

                val snap = market.getSnapshot(ticker) ?: return@collectLatest

                for (rule in rules) {
                    if (!canFire(rule.id, rule.cooldownMs)) continue
                    evaluateAndExecute(rule, snap)
                }
            }
        }
    }

    private suspend fun evaluateAndExecute(rule: StrategyRule, snap: StockSnapshotLite) {
        when (rule) {
            is StrategyRule.AutoBuyDip -> handleAutoBuyDip(rule, snap)
            is StrategyRule.TakeProfit -> handleTakeProfit(rule, snap)
            is StrategyRule.StopLoss -> handleStopLoss(rule, snap)
        }
    }

    private suspend fun handleAutoBuyDip(rule: StrategyRule.AutoBuyDip, snap: StockSnapshotLite) {
        val refPrice = when (rule.reference) {
            DipReference.OPEN -> snap.openPrice
            DipReference.HIGH -> snap.highPrice
            DipReference.LAST_N_AVG -> snap.priceHistory.takeLast(10).averageOrNull() ?: snap.openPrice
        }

        // Precio ha caído X% respecto a referencia
        val threshold = refPrice * (1.0 - rule.dropPercent / 100.0)
        if (snap.currentPrice > threshold) return

        // compra por presupuesto
        val qty = (rule.budgetEuro / snap.currentPrice).toInt().coerceAtLeast(0)
        if (qty <= 0) return

        val ok = portfolio.buy(ticker = snap.ticker, qty = qty)
        if (!ok) return

        val trigger = StrategyTrigger(
            strategyId = rule.id,
            ticker = snap.ticker,
            action = StrategyAction.BUY,
            reason = "AutoBuyDip: caída >= ${rule.dropPercent}%, ref=${"%.2f".format(refPrice)}",
            price = snap.currentPrice
        )
        emitTrigger(trigger)
    }

    private suspend fun handleTakeProfit(rule: StrategyRule.TakeProfit, snap: StockSnapshotLite) {
        val pos = portfolio.getPosition(snap.ticker) ?: return
        if (pos.qty <= 0) return

        val gainPercent = ((snap.currentPrice - pos.avgBuyPrice) / pos.avgBuyPrice) * 100.0
        if (gainPercent < rule.profitPercent) return

        val sellQty = (pos.qty * rule.sellFraction).toInt().coerceIn(1, pos.qty)
        val ok = portfolio.sell(ticker = snap.ticker, qty = sellQty)
        if (!ok) return

        val trigger = StrategyTrigger(
            strategyId = rule.id,
            ticker = snap.ticker,
            action = StrategyAction.SELL,
            reason = "TakeProfit: beneficio ${"%.2f".format(gainPercent)}% >= ${rule.profitPercent}%",
            price = snap.currentPrice
        )
        emitTrigger(trigger)
    }

    private suspend fun handleStopLoss(rule: StrategyRule.StopLoss, snap: StockSnapshotLite) {
        val pos = portfolio.getPosition(snap.ticker) ?: return
        if (pos.qty <= 0) return

        val lossPercent = ((pos.avgBuyPrice - snap.currentPrice) / pos.avgBuyPrice) * 100.0
        if (lossPercent < rule.lossPercent) return

        val sellQty = (pos.qty * rule.sellFraction).toInt().coerceIn(1, pos.qty)
        val ok = portfolio.sell(ticker = snap.ticker, qty = sellQty)
        if (!ok) return

        val trigger = StrategyTrigger(
            strategyId = rule.id,
            ticker = snap.ticker,
            action = StrategyAction.SELL,
            reason = "StopLoss: pérdida ${"%.2f".format(lossPercent)}% >= ${rule.lossPercent}%",
            price = snap.currentPrice
        )
        emitTrigger(trigger)
    }

    private fun emitTrigger(trigger: StrategyTrigger) {
        // Flow realtime
        _triggers.tryEmit(trigger)

        // Debug (Desktop/Android logcat)
        println(
            "[AUTO] ${trigger.action} ${trigger.ticker} -> ${trigger.reason} @ ${"%.2f".format(trigger.price)}"
        )

        // Historial (máx 200)
        scope.launch {
            triggersMutex.withLock {
                _triggerHistory.value = (_triggerHistory.value + trigger).takeLast(200)
            }
        }
    }

    private suspend fun canFire(ruleId: Int, cooldownMs: Long): Boolean {
        val now = System.currentTimeMillis()
        return lastFiredMutex.withLock {
            val last = lastFiredByRule[ruleId] ?: 0L
            if (now - last < cooldownMs) return@withLock false
            lastFiredByRule[ruleId] = now
            true
        }
    }

    /** Opcional: por si quieres un botón "Limpiar historial" en UI */
    fun clearHistory() {
        scope.launch {
            triggersMutex.withLock { _triggerHistory.value = emptyList() }
        }
    }

    override fun close() {
        job?.cancel()
        job = null
        scope.cancel()
    }
}

/** Helpers */
private fun List<Double>.averageOrNull(): Double? =
    if (isEmpty()) null else sum() / size

/**
 * Pequeños modelos "lite" para no acoplar StrategyEngine a tus clases reales.
 * Luego los mapeas desde tu StockSnapshot real.
 */
data class StockSnapshotLite(
    val ticker: String,
    val currentPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val priceHistory: List<Double>
)

data class PositionLite(
    val qty: Int,
    val avgBuyPrice: Double
)

/**
 * Puentes para adaptar el engine sin conocer tus repos exactos.
 */
interface StrategyMarketBridge {
    val priceUpdates: Flow<String>
    suspend fun getSnapshot(ticker: String): StockSnapshotLite?
    fun isMarketOpen(): Boolean
}

interface StrategyPortfolioBridge {
    suspend fun buy(ticker: String, qty: Int): Boolean
    suspend fun sell(ticker: String, qty: Int): Boolean
    suspend fun getPosition(ticker: String): PositionLite?
}
