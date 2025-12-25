package org.example.project.domain.strategy

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class StrategyAction { BUY, SELL }

data class StrategyTrigger(
    val strategyId: Int,
    val ticker: String,
    val action: StrategyAction,
    val reason: String,
    val price: Double,
    val timestamp: Instant = Clock.System.now()
)
