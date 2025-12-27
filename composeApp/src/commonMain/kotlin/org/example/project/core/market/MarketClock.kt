package org.example.project.core.market

import kotlinx.coroutines.*
import org.example.project.data.repository.MarketRepository

class MarketClock(
    private val marketRepository: MarketRepository,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()

        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkMarket()
                delay(60_000)
            }
        }

    }

    private suspend fun checkMarket() {
        val state = marketRepository.marketState.value
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        val openHour = state.schedule.openTime.hour
        val openMinute = state.schedule.openTime.minute
        val closeHour = state.schedule.closeTime.hour
        val closeMinute = state.schedule.closeTime.minute

        // Convertir a minutos totales del día para comparar mejor
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val openTotalMinutes = openHour * 60 + openMinute
        val closeTotalMinutes = closeHour * 60 + closeMinute

        val shouldBeOpen = currentTotalMinutes >= openTotalMinutes && currentTotalMinutes < closeTotalMinutes

        if (state.isOpen != shouldBeOpen) {
            marketRepository.setMarketOpen(shouldBeOpen)
        }
    }
    fun stop() {
        job?.cancel()
    }
}