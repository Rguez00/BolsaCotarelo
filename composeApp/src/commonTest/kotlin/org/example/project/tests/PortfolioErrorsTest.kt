package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.example.project.data.repository.PortfolioError

class PortfolioErrorsTest {

    @Test
    fun buy_fails_with_invalid_quantity() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        val r = repo.buy("NBS", 0)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is PortfolioError.InvalidQuantity)
    }

    @Test
    fun sell_fails_when_not_enough_holdings() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        val r = repo.sell("NBS", 1)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is PortfolioError.InsufficientHoldings)
    }
}
