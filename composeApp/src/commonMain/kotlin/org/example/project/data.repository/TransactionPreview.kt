package org.example.project.data.repository

/**
 * Resultado calculado para confirmación previa.
 *
 * - grossTotal = pricePerShare * quantity
 * - commission = grossTotal * 0.005
 * - netTotal:
 *   - BUY  -> grossTotal + commission
 *   - SELL -> grossTotal - commission
 */
data class TransactionPreview(
    val ticker: String,
    val quantity: Int,
    val pricePerShare: Double,
    val grossTotal: Double,
    val commission: Double,
    val netTotal: Double
) {
    init {
        require(ticker.isNotBlank()) { "TransactionPreview.ticker no puede estar vacío" }
        require(quantity > 0) { "TransactionPreview.quantity debe ser > 0" }
        require(pricePerShare.isFinite() && pricePerShare >= 0.0) { "pricePerShare inválido: $pricePerShare" }
        require(grossTotal.isFinite() && grossTotal >= 0.0) { "grossTotal inválido: $grossTotal" }
        require(commission.isFinite() && commission >= 0.0) { "commission inválido: $commission" }
        require(netTotal.isFinite() && netTotal >= 0.0) { "netTotal inválido: $netTotal" }
    }
}
