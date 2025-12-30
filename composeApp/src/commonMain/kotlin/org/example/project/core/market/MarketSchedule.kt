package org.example.project.core.market

import kotlinx.datetime.LocalTime

data class MarketSchedule(
    val openTime: LocalTime,
    val closeTime: LocalTime
)
