package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PortfolioBuySellTest {

    @Test
    fun buy_and_sell_updates_cash_and_holdings_consistently() = runBlocking {
        val marketRepo = testMarketRepo()
        val portfolioRepo = testPortfolioRepo(marketRepo)

        val ticker = "NBS"
        val qtyBuy = 10

        val snap = marketRepo.getSnapshot(ticker)
        assertNotNull(snap, "El ticker de prueba debe existir en InitialData.defaultStocks()")

        val price = snap.currentPrice
        val commissionRate = 0.005
        val expectedBuyCost = price * qtyBuy * (1.0 + commissionRate)

        // BUY
        val buyResult = portfolioRepo.buy(ticker, qtyBuy)
        assertTrue(buyResult.isSuccess, "La compra debería ser válida con cash inicial suficiente")

        val afterBuy = portfolioRepo.getSnapshot()
        afterBuy.validateBasics()

        val holdingAfterBuy = afterBuy.holdings.firstOrNull { it.ticker == ticker }
        assertNotNull(holdingAfterBuy, "Debe existir holding tras comprar")
        assertEquals(qtyBuy, holdingAfterBuy.quantity)

        assertEquals(10_000.0 - expectedBuyCost, afterBuy.cash, absoluteTolerance = 1e-6)

        // SELL parcial
        val qtySell = 4
        val expectedSellNet = price * qtySell * (1.0 - commissionRate)

        val sellResult = portfolioRepo.sell(ticker, qtySell)
        assertTrue(sellResult.isSuccess, "La venta parcial debería ser válida")

        val afterSell = portfolioRepo.getSnapshot()
        afterSell.validateBasics()

        val holdingAfterSell = afterSell.holdings.firstOrNull { it.ticker == ticker }
        assertNotNull(holdingAfterSell, "Debe seguir existiendo holding tras venta parcial")
        assertEquals(qtyBuy - qtySell, holdingAfterSell.quantity)

        val expectedFinalCash = (10_000.0 - expectedBuyCost) + expectedSellNet
        assertEquals(expectedFinalCash, afterSell.cash, absoluteTolerance = 1e-6)
    }
}
