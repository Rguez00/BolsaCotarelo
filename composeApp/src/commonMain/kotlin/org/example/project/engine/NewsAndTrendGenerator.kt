package org.example.project.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.example.project.data.repository.MarketRepository
import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.Sector
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

class NewsAndTrendGenerator(
    private val repo: MarketRepository,
    private val engineScope: CoroutineScope
) {
    // 1 expiración activa por sector
    private val expireJobs: MutableMap<Sector, Job> = mutableMapOf()
    private val expireLock = Any()

    suspend fun runTrend() {
        val ctx = currentCoroutineContext()
        val trends = arrayOf(MarketTrend.BULLISH, MarketTrend.BEARISH, MarketTrend.NEUTRAL)

        while (ctx.isActive) {
            val state = repo.marketState.value

            if (!state.isOpen || state.isPaused) {
                delay(500)
                continue
            }

            val speed = state.simSpeed.coerceIn(0.25, 10.0)

            val baseDelay = Random.nextLong(30_000L, 60_000L)
            val scaledDelay = (baseDelay / speed).toLong().coerceAtLeast(250L)
            delay(scaledDelay)

            // ✅ Re-chequeo tras delay
            val stateAfter = repo.marketState.value
            if (!stateAfter.isOpen || stateAfter.isPaused) continue

            val trend = trends.random()

            // Bias global por tick (%)
            val trendBias = when (trend) {
                MarketTrend.BULLISH -> Random.nextDouble(0.05, 0.20)
                MarketTrend.BEARISH -> Random.nextDouble(-0.20, -0.05)
                MarketTrend.NEUTRAL -> 0.0
            }

            repo.setTrend(trend, trendBias)
        }
    }

    suspend fun runNews() {
        val ctx = currentCoroutineContext()
        val sectors = Sector.values().toList()

        try {
            while (ctx.isActive) {
                val state = repo.marketState.value

                if (!state.isOpen || state.isPaused) {
                    delay(500)
                    continue
                }

                val speed = state.simSpeed.coerceIn(0.25, 10.0)

                val baseDelay = Random.nextLong(8_000L, 15_000L)
                val scaledDelay = (baseDelay / speed).toLong().coerceAtLeast(200L)
                delay(scaledDelay)

                // ✅ Re-chequeo tras delay
                val stateAfter = repo.marketState.value
                if (!stateAfter.isOpen || stateAfter.isPaused) continue

                val sector = sectors.random()

                // Impacto headline en %
                val impactHeadline = Random.nextDouble(-4.0, 4.0)

                val event = NewsEvent(
                    timestamp = Clock.System.now(),
                    sector = sector,
                    title = buildTitle(sector, impactHeadline),
                    impactPercent = impactHeadline
                )
                repo.pushNews(event)

                // headline -> bias por tick (%)
                val intensity = (abs(impactHeadline) / 4.0).coerceIn(0.2, 1.0)
                val factor = Random.nextDouble(0.08, 0.20) * intensity
                val perTickBiasPercent =
                    (impactHeadline * factor).coerceIn(-1.0, 1.0)

                repo.setSectorBias(sector, perTickBiasPercent)

                // Expiración escalada (1 job por sector)
                val expireBase = Random.nextLong(20_000L, 40_000L)
                val expireScaled = (expireBase / speed).toLong().coerceAtLeast(500L)

                scheduleSectorBiasExpiration(
                    sector = sector,
                    delayMs = expireScaled,
                    expectedBias = perTickBiasPercent
                )
            }
        } finally {
            cancelAllExpirations()
        }
    }

    private fun scheduleSectorBiasExpiration(
        sector: Sector,
        delayMs: Long,
        expectedBias: Double
    ) {
        synchronized(expireLock) {
            expireJobs[sector]?.cancel()

            val job = engineScope.launch {
                delay(delayMs)

                // ✅ Evita borrar bias nuevo y evita igualdad exacta de Double
                val currentBias = repo.getSectorBiasPercent(sector)
                if (almostEquals(currentBias, expectedBias)) {
                    repo.setSectorBias(sector, 0.0)
                }
            }

            expireJobs[sector] = job
        }
    }

    private fun cancelAllExpirations() {
        synchronized(expireLock) {
            expireJobs.values.forEach { it.cancel() }
            expireJobs.clear()
        }
    }

    // ============================================================
    // KMP-safe formatting
    // ============================================================

    private fun buildTitle(sector: Sector, impact: Double): String {
        val sign = if (impact >= 0) "+" else ""
        val pct = fmt1(impact) // 1 decimal, KMP-safe

        return when (sector) {
            Sector.TECHNOLOGY -> "Tecnología en movimiento: $sign$pct%"
            Sector.ENERGY -> "Tensión energética: $sign$pct%"
            Sector.BANKING -> "Noticias bancarias: $sign$pct%"
            Sector.RETAIL -> "Consumo y comercio: $sign$pct%"
            Sector.HEALTHCARE -> "Salud e investigación: $sign$pct%"
        }
    }

    private fun fmt1(value: Double): String = fmtFixed(value, 1)

    private fun fmtFixed(value: Double, decimals: Int): String {
        val sign = if (value < 0) "-" else ""
        val absValue = abs(value)

        val pow10 = pow10(decimals)
        val scaled = (absValue * pow10.toDouble()).roundToLong()

        val intPart = scaled / pow10
        val fracPart = (scaled % pow10).toInt()

        return if (decimals == 0) {
            "$sign$intPart"
        } else {
            "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
        }
    }

    private fun pow10(decimals: Int): Long {
        var p = 1L
        repeat(decimals.coerceAtLeast(0)) { p *= 10L }
        return p
    }

    private fun almostEquals(a: Double, b: Double, eps: Double = 1e-9): Boolean =
        abs(a - b) <= eps
}
