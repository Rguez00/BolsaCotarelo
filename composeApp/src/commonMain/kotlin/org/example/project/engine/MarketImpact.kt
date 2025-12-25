package org.example.project.engine

import org.example.project.domain.model.Sector

data class MarketImpact(
    val trendBiasPercent: Double,               // ej: +0.6 (por tick)
    val sectorBiasPercent: Map<Sector, Double>  // ej: ENERGY -> -1.5 (por tick)
)
