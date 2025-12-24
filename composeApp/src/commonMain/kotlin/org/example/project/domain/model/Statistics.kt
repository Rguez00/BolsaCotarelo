package org.example.project.domain.model

data class TransactionSummary(
    val ticker: String,
    val quantity: Int,
    val pricePerShare: Double,
    val netTotal: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    val timestamp: String
)

data class StockProfitability(
    val ticker: String,
    val quantity: Int,
    val invested: Double,
    val currentValue: Double,
    val pnlEuro: Double,
    val pnlPercent: Double
)

data class PortfolioStatistics(
    val bestTransaction: TransactionSummary?,
    val worstTransaction: TransactionSummary?,
    val mostProfitableStock: StockProfitability?,
    val successRate: Double,
    val profitableSells: Int,
    val totalSells: Int,
    val averageProfitability: Double,
    val totalPositions: Int
)

/**
 * Estadísticas:
 * - Reconstruye coste medio por ticker con BUY (netTotal incluye comisión)
 * - Calcula beneficio por SELL: sell.netTotal - coste_removido
 */
fun calculateStatistics(
    transactions: List<Transaction>,
    positions: List<PositionSnapshot>
): PortfolioStatistics {

    data class CostState(var qty: Int = 0, var cost: Double = 0.0)

    val states = mutableMapOf<String, CostState>()
    val sells = mutableListOf<TransactionSummary>()

    val orderedTx = transactions.sortedBy { it.timestamp }

    for (tx in orderedTx) {
        val st = states.getOrPut(tx.ticker) { CostState() }

        when (tx.type) {
            TransactionType.BUY -> {
                st.qty += tx.quantity
                st.cost += tx.netTotal // incluye comisión
            }

            TransactionType.SELL -> {
                val sellQty = tx.quantity
                val avgCost = if (st.qty > 0) st.cost / st.qty.toDouble() else 0.0
                val removedCost = avgCost * sellQty.toDouble()

                st.qty = (st.qty - sellQty).coerceAtLeast(0)
                st.cost = (st.cost - removedCost).coerceAtLeast(0.0)

                val profit = tx.netTotal - removedCost
                val profitPct = if (removedCost > 1e-9) (profit / removedCost) * 100.0 else 0.0

                sells.add(
                    TransactionSummary(
                        ticker = tx.ticker,
                        quantity = tx.quantity,
                        pricePerShare = tx.pricePerShare,
                        netTotal = tx.netTotal,
                        profitLoss = profit,
                        profitLossPercent = profitPct,
                        timestamp = tx.timestamp.toString().take(19)
                    )
                )
            }
        }
    }

    val best = sells.maxByOrNull { it.profitLoss }
    val worst = sells.minByOrNull { it.profitLoss }

    val totalSells = sells.size
    val profitableSells = sells.count { it.profitLoss > 0.0001 }
    val successRate = if (totalSells > 0) profitableSells.toDouble() / totalSells.toDouble() * 100.0 else 0.0

    val mostProfitableStock = positions
        .map {
            StockProfitability(
                ticker = it.ticker,
                quantity = it.quantity,
                invested = it.invested,
                currentValue = it.valueNow,
                pnlEuro = it.pnlEuro,
                pnlPercent = it.pnlPercent
            )
        }
        .maxByOrNull { it.pnlPercent }

    val totalInvested = positions.sumOf { it.invested }
    val totalPnl = positions.sumOf { it.pnlEuro }
    val avgProfitability = if (totalInvested > 1e-9) (totalPnl / totalInvested) * 100.0 else 0.0

    return PortfolioStatistics(
        bestTransaction = best,
        worstTransaction = worst,
        mostProfitableStock = mostProfitableStock,
        successRate = successRate,
        profitableSells = profitableSells,
        totalSells = totalSells,
        averageProfitability = avgProfitability,
        totalPositions = positions.size
    )
}
