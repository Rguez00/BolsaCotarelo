package org.example.project.domain.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Posición “raw” persistible del usuario para un ticker.
 *
 * ✅ Nota importante para la persistencia:
 * - Esta clase es segura para serializar a JSON (solo primitives).
 * - avgBuyPrice representa el coste medio efectivo (incluyendo comisiones) por acción.
 */
data class Holding(
    val ticker: String,
    val quantity: Int,
    val avgBuyPrice: Double // precio medio ponderado de compra (coste efectivo, incluye comisiones)
) {
    init {
        val t = ticker.trim()
        require(t.isNotEmpty()) { "Holding.ticker no puede estar vacío" }
        require(quantity > 0) { "Holding.quantity debe ser > 0" }
        require(avgBuyPrice.isFinite() && avgBuyPrice >= 0.0) { "Holding.avgBuyPrice inválido: $avgBuyPrice" }
    }

    /**
     * Devuelve el ticker normalizado (trim + uppercase).
     * Útil para asegurar consistencia al persistir/cargar y al mapear holdings.
     */
    fun normalizedTicker(): String = ticker.trim().uppercase()

    /**
     * Devuelve una copia con ticker normalizado.
     * - No toca quantity/avgBuyPrice.
     */
    fun normalize(): Holding = copy(ticker = normalizedTicker())

    /**
     * Basis (invertido) = avgBuyPrice * quantity.
     * KMP-safe y útil para cálculos/validaciones.
     */
    fun invested(): Double = avgBuyPrice * quantity.toDouble()

    /**
     * Comparación robusta para tests/validaciones de migraciones (JSON).
     */
    fun almostEquals(
        other: Holding,
        absEps: Double = 1e-6,
        relEps: Double = 1e-9
    ): Boolean {
        if (normalizedTicker() != other.normalizedTicker()) return false
        if (quantity != other.quantity) return false

        val a = avgBuyPrice
        val b = other.avgBuyPrice
        val diff = abs(a - b)
        val scale = max(1.0, max(abs(a), abs(b)))
        return diff <= max(absEps, relEps * scale)
    }
}
