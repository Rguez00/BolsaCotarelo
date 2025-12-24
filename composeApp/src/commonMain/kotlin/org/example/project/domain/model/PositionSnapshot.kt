package org.example.project.domain.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Posición "enriquecida" para UI:
 * combina lo que tengo (Holding) con datos de mercado (precio actual),
 * y calcula valor y PnL en tiempo real.
 */
data class PositionSnapshot(
    val ticker: String,
    val quantity: Int,

    val avgBuyPrice: Double,
    val currentPrice: Double,

    val invested: Double,     // avgBuyPrice * quantity
    val valueNow: Double,     // currentPrice * quantity

    val pnlEuro: Double,      // valueNow - invested
    val pnlPercent: Double    // (pnlEuro / invested) * 100 (si invested > 0)
) {
    init {
        require(ticker.trim().isNotEmpty()) { "PositionSnapshot.ticker no puede estar vacío" }
        require(quantity > 0) { "PositionSnapshot.quantity debe ser > 0" }

        require(avgBuyPrice.isFinite() && avgBuyPrice >= 0.0) { "PositionSnapshot.avgBuyPrice inválido: $avgBuyPrice" }
        require(currentPrice.isFinite() && currentPrice >= 0.0) { "PositionSnapshot.currentPrice inválido: $currentPrice" }

        require(invested.isFinite() && invested >= 0.0) { "PositionSnapshot.invested inválido: $invested" }
        require(valueNow.isFinite() && valueNow >= 0.0) { "PositionSnapshot.valueNow inválido: $valueNow" }

        require(pnlEuro.isFinite()) { "PositionSnapshot.pnlEuro debe ser finito" }
        require(pnlPercent.isFinite()) { "PositionSnapshot.pnlPercent debe ser finito" }

        // Coherencia matemática
        val expectedInvested = avgBuyPrice * quantity
        require(almostEquals(invested, expectedInvested)) {
            "PositionSnapshot.invested no cuadra (esperado=$expectedInvested, recibido=$invested)"
        }

        val expectedValueNow = currentPrice * quantity
        require(almostEquals(valueNow, expectedValueNow)) {
            "PositionSnapshot.valueNow no cuadra (esperado=$expectedValueNow, recibido=$valueNow)"
        }

        val expectedPnlEuro = valueNow - invested
        require(almostEquals(pnlEuro, expectedPnlEuro)) {
            "PositionSnapshot.pnlEuro no cuadra (esperado=$expectedPnlEuro, recibido=$pnlEuro)"
        }

        val expectedPnlPercent =
            if (invested > 0.0) (pnlEuro / invested) * 100.0 else 0.0
        require(almostEquals(pnlPercent, expectedPnlPercent, absEps = 1e-5)) {
            "PositionSnapshot.pnlPercent no cuadra (esperado=$expectedPnlPercent, recibido=$pnlPercent)"
        }
    }

    private fun almostEquals(a: Double, b: Double, absEps: Double = 1e-6, relEps: Double = 1e-9): Boolean {
        val diff = abs(a - b)
        val scale = max(1.0, max(abs(a), abs(b)))
        return diff <= max(absEps, relEps * scale)
    }
}
