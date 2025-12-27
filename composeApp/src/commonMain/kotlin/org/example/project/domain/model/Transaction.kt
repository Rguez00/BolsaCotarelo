package org.example.project.domain.model

import kotlin.math.abs
import kotlin.math.max

/**
 * Transacción persistible (JSON-friendly):
 * - timestamp como Long epochMillis (KMP-safe)
 * - companyName / sector opcionales para no depender del market al exportar/estadísticas
 */
data class Transaction(
    val id: Int,

    /** Epoch millis (System.currentTimeMillis()) */
    val timestamp: Long,

    val type: TransactionType,

    /** Identificador (ticker) */
    val ticker: String,

    /** Metadatos opcionales (útiles para CSV/estadísticas sin depender del market) */
    val companyName: String? = null,
    val sector: Sector? = null,

    val quantity: Int,

    /** Precio de mercado por acción (bruto, sin comisión). */
    val pricePerShare: Double,

    /** quantity * pricePerShare */
    val grossTotal: Double,

    /** Comisión absoluta aplicada a la operación (0.5% del gross). */
    val commission: Double,

    /**
     * Total neto:
     * - BUY: gross + commission (sale de caja)
     * - SELL: gross - commission (entra a caja)
     */
    val netTotal: Double
) {
    init {
        require(id >= 0) { "Transaction.id no puede ser negativo" }
        require(timestamp >= 0L) { "Transaction.timestamp inválido: $timestamp" }

        val t = ticker.trim()
        require(t.isNotEmpty()) { "Transaction.ticker no puede estar vacío" }

        require(quantity > 0) { "Transaction.quantity debe ser > 0" }

        require(pricePerShare.isFinite() && pricePerShare >= 0.0) { "Transaction.pricePerShare inválido: $pricePerShare" }
        require(grossTotal.isFinite() && grossTotal >= 0.0) { "Transaction.grossTotal inválido: $grossTotal" }
        require(commission.isFinite() && commission >= 0.0) { "Transaction.commission inválido: $commission" }
        require(netTotal.isFinite() && netTotal >= 0.0) { "Transaction.netTotal inválido: $netTotal" }

        // Coherencia: gross ≈ qty * price
        val expectedGross = pricePerShare * quantity.toDouble()
        require(almostEquals(grossTotal, expectedGross)) {
            "Transaction.grossTotal no cuadra (esperado=$expectedGross, recibido=$grossTotal)"
        }

        // Coherencia: net según tipo
        val expectedNet = when (type) {
            TransactionType.BUY -> grossTotal + commission
            TransactionType.SELL -> grossTotal - commission
        }
        require(almostEquals(netTotal, expectedNet)) {
            "Transaction.netTotal no cuadra para $type (esperado=$expectedNet, recibido=$netTotal)"
        }

        // BUY debe pagar >= gross (por comisión); SELL debe recibir <= gross
        when (type) {
            TransactionType.BUY -> require(netTotal + 1e-9 >= grossTotal) { "BUY netTotal debe ser >= grossTotal" }
            TransactionType.SELL -> require(netTotal <= grossTotal + 1e-9) { "SELL netTotal debe ser <= grossTotal" }
        }

        // Opcional: evitar nombres “vacíos” si vienen
        require(companyName == null || companyName.trim().isNotEmpty()) {
            "Transaction.companyName si existe no puede estar vacío"
        }
    }

    fun normalizedTicker(): String = ticker.trim().uppercase()

    /** Utilidad para CSV/UI: "BUY"/"SELL" */
    fun typeLabel(): String = type.name

    /**
     * Tolerancia robusta para doubles (abs + relativa)
     */
    private fun almostEquals(
        a: Double,
        b: Double,
        absEps: Double = 1e-6,
        relEps: Double = 1e-9
    ): Boolean {
        val diff = abs(a - b)
        val scale = max(1.0, max(abs(a), abs(b)))
        return diff <= max(absEps, relEps * scale)
    }
}
