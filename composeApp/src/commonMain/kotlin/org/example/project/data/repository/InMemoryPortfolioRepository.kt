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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.data.repository.PortfolioError.*
import org.example.project.domain.model.Holding
import org.example.project.domain.model.PortfolioSnapshot
import org.example.project.domain.model.PositionSnapshot
import org.example.project.domain.model.Sector
import org.example.project.domain.model.Transaction
import org.example.project.domain.model.TransactionType
import org.example.project.presentation.state.PortfolioState
import org.example.project.presentation.state.ProfitBarPoint
import kotlin.math.abs
import kotlin.math.roundToLong

class InMemoryPortfolioRepository(
    private val marketRepo: MarketRepository,
    private val initialCash: Double = 10_000.0, // ✅ ahora propiedad
    private val externalScope: CoroutineScope? = null
) : PortfolioRepository {

    companion object {
        private const val COMMISSION_RATE = 0.005 // 0.5%
        private const val EPS = 1e-9
    }

    // JSON config: tolerante a cambios futuros
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
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
            profitBars = emptyList()
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

        val owned = mutex.withLock { holdingsMap[t]?.quantity ?: 0 }
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

            // caja: siempre neto
            cash -= net
            if (cash in -EPS..0.0) cash = 0.0

            // coste medio: siempre neto (incluye comisión)
            val current = holdingsMap[t]
            val newHolding = if (current == null) {
                val effectivePrice = net / quantity.toDouble()
                Holding(ticker = t, quantity = quantity, avgBuyPrice = effectivePrice)
            } else {
                val oldQty = current.quantity
                val newQty = oldQty + quantity
                val oldBasis = current.avgBuyPrice * oldQty.toDouble()
                val newBasis = oldBasis + net
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

    override suspend fun sell(ticker: String, quantity: Int): Result<Transaction> {
        val t = normalizeTicker(ticker)
        if (quantity <= 0) return Result.failure(InvalidQuantity(quantity))

        return mutex.withLock {
            if (!isMarketTradable()) return@withLock Result.failure(MarketClosedOrPaused)

            val current = holdingsMap[t]
            val owned = current?.quantity ?: 0
            if (owned < quantity) return@withLock Result.failure(InsufficientHoldings(t, quantity, owned))

            val snap = marketRepo.getSnapshot(t) ?: return@withLock Result.failure(UnknownTicker(t))
            val price = snap.currentPrice

            val gross = price * quantity
            val commission = gross * COMMISSION_RATE
            val net = gross - commission

            val remaining = owned - quantity
            if (remaining == 0) holdingsMap.remove(t) else holdingsMap[t] = current!!.copy(quantity = remaining)

            cash += net
            if (cash in -EPS..0.0) cash = 0.0

            val tx = Transaction(
                id = nextTxId++,
                timestamp = System.currentTimeMillis(),
                type = TransactionType.SELL,
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

    // ============================================================
    // ✅ PortfolioRepository: PERSISTENCIA JSON (contrato)
    // ============================================================

    override suspend fun exportStateJson(): Result<String> =
        runCatching { toPersistedJsonV1() }

    override suspend fun importStateJson(json: String): Result<Unit> =
        restoreFromPersistedJsonV1(json)

    override suspend fun clearState(): Result<Unit> =
        runCatching {
            mutex.withLock {
                cash = initialCash
                holdingsMap.clear()
                transactions.clear()
                nextTxId = 1
                emitPortfolioStateLocked()
            }
        }

    // ============================================================
    // ✅ PERSISTENCIA (DTO V1)
    // ============================================================

    @Serializable
    @SerialName("portfolio_v1")
    data class PersistedPortfolioV1(
        val cash: Double,
        val nextTxId: Int,
        val holdings: List<PersistedHoldingV1>,
        val transactions: List<PersistedTransactionV1>
    )

    @Serializable
    data class PersistedHoldingV1(
        val ticker: String,
        val quantity: Int,
        val avgBuyPrice: Double
    )

    @Serializable
    data class PersistedTransactionV1(
        val id: Int,
        val timestamp: Long,
        val type: String,
        val ticker: String,
        val companyName: String? = null,
        val sector: String? = null,
        val quantity: Int,
        val pricePerShare: Double,
        val grossTotal: Double,
        val commission: Double,
        val netTotal: Double
    )

    suspend fun exportPersistedStateV1(): PersistedPortfolioV1 =
        mutex.withLock {
            PersistedPortfolioV1(
                cash = cash.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
                nextTxId = nextTxId.coerceAtLeast(1),
                holdings = holdingsMap.values.map { h ->
                    PersistedHoldingV1(
                        ticker = normalizeTicker(h.ticker),
                        quantity = h.quantity.coerceAtLeast(0),
                        avgBuyPrice = h.avgBuyPrice
                    )
                },
                transactions = transactions.map { tx ->
                    PersistedTransactionV1(
                        id = tx.id,
                        timestamp = tx.timestamp,
                        type = tx.type.name,
                        ticker = normalizeTicker(tx.ticker),
                        companyName = tx.companyName,
                        sector = tx.sector?.name,
                        quantity = tx.quantity,
                        pricePerShare = tx.pricePerShare,
                        grossTotal = tx.grossTotal,
                        commission = tx.commission,
                        netTotal = tx.netTotal
                    )
                }
            )
        }

    /** JSON (string) listo para guardar a fichero en Android/Desktop */
    suspend fun toPersistedJsonV1(): String {
        val state = exportPersistedStateV1()
        return json.encodeToString(PersistedPortfolioV1.serializer(), state)
    }

    /** Restaura desde JSON (string) cargado de fichero en Android/Desktop */
    suspend fun restoreFromPersistedJsonV1(rawJson: String): Result<Unit> {
        return runCatching {
            val state = json.decodeFromString(PersistedPortfolioV1.serializer(), rawJson)
            importPersistedStateV1(state)
        }
    }

    suspend fun importPersistedStateV1(state: PersistedPortfolioV1) {
        mutex.withLock {
            holdingsMap.clear()
            transactions.clear()

            cash = state.cash.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0

            // holdings
            state.holdings.forEach { h ->
                val t = normalizeTicker(h.ticker)
                val qty = h.quantity
                val avg = h.avgBuyPrice
                if (t.isBlank()) return@forEach
                if (qty <= 0) return@forEach
                if (!avg.isFinite() || avg < 0.0) return@forEach
                holdingsMap[t] = Holding(ticker = t, quantity = qty, avgBuyPrice = avg)
            }

            // transactions
            val importedTx = state.transactions
                .mapNotNull { p ->
                    val t = normalizeTicker(p.ticker)
                    if (t.isBlank()) return@mapNotNull null
                    if (p.id <= 0) return@mapNotNull null
                    if (p.timestamp <= 0L) return@mapNotNull null
                    if (p.quantity <= 0) return@mapNotNull null

                    val type = runCatching { TransactionType.valueOf(p.type) }.getOrNull() ?: return@mapNotNull null

                    if (!p.pricePerShare.isFinite() || p.pricePerShare < 0.0) return@mapNotNull null
                    if (!p.grossTotal.isFinite() || p.grossTotal < 0.0) return@mapNotNull null
                    if (!p.commission.isFinite() || p.commission < 0.0) return@mapNotNull null
                    if (!p.netTotal.isFinite() || p.netTotal < 0.0) return@mapNotNull null

                    val sectorEnum = p.sector?.let { runCatching { Sector.valueOf(it) }.getOrNull() }

                    Transaction(
                        id = p.id,
                        timestamp = p.timestamp,
                        type = type,
                        ticker = t,
                        companyName = p.companyName,
                        sector = sectorEnum,
                        quantity = p.quantity,
                        pricePerShare = p.pricePerShare,
                        grossTotal = p.grossTotal,
                        commission = p.commission,
                        netTotal = p.netTotal
                    )
                }
                .distinctBy { it.id }
                .sortedBy { it.timestamp }

            transactions.addAll(importedTx)

            val maxId = importedTx.maxOfOrNull { it.id } ?: 0
            val candidate = maxOf(state.nextTxId, maxId + 1)
            nextTxId = candidate.coerceAtLeast(1)

            emitPortfolioStateLocked()
        }
    }

    // ============================================================

    private fun emitPortfolioStateLocked() {
        val snap = buildSnapshotLocked()

        val bars = snap.positions
            .map { p -> ProfitBarPoint(p.ticker, p.pnlEuro) }
            .sortedByDescending { abs(it.valueEuro) }
            .take(12)

        _portfolioState.value = PortfolioState(
            cash = snap.cash,
            holdings = snap.holdings,
            positions = snap.positions,
            transactions = transactions.toList(),
            portfolioValue = snap.portfolioValue,
            pnlEuro = snap.pnlEuro,
            pnlPercent = snap.pnlPercent,
            profitBars = bars
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
