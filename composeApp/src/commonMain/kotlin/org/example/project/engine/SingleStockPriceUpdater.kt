package org.example.project.engine

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.example.project.data.repository.MarketRepository
import org.example.project.domain.model.StockSnapshot
import kotlin.math.abs
import kotlin.random.Random

class SingleStockPriceUpdater(
    private val repo: MarketRepository
) {

    suspend fun run(ticker: String) {
        val ctx = currentCoroutineContext()
        val t = normalizeTicker(ticker)

        while (ctx.isActive) {

            // 1) Respetar mercado y pausa
            val stateBeforeDelay = repo.marketState.value
            if (!stateBeforeDelay.isOpen || stateBeforeDelay.isPaused) {
                delay(300)
                continue
            }

            // 2) Delay escalado por simSpeed
            val speed = stateBeforeDelay.simSpeed.coerceIn(0.25, 10.0)
            val baseDelay = Random.nextLong(1_000L, 3_000L)
            val scaledDelay = (baseDelay / speed).toLong().coerceAtLeast(50L)
            delay(scaledDelay)

            // 3) Re-chequeo tras delay
            val state = repo.marketState.value
            if (!state.isOpen || state.isPaused) continue

            val current: StockSnapshot = repo.getSnapshot(t) ?: continue

            // 4) Base aleatoria (-5%..+5%) modulada por volatilidad, clamp final [-5..+5]
            val vol = current.volatility.coerceIn(0.5, 2.0)
            val basePercentRaw = Random.nextDouble(-5.0, 5.0)
            val basePercent = (basePercentRaw * vol).coerceIn(-5.0, 5.0)

            // 5) Bias por tendencia + sector
            val trendBiasPercent = repo.getTrendBiasPercent()
            val sectorBiasPercent = repo.getSectorBiasPercent(current.sector)

            // 6) Cambio efectivo total (clamp final por requisito)
            val effectivePercent = (basePercent + trendBiasPercent + sectorBiasPercent)
                .coerceIn(-5.0, 5.0)

            val percentChange = effectivePercent / 100.0

            // 7) Nuevo precio (mínimo 0.01€)
            val newPrice = (current.currentPrice * (1.0 + percentChange))
                .coerceAtLeast(0.01)

            val newHigh = maxOf(current.highPrice, newPrice)
            val newLow = minOf(current.lowPrice, newPrice)

            val changeEuro = newPrice - current.openPrice
            val changePercent = if (current.openPrice > 0.0) {
                (changeEuro / current.openPrice) * 100.0
            } else 0.0

            // Historial máximo 100 valores
            val newHistory = (current.priceHistory + newPrice).takeLast(100)

            // Volumen “realista”
            val moveFactor = (abs(effectivePercent) / 5.0).coerceIn(0.2, 2.0)
            val addVolume = (Random.nextLong(10, 200) * moveFactor).toLong()

            val newSnapshot = current.copy(
                currentPrice = newPrice,
                highPrice = newHigh,
                lowPrice = newLow,
                changeEuro = changeEuro,
                changePercent = changePercent,
                volume = current.volume + addVolume,
                priceHistory = newHistory
            )

            repo.updateSnapshot(t, newSnapshot)
        }
    }

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()
}
