package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.example.project.data.repository.PortfolioError

class PortfolioMarketClosedTest {

    @Test
    fun buy_fails_when_market_is_paused() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        marketRepo.setPaused(true)

        val r = repo.buy("NBS", 1)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() === PortfolioError.MarketClosedOrPaused)
    }

    @Test
    fun previewBuy_fails_when_market_is_closed() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        marketRepo.setMarketOpen(false)

        val r = repo.previewBuy("NBS", 1)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() === PortfolioError.MarketClosedOrPaused)
    }
}
