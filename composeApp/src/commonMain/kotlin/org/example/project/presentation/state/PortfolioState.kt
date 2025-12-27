package org.example.project.presentation.state

import org.example.project.domain.model.Holding
import org.example.project.domain.model.PositionSnapshot
import org.example.project.domain.model.Transaction

/**
 * Estado “live” del portfolio para la UI.
 *
 * Fuente de verdad:
 * - Este estado SOLO lo emite el repositorio.
 * - La UI nunca recalcula valores financieros.
 *
 * Importante:
 * - holdings: base para operar (qty + avgBuyPrice)
 * - positions: datos enriquecidos para UI (precio actual + PnL)
 * - portfolioValue / pnl*: ya calculados en el repo
 *
 * Nota:
 * - Estado de presentación, NO persistente.
 */
data class PortfolioState(

    // =========================
    // BASE
    // =========================

    /** Efectivo disponible */
    val cash: Double = 10_000.0,

    /** Holdings base (fuente para operar) */
    val holdings: List<Holding> = emptyList(),

    /** Posiciones enriquecidas para UI */
    val positions: List<PositionSnapshot> = emptyList(),

    /** Historial completo de transacciones */
    val transactions: List<Transaction> = emptyList(),

    // =========================
    // AGREGADOS (calculados en repo)
    // =========================

    /** Valor total del portfolio: cash + holdingsValue */
    val portfolioValue: Double = cash,

    /** Beneficio/pérdida TOTAL de holdings (no incluye cash) */
    val pnlEuro: Double = 0.0,

    /** Beneficio/pérdida TOTAL en porcentaje */
    val pnlPercent: Double = 0.0,

    /** Datos para gráfico de barras (top movers) */
    val profitBars: List<ProfitBarPoint> = emptyList()

) {

    // =========================
    // FLAGS DE UI (DERIVADOS)
    // =========================

    /** ¿Hay alguna posición abierta? */
    val hasPositions: Boolean
        get() = positions.isNotEmpty()

    /** ¿Se ha realizado alguna transacción? */
    val hasTransactions: Boolean
        get() = transactions.isNotEmpty()

    /** ¿El portfolio está completamente vacío? */
    val isEmpty: Boolean
        get() = cash <= 1e-6 && positions.isEmpty()

    /** ¿El PnL es positivo? */
    val isProfit: Boolean
        get() = pnlEuro > 1e-6

    /** ¿El PnL es negativo? */
    val isLoss: Boolean
        get() = pnlEuro < -1e-6

    // =========================
    // VALIDACIONES DE SEGURIDAD
    // =========================

    init {
        require(cash.isFinite()) {
            "PortfolioState.cash debe ser finito (actual: $cash)"
        }
        require(portfolioValue.isFinite()) {
            "PortfolioState.portfolioValue debe ser finito (actual: $portfolioValue)"
        }
        require(pnlEuro.isFinite()) {
            "PortfolioState.pnlEuro debe ser finito (actual: $pnlEuro)"
        }
        require(pnlPercent.isFinite()) {
            "PortfolioState.pnlPercent debe ser finito (actual: $pnlPercent)"
        }

        // El repo ya evita cash negativo, pero protegemos la UI
        require(cash >= -1e-6) {
            "PortfolioState.cash no debería ser negativo: $cash"
        }
    }
}
