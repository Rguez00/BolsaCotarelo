package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.example.project.engine.SingleStockPriceUpdater

class SingleStockPriceUpdaterTest {

    @Test
    fun updater_emits_update_when_market_open() = runBlocking {
        val marketRepo = testMarketRepo()
        marketRepo.setMarketOpen(true)
        marketRepo.setPaused(false)
        marketRepo.setSimSpeed(10.0)

        val updater = SingleStockPriceUpdater(marketRepo)

        val job = launch { updater.run("nbs") } // testea normalización del ticker

        val update = withTimeout(2_000) {
            marketRepo.priceUpdates
                .filter { it.ticker == "NBS" }
                .first()
        }

        assertEquals("NBS", update.ticker)
        assertTrue(update.currentPrice > 0.0)
        assertNotNull(update.priceHistory)

        job.cancelAndJoin()
    }

    @Test
    fun updater_does_not_emit_when_paused() = runBlocking {
        val marketRepo = testMarketRepo()
        marketRepo.setMarketOpen(true)
        marketRepo.setPaused(true)
        marketRepo.setSimSpeed(10.0)

        val updater = SingleStockPriceUpdater(marketRepo)

        val job = launch { updater.run("NBS") }

        val update = withTimeoutOrNull(400) {
            marketRepo.priceUpdates
                .filter { it.ticker == "NBS" }
                .first()
        }

        assertEquals(null, update)

        job.cancelAndJoin()
    }
}
