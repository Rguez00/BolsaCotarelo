package org.example.project.domain.model

import kotlinx.datetime.Instant

data class NewsEvent(
    val timestamp: Instant,
    val sector: Sector,
    val title: String,
    val impactPercent: Double // ej: +3.0 o -4.0
)
