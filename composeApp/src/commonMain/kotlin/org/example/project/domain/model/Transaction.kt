package org.example.project.domain.model

import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.max

data class Transaction(
    val id: Int,
    val timestamp: Long,

    val type: TransactionType,

    // identificador
    val ticker: String,

    // metadatos opcionales (útiles para CSV/estadísticas sin depender del market)
    val companyName: String? = null,
    val sector: Sector? = null,

    val quantity: Int,

    val pricePerShare: Double,
    val grossTotal: Double,  // quantity * pricePerShare
    val commission: Double,  // 0.5%
    val netTotal: Double     // BUY: gross + commission | SELL: gross - commission
) {
    init {
        require(id >= 0) { "Transaction.id no puede ser negativo" }

        val t = ticker.trim()
        require(t.isNotEmpty()) { "Transaction.ticker no puede estar vacío" }

        require(quantity > 0) { "Transaction.quantity debe ser > 0" }

        require(pricePerShare.isFinite() && pricePerShare >= 0.0) { "Transaction.pricePerShare inválido: $pricePerShare" }
        require(grossTotal.isFinite() && grossTotal >= 0.0) { "Transaction.grossTotal inválido: $grossTotal" }
        require(commission.isFinite() && commission >= 0.0) { "Transaction.commission inválido: $commission" }
        require(netTotal.isFinite() && netTotal >= 0.0) { "Transaction.netTotal inválido: $netTotal" }

        // Coherencia: gross ≈ qty * price
        val expectedGross = pricePerShare * quantity
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

        // Opcional: evitar nombres “vacíos” si vienen
        require(companyName == null || companyName.trim().isNotEmpty()) {
            "Transaction.companyName si existe no puede estar vacío"
        }
    }

    /**
     * Tolerancia robusta para doubles (abs + relativa)
     */
    private fun almostEquals(a: Double, b: Double, absEps: Double = 1e-6, relEps: Double = 1e-9): Boolean {
        val diff = abs(a - b)
        val scale = max(1.0, max(abs(a), abs(b)))
        return diff <= max(absEps, relEps * scale)
    }
}
