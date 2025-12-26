package org.example.project.domain.strategy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.data.repository.MarketRepository
import org.example.project.data.repository.PortfolioRepository

/**
 * Adaptadores que conectan StrategyEngine con tus repos actuales.
 */
class RepoStrategyMarketBridge(
    private val marketRepo: MarketRepository
) : StrategyMarketBridge {

    override val priceUpdates: Flow<String> =
        marketRepo.priceUpdates.map { it.ticker }

    override suspend fun getSnapshot(ticker: String): StockSnapshotLite? {
        val s = marketRepo.getSnapshot(ticker) ?: return null
        return StockSnapshotLite(
            ticker = s.ticker,
            currentPrice = s.currentPrice,
            openPrice = s.openPrice,
            highPrice = s.highPrice,
            priceHistory = s.priceHistory
        )
    }

    override fun isMarketOpen(): Boolean =
        marketRepo.marketState.value.isOpen && !marketRepo.marketState.value.isPaused
}

class RepoStrategyPortfolioBridge(
    private val portfolioRepo: PortfolioRepository
) : StrategyPortfolioBridge {

    override suspend fun buy(ticker: String, qty: Int): Boolean =
        portfolioRepo.buy(ticker, qty).isSuccess

    override suspend fun sell(ticker: String, qty: Int): Boolean =
        portfolioRepo.sell(ticker, qty).isSuccess

    override suspend fun getPosition(ticker: String): PositionLite? {
        val snap = portfolioRepo.getSnapshot()
        val pos = snap.positions.firstOrNull { it.ticker.equals(ticker, ignoreCase = true) } ?: return null
        return PositionLite(
            qty = pos.quantity,
            avgBuyPrice = pos.avgBuyPrice
        )
    }
}
