package org.example.project.domain.model

import kotlinx.datetime.Instant

data class AlertTriggered(
    val id: Int,
    val timestamp: Instant,
    val rule: AlertRule,
    val currentPrice: Double,
    val currentChangePercent: Double,
    val message: String
)
