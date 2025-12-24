package org.example.project.domain.model

data class Stock(
    val name: String,
    val ticker: String,
    val sector: Sector,
    val initialPrice: Double,

    // 0.5 estable, 1.0 normal, 1.5+ volátil
    // Recomendado: mantener entre 0.3 y 2.5
    val volatility: Double = 1.0
)
