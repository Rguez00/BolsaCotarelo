package org.example.project.domain.strategy

/**
 * Regla base para estrategias automáticas.
 */
sealed class StrategyRule(
    open val id: Int,
    open val enabled: Boolean = true,
    open val ticker: String? = null,          // null => aplica a cualquier ticker
    open val cooldownMs: Long = 5_000L        // evita que dispare en bucle
) {

    data class AutoBuyDip(
        override val id: Int,
        override val enabled: Boolean = true,
        override val ticker: String? = null,
        override val cooldownMs: Long = 10_000L,
        val dropPercent: Double,              // X%
        val reference: DipReference = DipReference.OPEN,
        val budgetEuro: Double = 250.0,       // presupuesto por compra
        val minSecondsBetweenBuys: Long = 10L // extra safety
    ) : StrategyRule(id, enabled, ticker, cooldownMs)

    data class TakeProfit(
        override val id: Int,
        override val enabled: Boolean = true,
        override val ticker: String? = null,
        override val cooldownMs: Long = 10_000L,
        val profitPercent: Double,            // Y%
        val sellFraction: Double = 1.0        // 1.0 vende todo
    ) : StrategyRule(id, enabled, ticker, cooldownMs)

    data class StopLoss(
        override val id: Int,
        override val enabled: Boolean = true,
        override val ticker: String? = null,
        override val cooldownMs: Long = 10_000L,
        val lossPercent: Double,              // Z%
        val sellFraction: Double = 1.0
    ) : StrategyRule(id, enabled, ticker, cooldownMs)
}

enum class DipReference {
    OPEN,       // contra openPrice
    HIGH,       // contra highPrice
    LAST_N_AVG  // contra media últimos N
}
