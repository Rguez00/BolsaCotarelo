package org.example.project.presentation.state

import org.example.project.domain.model.Holding
import org.example.project.domain.model.PositionSnapshot
import org.example.project.domain.model.Transaction

/**
 * Estado “live” del portfolio para la UI.
 *
 * Importante:
 * - holdings: base para operar (qty + avgBuyPrice)
 * - positions: enriquecido para UI (precio actual + PnL por ticker)
 * - portfolioValue / pnl*: calculados en repo (fuente de verdad)
 *
 * Nota:
 * - Este estado es “de presentación”, no de persistencia.
 */
data class PortfolioState(
    val cash: Double = 10_000.0,

    val holdings: List<Holding> = emptyList(),
    val positions: List<PositionSnapshot> = emptyList(),

    val transactions: List<Transaction> = emptyList(),

    // cash + holdingsValue (lo calcula el repo)
    val portfolioValue: Double = cash,

    // PnL global SOLO holdings: holdingsValue - totalInvested (lo calcula el repo)
    val pnlEuro: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val profitBars: List<ProfitBarPoint> = emptyList()

) {
    init {
        require(cash.isFinite()) { "PortfolioState.cash debe ser finito" }
        require(portfolioValue.isFinite()) { "PortfolioState.portfolioValue debe ser finito" }
        require(pnlEuro.isFinite()) { "PortfolioState.pnlEuro debe ser finito" }
        require(pnlPercent.isFinite()) { "PortfolioState.pnlPercent debe ser finito" }

        // cash puede ser 0, pero no debería ser negativo (el repo ya lo evita con EPS).
        require(cash >= -1e-6) { "PortfolioState.cash no debería ser negativo: $cash" }
    }
}
