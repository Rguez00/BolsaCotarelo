package org.example.project.data.repository

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Errores tipados para usar con Result.failure(...)
 *
 * Son errores de validación / operación, por eso usamos IllegalArgumentException.
 */
sealed class PortfolioError(message: String) : IllegalArgumentException(message) {

    data class InvalidQuantity(val quantity: Int) :
        PortfolioError("Cantidad inválida ($quantity). Debe ser mayor que 0.")

    data class UnknownTicker(val ticker: String) :
        PortfolioError("Ticker desconocido: $ticker")

    data class InsufficientCash(val required: Double, val available: Double) :
        PortfolioError("Saldo insuficiente. Necesitas ${fmtEuro(required)} y tienes ${fmtEuro(available)}.")

    data class InsufficientHoldings(val ticker: String, val requested: Int, val owned: Int) :
        PortfolioError("No tienes suficientes acciones de $ticker. Quieres vender $requested y tienes $owned.")

    // ✅ NUEVO (lo estabas usando y faltaba)
    object MarketClosedOrPaused :
        PortfolioError("Mercado no disponible (cerrado o pausado).")

    companion object {
        /**
         * Formateo KMP-safe (sin Locale/String.format).
         * Siempre 2 decimales y '.' como separador.
         */
        private fun fmtEuro(value: Double): String {
            if (!value.isFinite()) return "NaN €"

            val sign = if (value < 0) "-" else ""
            val absValue = abs(value)

            val centsTotal = (absValue * 100.0).roundToLong()
            val euros = centsTotal / 100
            val cents = (centsTotal % 100).toInt()

            return "$sign$euros.${cents.toString().padStart(2, '0')} €"
        }
    }
}
