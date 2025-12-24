package org.example.project.domain.model

import kotlin.math.abs
import kotlin.math.max

data class PortfolioSnapshot(
    val cash: Double,

    // Holdings base (para persistencia/operaciones)
    val holdings: List<Holding>,

    // Posiciones enriquecidas para UI (precio actual, pnl por ticker, etc.)
    val positions: List<PositionSnapshot>,

    // Total invertido (suma de invested de todas las posiciones)
    val totalInvested: Double,

    // Valor actual de todas las posiciones (suma de valueNow)
    val holdingsValue: Double,

    // Valor total (cash + holdingsValue)
    val portfolioValue: Double,

    // PnL global SOLO holdings (holdingsValue - totalInvested)
    val pnlEuro: Double,

    // PnL% global SOLO holdings
    val pnlPercent: Double
) {
    init {
        require(cash.isFinite() && cash >= 0.0) { "PortfolioSnapshot.cash inválido: $cash" }
        require(totalInvested.isFinite() && totalInvested >= 0.0) { "PortfolioSnapshot.totalInvested inválido: $totalInvested" }
        require(holdingsValue.isFinite() && holdingsValue >= 0.0) { "PortfolioSnapshot.holdingsValue inválido: $holdingsValue" }

        require(portfolioValue.isFinite() && portfolioValue >= 0.0) { "PortfolioSnapshot.portfolioValue inválido: $portfolioValue" }
        require(pnlEuro.isFinite()) { "PortfolioSnapshot.pnlEuro debe ser finito" }
        require(pnlPercent.isFinite()) { "PortfolioSnapshot.pnlPercent debe ser finito" }

        // Coherencia matemática (tolerancia un poco mayor por sumas)
        val expectedPortfolioValue = cash + holdingsValue
        require(almostEquals(portfolioValue, expectedPortfolioValue, absEps = 1e-4)) {
            "PortfolioSnapshot.portfolioValue no cuadra (esperado=$expectedPortfolioValue, recibido=$portfolioValue)"
        }

        val expectedPnlEuro = holdingsValue - totalInvested
        require(almostEquals(pnlEuro, expectedPnlEuro, absEps = 1e-4)) {
            "PortfolioSnapshot.pnlEuro no cuadra (esperado=$expectedPnlEuro, recibido=$pnlEuro)"
        }

        val expectedPnlPercent =
            if (totalInvested > 0.0) (pnlEuro / totalInvested) * 100.0 else 0.0
        require(almostEquals(pnlPercent, expectedPnlPercent, absEps = 1e-4)) {
            "PortfolioSnapshot.pnlPercent no cuadra (esperado=$expectedPnlPercent, recibido=$pnlPercent)"
        }
    }

    private fun almostEquals(a: Double, b: Double, absEps: Double = 1e-6, relEps: Double = 1e-9): Boolean {
        val diff = abs(a - b)
        val scale = max(1.0, max(abs(a), abs(b)))
        return diff <= max(absEps, relEps * scale)
    }
}
