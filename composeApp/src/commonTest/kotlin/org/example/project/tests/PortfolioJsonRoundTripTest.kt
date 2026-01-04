package org.example.project.tests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PortfolioJsonRoundTripTest {

    @Test
    fun export_import_roundtrip_restores_state() = runBlocking {
        val marketRepo = testMarketRepo()
        val repo = testPortfolioRepo(marketRepo)

        assertTrue(repo.buy("NBS", 5).isSuccess)
        assertTrue(repo.buy("HLP", 3).isSuccess)
        assertTrue(repo.sell("NBS", 2).isSuccess)

        val before = repo.getSnapshot()
        before.validateBasics()
        val beforeTxCount = repo.getTransactions().size

        val json = repo.exportStateJson()
        assertTrue(json.isSuccess)

        // Reset y restore
        assertTrue(repo.clearState().isSuccess)
        assertTrue(repo.importStateJson(json.getOrThrow()).isSuccess)

        val after = repo.getSnapshot()
        after.validateBasics()
        val afterTxCount = repo.getTransactions().size

        assertEquals(before.cash, after.cash, absoluteTolerance = 1e-6)
        assertEquals(before.holdings.map { it.ticker }.sorted(), after.holdings.map { it.ticker }.sorted())
        assertEquals(beforeTxCount, afterTxCount)
    }
}
