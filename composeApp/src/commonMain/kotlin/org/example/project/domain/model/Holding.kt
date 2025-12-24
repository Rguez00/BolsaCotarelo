package org.example.project.domain.model

data class Holding(
    val ticker: String,
    val quantity: Int,
    val avgBuyPrice: Double // precio medio ponderado de compra
) {
    init {
        require(ticker.trim().isNotEmpty()) { "Holding.ticker no puede estar vacío" }
        require(quantity > 0) { "Holding.quantity debe ser > 0" }
        require(avgBuyPrice.isFinite() && avgBuyPrice >= 0.0) { "Holding.avgBuyPrice inválido: $avgBuyPrice" }
    }
}
