package org.example.project.core.market

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
                runCatching { checkMarket() }.onFailure { it.printStackTrace() }
                delay(1_000) // para pruebas rápidas
            }
        }

        checkNow()
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun checkNow() {
        scope.launch(Dispatchers.Default) {
            runCatching { checkMarket() }.onFailure { it.printStackTrace() }
        }
    }

    private suspend fun checkMarket() {
        val state = marketRepository.marketState.value

        // Si automático OFF, el MarketClock no toca isOpen.
        // (El mercado lo controlas manual desde UI/Engine)
        if (!state.autoScheduleEnabled) return

        val tz = TimeZone.currentSystemDefault()
        val nowInstant = Clock.System.now()
        val nowLocal = nowInstant.toLocalDateTime(tz)
        val nowEpochMs = nowInstant.toEpochMilliseconds()

        // ✅ Override manual: si está activo y NO expiró, no pisamos isOpen.
        val overrideStillValid =
            state.manualOverrideActive && state.manualOverrideUntilEpochMs > nowEpochMs

        if (overrideStillValid) {
            println(
                "MarketClockDBG | tz=$tz local=%02d:%02d auto=true override=ON(until=%d) " +
                        "open=%02d:%02d close=%02d:%02d isOpen=%s paused=%s -> skip"
                            .format(
                                nowLocal.hour, nowLocal.minute,
                                state.manualOverrideUntilEpochMs,
                                state.schedule.openTime.hour, state.schedule.openTime.minute,
                                state.schedule.closeTime.hour, state.schedule.closeTime.minute,
                                state.isOpen, state.isPaused
                            )
            )
            return
        }

        // ✅ Si el override estaba marcado pero ya expiró, lo limpiamos
        // para que UI/estado no se queden “sucios”.
        if (state.manualOverrideActive) {
            marketRepository.clearManualOverride()
        }

        // Hora local del dispositivo
        val currentTotalMinutes = nowLocal.hour * 60 + nowLocal.minute
        val openTotalMinutes = state.schedule.openTime.hour * 60 + state.schedule.openTime.minute
        val closeTotalMinutes = state.schedule.closeTime.hour * 60 + state.schedule.closeTime.minute

        val shouldBeOpen = if (openTotalMinutes <= closeTotalMinutes) {
            currentTotalMinutes in openTotalMinutes until closeTotalMinutes
        } else {
            // horario que cruza medianoche
            currentTotalMinutes >= openTotalMinutes || currentTotalMinutes < closeTotalMinutes
        }

        println(
            "MarketClockDBG | tz=$tz local=%02d:%02d auto=true override=OFF " +
                    "open=%02d:%02d close=%02d:%02d shouldBeOpen=%s isOpen=%s paused=%s"
                        .format(
                            nowLocal.hour, nowLocal.minute,
                            state.schedule.openTime.hour, state.schedule.openTime.minute,
                            state.schedule.closeTime.hour, state.schedule.closeTime.minute,
                            shouldBeOpen, state.isOpen, state.isPaused
                        )
        )

        if (state.isOpen != shouldBeOpen) {
            marketRepository.setMarketOpen(shouldBeOpen)

            // Si el horario ABRE el mercado, lo reanudamos
            if (shouldBeOpen && state.isPaused) {
                marketRepository.setPaused(false)
            }
        }
    }
}
