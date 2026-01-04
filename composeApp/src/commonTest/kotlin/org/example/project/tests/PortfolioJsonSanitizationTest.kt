package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PortfolioJsonSanitizationTest {

    @Test
    fun import_ignores_invalid_holdings_and_transactions() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        // JSON intencionalmente “sucio”: holdings con qty <= 0, ticker vacío, avgBuyPrice negativo, tx inválidas...
        val dirtyJson = """
            {
              "type": "portfolio_v1",
              "cash": 5000.0,
              "nextTxId": 3,
              "holdings": [
                {"ticker":"", "quantity":10, "avgBuyPrice":10.0},
                {"ticker":"NBS", "quantity":0, "avgBuyPrice":10.0},
                {"ticker":"HLP", "quantity":5, "avgBuyPrice":-1.0}
              ],
              "transactions": [
                {"id":0,"timestamp":1,"type":"BUY","ticker":"NBS","quantity":1,"pricePerShare":1.0,"grossTotal":1.0,"commission":0.0,"netTotal":1.0},
                {"id":1,"timestamp":0,"type":"BUY","ticker":"NBS","quantity":1,"pricePerShare":1.0,"grossTotal":1.0,"commission":0.0,"netTotal":1.0},
                {"id":2,"timestamp":2,"type":"INVALID","ticker":"NBS","quantity":1,"pricePerShare":1.0,"grossTotal":1.0,"commission":0.0,"netTotal":1.0}
              ]
            }
        """.trimIndent()

        val r = repo.importStateJson(dirtyJson)
        assertTrue(r.isSuccess, "Import no debe romper aunque el JSON tenga datos inválidos")

        val snap = repo.getSnapshot()
        snap.validateBasics()

        // Todos los holdings eran inválidos => debe quedar vacío
        assertEquals(0, snap.holdings.size)

        // Todas las transacciones eran inválidas => debe quedar vacío
        assertEquals(0, repo.getTransactions().size)
    }
}
