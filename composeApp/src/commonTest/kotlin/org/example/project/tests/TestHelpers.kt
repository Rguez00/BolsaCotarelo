package org.example.project.tests

import kotlin.math.abs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.example.project.core.config.InitialData
import org.example.project.data.repository.InMemoryMarketRepository
import org.example.project.data.repository.InMemoryPortfolioRepository
import org.example.project.domain.model.PortfolioSnapshot

internal fun testMarketRepo(): InMemoryMarketRepository =
    InMemoryMarketRepository(InitialData.defaultStocks())

internal fun testPortfolioRepo(market: InMemoryMarketRepository): InMemoryPortfolioRepository {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    return InMemoryPortfolioRepository(
        marketRepo = market,
        externalScope = scope
    )
}

/** Extensión SOLO para tests: PortfolioSnapshot no tiene validate() en producción */
internal fun PortfolioSnapshot.validateBasics() {
    assertTrue(cash.isFinite() && cash >= 0.0, "cash inválido: $cash")
    assertTrue(holdingsValue.isFinite() && holdingsValue >= 0.0, "holdingsValue inválido: $holdingsValue")
    assertTrue(totalInvested.isFinite() && totalInvested >= 0.0, "totalInvested inválido: $totalInvested")
    assertTrue(portfolioValue.isFinite() && portfolioValue >= 0.0, "portfolioValue inválido: $portfolioValue")

    val expected = cash + holdingsValue
    assertTrue(abs(portfolioValue - expected) < 1e-3, "portfolioValue no cuadra: $portfolioValue vs $expected")
}
