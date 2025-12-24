package org.example.project.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.repository.PortfolioError.*
import org.example.project.domain.model.Holding
import org.example.project.domain.model.PortfolioSnapshot
import org.example.project.domain.model.PositionSnapshot
import org.example.project.domain.model.Transaction
import org.example.project.domain.model.TransactionType
import org.example.project.presentation.state.PortfolioState
import kotlin.math.abs
import kotlin.math.roundToLong

class InMemoryPortfolioRepository(
    private val marketRepo: MarketRepository,
    initialCash: Double = 10_000.0,
    private val externalScope: CoroutineScope? = null
) : PortfolioRepository {

    companion object {
        private const val COMMISSION_RATE = 0.005 // 0.5%
        private const val EPS = 1e-9
    }

    private val job: Job? = if (externalScope == null) SupervisorJob() else null
    private val scope: CoroutineScope =
        externalScope ?: CoroutineScope(Dispatchers.Main.immediate + job!!)
    private val mutex = Mutex()

    private var cash: Double = initialCash
    private val holdingsMap: MutableMap<String, Holding> = linkedMapOf()
    private val transactions: MutableList<Transaction> = mutableListOf()
    private var nextTxId: Int = 1

    private val _portfolioState = MutableStateFlow(
        PortfolioState(
            cash = cash,
            holdings = emptyList(),
            positions = emptyList(),
            transactions = emptyList(),
            portfolioValue = cash,
            pnlEuro = 0.0,
            pnlPercent = 0.0,
            profitBars = emptyList() // 👈 NUEVO
        )
    )
    override val portfolioState: StateFlow<PortfolioState> = _portfolioState

    private var pricesJob: Job? = null

    init {
        pricesJob = scope.launch {
            marketRepo.priceUpdates.collect { update ->
                val t = normalizeTicker(update.ticker)
                mutex.withLock {
                    if (!holdingsMap.containsKey(t)) return@withLock
                    emitPortfolioStateLocked()
                }
            }
        }
    }

    // ✅ Guardia repo-level
    private fun isMarketTradable(): Boolean {
        val st = marketRepo.marketState.value
        return st.isOpen && !st.isPaused
    }

    override suspend fun previewBuy(ticker: String, quantity: Int): Result<TransactionPreview> {
        if (!isMarketTradable()) return Result.failure(MarketClosedOrPaused)

        val t = normalizeTicker(ticker)
        if (quantity <= 0) return Result.failure(InvalidQuantity(quantity))

        val snap = marketRepo.getSnapshot(t) ?: return Result.failure(UnknownTicker(t))
        val price = snap.currentPrice

        val gross = price * quantity
        val commission = gross * COMMISSION_RATE
        val net = gross + commission

        val available = mutex.withLock { cash }
        if (available + EPS < net) {
            return Result.failure(InsufficientCash(required = net, available = available))
        }

        return Result.success(
            TransactionPreview(
                ticker = t,
                quantity = quantity,
                pricePerShare = price,
                grossTotal = gross,
                commission = commission,
                netTotal = net
            )
        )
    }

    override suspend fun previewSell(ticker: String, quantity: Int): Result<TransactionPreview> {
        if (!isMarketTradable()) return Result.failure(MarketClosedOrPaused)

        val t = normalizeTicker(ticker)
        if (quantity <= 0) return Result.failure(InvalidQuantity(quantity))

        val owned = mutex.withLock {
            // 1) Fuente principal: holdingsMap (ticker normalizado)
            holdingsMap[t]?.quantity
            // 2) Fallback: snapshot (por si hay cualquier desajuste temporal)
                ?: buildSnapshotLocked()
                    .positions
                    .firstOrNull { it.ticker == t }
                    ?.quantity
                ?: 0
        }

        // ✅ Permitimos vender EXACTAMENTE lo que tienes (owned == quantity es válido)
        if (owned < quantity) return Result.failure(InsufficientHoldings(t, quantity, owned))

        val snap = marketRepo.getSnapshot(t) ?: return Result.failure(UnknownTicker(t))
        val price = snap.currentPrice

        val gross = price * quantity
        val commission = gross * COMMISSION_RATE
        val net = gross - commission

        return Result.success(
            TransactionPreview(
                ticker = t,
                quantity = quantity,
                pricePerShare = price,
                grossTotal = gross,
                commission = commission,
                netTotal = net
            )
        )
    }


    override suspend fun buy(ticker: String, quantity: Int): Result<Transaction> {
        val t = normalizeTicker(ticker)
        if (quantity <= 0) return Result.failure(InvalidQuantity(quantity))

        return mutex.withLock {
            if (!isMarketTradable()) return@withLock Result.failure(MarketClosedOrPaused)

            val snap = marketRepo.getSnapshot(t) ?: return@withLock Result.failure(UnknownTicker(t))
            val price = snap.currentPrice

            val gross = price * quantity
            val commission = gross * COMMISSION_RATE
            val net = gross + commission

            if (cash + EPS < net) {
                return@withLock Result.failure(InsufficientCash(required = net, available = cash))
            }

            // 1) Caja: siempre neto
            cash -= net
            if (cash in -EPS..0.0) cash = 0.0

            // 2) Coste medio: SIEMPRE neto (incluye comisión)
            val current = holdingsMap[t]
            val newHolding = if (current == null) {
                val effectivePrice = net / quantity.toDouble()
                Holding(ticker = t, quantity = quantity, avgBuyPrice = effectivePrice)
            } else {
                val oldQty = current.quantity
                val newQty = oldQty + quantity

                val oldBasis = current.avgBuyPrice * oldQty.toDouble() // ya incluye comisiones previas
                val newBasis = oldBasis + net                          // añadimos coste neto de esta compra
                val newAvg = newBasis / newQty.toDouble()

                current.copy(quantity = newQty, avgBuyPrice = newAvg)
            }
            holdingsMap[t] = newHolding

            val tx = Transaction(
                id = nextTxId++,
                timestamp = System.currentTimeMillis(),
                type = TransactionType.BUY,
                ticker = t,
                companyName = snap.name,
                sector = snap.sector,
                quantity = quantity,
                pricePerShare = price,     // precio de mercado (bruto), OK para mostrar
                grossTotal = gross,
                commission = commission,
                netTotal = net             // lo que realmente sale de caja
            )
            transactions.add(tx)

            emitPortfolioStateLocked()
            Result.success(tx)
        }
    }


    override suspend fun sell(ticker: String, quantity: Int): Result<Transaction> {
        val t = normalizeTicker(ticker)
        if (quantity <= 0) return Result.failure(InvalidQuantity(quantity))

        return mutex.withLock {
            if (!isMarketTradable()) return@withLock Result.failure(MarketClosedOrPaused)

            val current = holdingsMap[t]
            val owned = current?.quantity ?: 0
            if (owned < quantity) {
                return@withLock Result.failure(InsufficientHoldings(t, quantity, owned))
            }

            val snap = marketRepo.getSnapshot(t) ?: return@withLock Result.failure(UnknownTicker(t))
            val price = snap.currentPrice

            val gross = price * quantity
            val commission = gross * COMMISSION_RATE
            val net = gross - commission

            val remaining = owned - quantity
            if (remaining == 0) holdingsMap.remove(t)
            else holdingsMap[t] = current!!.copy(quantity = remaining)

            cash += net

            val tx = Transaction(
                id = nextTxId++,
                timestamp = System.currentTimeMillis(), // ✅ SIMPLE Y FUNCIONA
                type = TransactionType.SELL, // o SELL
                ticker = t,
                companyName = snap.name,
                sector = snap.sector,
                quantity = quantity,
                pricePerShare = price,
                grossTotal = gross,
                commission = commission,
                netTotal = net
            )
            transactions.add(tx)

            emitPortfolioStateLocked()
            Result.success(tx)
        }
    }

    override suspend fun getTransactions(): List<Transaction> =
        mutex.withLock { transactions.toList() }

    override suspend fun exportTransactionsCsv(): String =
        mutex.withLock {
            buildString {
                appendLine("id,timestamp,type,ticker,quantity,pricePerShare,grossTotal,commission,netTotal")
                for (t in transactions) {
                    append(t.id).append(',')
                    append(t.timestamp).append(',')
                    append(t.type).append(',')
                    append(t.ticker).append(',')
                    append(t.quantity).append(',')
                    append(fmt6(t.pricePerShare)).append(',')
                    append(fmt6(t.grossTotal)).append(',')
                    append(fmt6(t.commission)).append(',')
                    append(fmt6(t.netTotal))
                    appendLine()
                }
            }
        }

    override suspend fun getSnapshot(): PortfolioSnapshot =
        mutex.withLock { buildSnapshotLocked() }

    private fun emitPortfolioStateLocked() {
        val snap = buildSnapshotLocked()

        // Barras: P/L (€) por acción
        // - Ordenamos por valor absoluto para que se vean las más “importantes”
        // - Limitamos a 12 para que el gráfico no quede ilegible
        val bars = snap.positions
            .map { p -> org.example.project.presentation.state.ProfitBarPoint(p.ticker, p.pnlEuro) }
            .sortedByDescending { kotlin.math.abs(it.valueEuro) }
            .take(12)

        _portfolioState.value = PortfolioState(
            cash = snap.cash,
            holdings = snap.holdings,
            positions = snap.positions,
            transactions = transactions.toList(),
            portfolioValue = snap.portfolioValue,
            pnlEuro = snap.pnlEuro,
            pnlPercent = snap.pnlPercent,
            profitBars = bars // 👈 NUEVO
        )
    }

    private fun buildSnapshotLocked(): PortfolioSnapshot {
        val holdingsList = holdingsMap.values.toList()

        val positions = holdingsList.map { h ->
            val currentPrice = marketRepo.getSnapshot(h.ticker)?.currentPrice ?: h.avgBuyPrice

            val invested = h.avgBuyPrice * h.quantity
            val valueNow = currentPrice * h.quantity
            val pnlEuro = valueNow - invested
            val pnlPercent = if (invested > 0.0) (pnlEuro / invested) * 100.0 else 0.0

            PositionSnapshot(
                ticker = h.ticker,
                quantity = h.quantity,
                avgBuyPrice = h.avgBuyPrice,
                currentPrice = currentPrice,
                invested = invested,
                valueNow = valueNow,
                pnlEuro = pnlEuro,
                pnlPercent = pnlPercent
            )
        }

        val totalInvested = positions.sumOf { it.invested }
        val holdingsValue = positions.sumOf { it.valueNow }
        val portfolioValue = cash + holdingsValue
        val pnlEuro = holdingsValue - totalInvested
        val pnlPercent = if (totalInvested > 0.0) (pnlEuro / totalInvested) * 100.0 else 0.0

        return PortfolioSnapshot(
            cash = cash,
            holdings = holdingsList,
            positions = positions,
            totalInvested = totalInvested,
            holdingsValue = holdingsValue,
            portfolioValue = portfolioValue,
            pnlEuro = pnlEuro,
            pnlPercent = pnlPercent
        )
    }

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()

    private fun fmt6(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absValue = abs(value)

        val scaled = (absValue * 1_000_000.0).roundToLong()
        val integer = scaled / 1_000_000
        val frac = (scaled % 1_000_000).toInt()

        return "$sign$integer.${frac.toString().padStart(6, '0')}"
    }

    fun close() {
        pricesJob?.cancel()
        pricesJob = null
        job?.cancel()
    }
}
