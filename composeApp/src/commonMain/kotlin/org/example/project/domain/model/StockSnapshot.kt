package org.example.project.domain.model

/**
 * Snapshot “vivo” de una acción para UI + simulación.
 * - Incluye volatility porque el enunciado pide volatilidad por acción.
 * - priceHistory: últimos 100 precios (para gráficas).
 */
data class StockSnapshot(
    val name: String,
    val ticker: String,
    val sector: Sector,

    // Volatilidad propia de la acción (0.5 estable, 1.0 normal, 1.5+ volátil)
    val volatility: Double,

    val currentPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,

    val changeEuro: Double,
    val changePercent: Double,

    val volume: Long,

    val priceHistory: List<Double> // últimos 100 valores
)
