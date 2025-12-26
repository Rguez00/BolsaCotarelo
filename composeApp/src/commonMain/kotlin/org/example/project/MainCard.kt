package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.project.core.util.fmt2
import org.example.project.domain.model.AlertRule
import org.example.project.domain.model.AlertTriggered
import org.example.project.domain.model.MarketTrend
import org.example.project.domain.model.NewsEvent
import org.example.project.domain.model.PortfolioStatistics
import org.example.project.domain.model.StockSnapshot
import org.example.project.presentation.state.MarketState
import org.example.project.presentation.state.PortfolioState
import kotlin.math.abs
import org.example.project.presentation.charts.ProfitBarChart
import org.example.project.presentation.charts.BarItem
import org.example.project.presentation.charts.PieChart
import org.example.project.presentation.charts.PieSlice


@Composable
internal fun MainCard(
    modifier: Modifier,
    p: AppPalette,
    sectionGap: Dp,
    innerPadH: Dp,
    innerPadV: Dp,
    marketState: MarketState,
    portfolioState: PortfolioState,
    tab: AppTab,
    featured: StockSnapshot?,
    selectedTicker: String,
    onSelectTicker: (String) -> Unit,
    showFeatured: Boolean,
    onToggleFeatured: () -> Unit,
    showNewsExpanded: Boolean,
    onToggleNews: () -> Unit,
    showPortfolioExpanded: Boolean,
    onTogglePortfolio: () -> Unit,
    pctColor: (Double) -> Color,
    arrow: (Double) -> String,
    onToggleOpen: () -> Unit,
    onTogglePause: () -> Unit,
    onSetSpeed: (Double) -> Unit,
    canTrade: Boolean,
    onBuy: (String) -> Unit,
    onSell: (String) -> Unit,
    priceHistory: Map<String, List<Double>>,
    valueHistory: List<Double>,
    alertRules: List<AlertRule>,
    triggeredAlerts: List<AlertTriggered>,
    banner: String?,
    onDismissBanner: () -> Unit,
    onCreateAlert: () -> Unit,
    onUpsertAlert: (AlertRule) -> Unit,
    onDeleteAlert: (Long) -> Unit,
    onOpenStrategies: () -> Unit,
    onExportPortfolioCsv: () -> Unit,
    statistics: PortfolioStatistics
) {
    val bars = portfolioState.profitBars
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier.fillMaxSize(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = p.surface0),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, p.stroke, shape)
                .padding(horizontal = innerPadH, vertical = innerPadV)
        ) {

            // HEADER
            HeaderCompact(
                isOpen = marketState.isOpen,
                simSpeed = marketState.simSpeed,
                trend = marketState.trend,
                stocksCount = marketState.stocks.size,
                success = p.success,
                danger = p.danger,
                textStrong = p.textStrong,
                textSoft = p.textSoft
            )

            Spacer(Modifier.height(sectionGap))
            Divider(color = p.strokeSoft)
            Spacer(Modifier.height(sectionGap))

            // TOP BAR
            CompactTopBarUltra(
                cash = portfolioState.cash,
                value = portfolioState.portfolioValue,
                pnlEuro = portfolioState.pnlEuro,
                pnlPercent = portfolioState.pnlPercent,
                isOpen = marketState.isOpen,
                isPaused = marketState.isPaused,
                simSpeed = marketState.simSpeed,
                surface = p.surface1,
                inner = p.surface2,
                stroke = p.strokeSoft,
                textStrong = p.textStrong,
                textSoft = p.textSoft,
                textMuted = p.textMuted,
                success = p.success,
                danger = p.danger,
                neutral = p.neutral,
                onToggleOpen = onToggleOpen,
                onTogglePause = onTogglePause,
                onSetSpeed = onSetSpeed
            )

            Spacer(Modifier.height(sectionGap))

            when (tab) {

                // =========================================================
                // MERCADO
                // =========================================================
                AppTab.MARKET -> {

                    FeaturedMini(
                        featured = featured,
                        show = showFeatured,
                        onToggle = onToggleFeatured,
                        brand = p.brand,
                        brand2 = p.brand2,
                        surface = p.surface1,
                        inner = p.surface2,
                        stroke = p.strokeSoft,
                        textStrong = p.textStrong,
                        textSoft = p.textSoft,
                        textMuted = p.textMuted,
                        pctColor = pctColor,
                        arrow = arrow
                    )

                    Spacer(Modifier.height(sectionGap))

                    NewsTickerScrollable(
                        news = marketState.news,
                        expanded = showNewsExpanded,
                        onToggle = onToggleNews,
                        surface = p.surface1,
                        inner = p.surface2,
                        stroke = p.strokeSoft,
                        textStrong = p.textStrong,
                        textSoft = p.textSoft,
                        neutral = p.neutral,
                        success = p.success,
                        danger = p.danger
                    )

                    Spacer(Modifier.height(sectionGap))
                    Divider(color = p.strokeSoft)
                    Spacer(Modifier.height(sectionGap))

                    Text("Mercado", color = p.textStrong, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(marketState.stocks, key = { it.ticker }) { s ->
                            val isSelected = s.ticker == selectedTicker
                            val rowShape = RoundedCornerShape(16.dp)
                            val border = if (isSelected) p.brand.copy(alpha = 0.45f) else p.strokeSoft

                            Card(
                                colors = CardDefaults.cardColors(containerColor = p.surface2),
                                shape = rowShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, border, rowShape)
                                    .clickable { onSelectTicker(s.ticker) }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            s.ticker,
                                            color = p.textStrong,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Text("${fmt2(s.currentPrice)} €", color = p.textStrong)
                                    }

                                    Text(
                                        s.name,
                                        color = p.textSoft,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        "${arrow(s.changePercent)} ${fmt2(s.changePercent)}%",
                                        color = pctColor(s.changePercent),
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    val buyColors = ButtonDefaults.buttonColors(
                                        containerColor = p.success.copy(alpha = 0.95f),
                                        contentColor = Color(0xFF001018),
                                        disabledContainerColor = p.neutral.copy(alpha = 0.18f),
                                        disabledContentColor = p.neutral
                                    )
                                    val sellColors = ButtonDefaults.buttonColors(
                                        containerColor = p.danger.copy(alpha = 0.95f),
                                        contentColor = Color(0xFF001018),
                                        disabledContainerColor = p.neutral.copy(alpha = 0.18f),
                                        disabledContentColor = p.neutral
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onSelectTicker(s.ticker); onBuy(s.ticker) },
                                            enabled = canTrade,
                                            modifier = Modifier.weight(1f),
                                            colors = buyColors,
                                            shape = RoundedCornerShape(999.dp)
                                        ) { Text("BUY", fontWeight = FontWeight.SemiBold) }

                                        Button(
                                            onClick = { onSelectTicker(s.ticker); onSell(s.ticker) },
                                            enabled = canTrade,
                                            modifier = Modifier.weight(1f),
                                            colors = sellColors,
                                            shape = RoundedCornerShape(999.dp)
                                        ) { Text("SELL", fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // PORTFOLIO
                // =========================================================
                AppTab.PORTFOLIO -> {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionPill(
                            text = "⚙ Estrategias",
                            bg = p.brand,
                            fg = Color(0xFF001018),
                            border = p.brand.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenStrategies
                        )
                        ActionPill(
                            text = "⬇ Export CSV",
                            bg = p.brand,
                            fg = Color(0xFF001018),
                            border = p.brand.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f),
                            onClick = onExportPortfolioCsv
                        )
                    }

                    Spacer(Modifier.height(sectionGap))

                    val wPortfolio = if (showPortfolioExpanded) 0.62f else 0.34f
                    val wTx = 1f - wPortfolio
                    val panelShape = RoundedCornerShape(16.dp)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = p.surface1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(wPortfolio),
                        shape = panelShape
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .border(1.dp, p.strokeSoft, panelShape)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTogglePortfolio() }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Portfolio",
                                    color = p.textStrong,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (showPortfolioExpanded) "Ocultar" else "Ver",
                                    color = p.textSoft,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Divider(color = p.strokeSoft)
                            Spacer(Modifier.height(10.dp))

                            if (!showPortfolioExpanded) {
                                Text(
                                    "Tap para ver detalle · ${portfolioState.positions.size} posiciones",
                                    color = p.textMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.weight(1f))
                            } else {

                                if (portfolioState.positions.isEmpty()) {
                                    Text(
                                        "Sin posiciones. Compra alguna acción para verla aquí.",
                                        color = p.textMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                } else {

                                    val buyColors = ButtonDefaults.buttonColors(
                                        containerColor = p.success.copy(alpha = 0.95f),
                                        contentColor = Color(0xFF001018),
                                        disabledContainerColor = p.neutral.copy(alpha = 0.18f),
                                        disabledContentColor = p.neutral
                                    )
                                    val sellColors = ButtonDefaults.buttonColors(
                                        containerColor = p.danger.copy(alpha = 0.95f),
                                        contentColor = Color(0xFF001018),
                                        disabledContainerColor = p.neutral.copy(alpha = 0.18f),
                                        disabledContentColor = p.neutral
                                    )

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(items = portfolioState.positions, key = { it.ticker }) { pos ->
                                            val cardShape = RoundedCornerShape(16.dp)

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = p.surface2),
                                                shape = cardShape,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, p.strokeSoft, cardShape)
                                                    .clickable { onSelectTicker(pos.ticker) }
                                            ) {
                                                Column(Modifier.padding(12.dp)) {

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            pos.ticker,
                                                            color = p.textStrong,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1
                                                        )
                                                        Text("Qty ${pos.quantity}", color = p.textSoft)
                                                    }

                                                    Spacer(Modifier.height(2.dp))

                                                    Text(
                                                        "Invertido: ${fmt2(pos.invested)}€ · Valor: ${fmt2(pos.valueNow)}€",
                                                        color = p.textSoft,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    val pnlColor = when {
                                                        pos.pnlEuro > 0.0001 -> p.success
                                                        pos.pnlEuro < -0.0001 -> p.danger
                                                        else -> p.neutral
                                                    }
                                                    val signEuro = if (pos.pnlEuro >= 0) "+" else ""
                                                    val signPct = if (pos.pnlPercent >= 0) "+" else ""

                                                    Text(
                                                        "PnL: $signEuro${fmt2(pos.pnlEuro)}€ · $signPct${fmt2(pos.pnlPercent)}%",
                                                        color = pnlColor,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(Modifier.height(10.dp))

                                                    val canSell = canTrade && pos.quantity > 0
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = { onSelectTicker(pos.ticker); onBuy(pos.ticker) },
                                                            enabled = canTrade,
                                                            modifier = Modifier.weight(1f),
                                                            colors = buyColors,
                                                            shape = RoundedCornerShape(999.dp)
                                                        ) { Text("BUY", fontWeight = FontWeight.SemiBold) }

                                                        Button(
                                                            onClick = { onSelectTicker(pos.ticker); onSell(pos.ticker) },
                                                            enabled = canSell,
                                                            modifier = Modifier.weight(1f),
                                                            colors = sellColors,
                                                            shape = RoundedCornerShape(999.dp)
                                                        ) { Text("SELL", fontWeight = FontWeight.SemiBold) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(sectionGap))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = p.surface1),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(wTx),
                        shape = panelShape
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .border(1.dp, p.strokeSoft, panelShape)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Transacciones",
                                    color = p.textStrong,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${portfolioState.transactions.size}",
                                    color = p.textSoft,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Divider(color = p.strokeSoft)
                            Spacer(Modifier.height(10.dp))

                            val txs = portfolioState.transactions.asReversed()
                            if (txs.isEmpty()) {
                                Text("Aún no hay operaciones.", color = p.textMuted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.weight(1f))
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(items = txs, key = { it.id }) { tx ->
                                        Card(colors = CardDefaults.cardColors(containerColor = p.surface2)) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(
                                                    "ID ${tx.id} · ${tx.type} · ${tx.ticker} x${tx.quantity}",
                                                    color = p.textStrong,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "Neto: ${fmt2(tx.netTotal)} €",
                                                    color = p.textSoft,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
// CHARTS (✅ IMPLEMENTADO + BARRAS)
// =========================================================
                AppTab.CHARTS -> {
                    val tickers = marketState.stocks.map { it.ticker }
                    val pointsPrice = priceHistory[selectedTicker].orEmpty()
                    val pointsValue = valueHistory

                    val barItems = bars.map { BarItem(label = it.ticker, value = it.valueEuro) }

                    val slicesBySector = run {
                        val sectorByTicker = marketState.stocks.associate { it.ticker.uppercase() to it.sector }

                        val bySector: Map<String, Double> = portfolioState.positions
                            .groupBy { pos ->
                                val key = pos.ticker.uppercase()
                                sectorByTicker[key]?.toString() ?: "Desconocido"
                            }
                            .mapValues { (_, positions) -> positions.sumOf { it.valueNow } }

                        val palette = listOf(
                            p.brand,
                            p.brand2,
                            p.success,
                            p.danger,
                            p.neutral,
                            p.brand.copy(alpha = 0.75f),
                            p.brand2.copy(alpha = 0.75f)
                        )

                        bySector.entries
                            .sortedByDescending { it.value }
                            .mapIndexed { i, e ->
                                PieSlice(
                                    label = e.key,
                                    value = e.value,
                                    color = palette[i % palette.size]
                                )
                            }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(sectionGap),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Gráficos",
                                    color = p.textStrong,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Ticker: $selectedTicker",
                                    color = p.textSoft,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        item {
                            FlowRowCompat(
                                modifier = Modifier.fillMaxWidth(),
                                mainAxisSpacing = 8.dp,
                                crossAxisSpacing = 8.dp
                            ) {
                                tickers.take(12).forEach { t ->
                                    val selected = t == selectedTicker
                                    SmallChip(
                                        text = t,
                                        selected = selected,
                                        bg = if (selected) p.brand else p.surface2,
                                        fg = if (selected) Color(0xFF001018) else p.textSoft,
                                        stroke = if (selected) p.brand.copy(alpha = 0.45f) else p.strokeSoft,
                                        onClick = { onSelectTicker(t) }
                                    )
                                }
                                if (tickers.size > 12) {
                                    SmallChip(
                                        text = "+${tickers.size - 12}",
                                        selected = false,
                                        bg = p.surface2,
                                        fg = p.textMuted,
                                        stroke = p.strokeSoft,
                                        onClick = { }
                                    )
                                }
                            }
                        }

                        item {
                            ChartCard(
                                title = "Precio · $selectedTicker",
                                subtitle = chartSubtitle(pointsPrice, suffix = "€"),
                                p = p,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) {
                                LineChart(
                                    points = pointsPrice,
                                    line = p.brand2,
                                    stroke = p.strokeSoft,
                                    inner = p.surface2,
                                    textSoft = p.textSoft,
                                    textMuted = p.textMuted
                                )
                            }
                        }

                        item {
                            ChartCard(
                                title = "Distribución del portfolio por sector",
                                subtitle = if (slicesBySector.isEmpty()) "Sin posiciones" else "Reparto por valor actual",
                                p = p,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            ) {
                                PieChart(
                                    slices = slicesBySector,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        }

                        item {
                            ChartCard(
                                title = "Beneficio / Pérdida por acción",
                                subtitle = if (barItems.isEmpty()) "Sin posiciones" else "Top ${barItems.size} por impacto",
                                p = p,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                ProfitBarChart(items = barItems)
                            }
                        }

                        item {
                            ChartCard(
                                title = "Valor total portfolio",
                                subtitle = chartSubtitle(pointsValue, suffix = "€"),
                                p = p,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) {
                                LineChart(
                                    points = pointsValue,
                                    line = p.brand,
                                    stroke = p.strokeSoft,
                                    inner = p.surface2,
                                    textSoft = p.textSoft,
                                    textMuted = p.textMuted
                                )
                            }
                        }
                    }
                }



                // =========================================================
                // ALERTS
                // =========================================================
                AppTab.ALERTS -> {
                    AlertsPanel(
                        marketStocks = marketState.stocks,
                        canTrade = canTrade,
                        alertRules = alertRules,
                        triggeredAlerts = triggeredAlerts,
                        banner = banner,
                        onDismissBanner = onDismissBanner,
                        onCreate = onCreateAlert,
                        onUpsert = onUpsertAlert,
                        onDelete = onDeleteAlert,
                        surface = p.surface1,
                        inner = p.surface2,
                        stroke = p.strokeSoft,
                        textStrong = p.textStrong,
                        textSoft = p.textSoft,
                        textMuted = p.textMuted,
                        brand = p.brand,
                        neutral = p.neutral,
                        success = p.success,
                        danger = p.danger
                    )
                    Spacer(Modifier.weight(1f))
                }

                // =========================================================
                // STATS (✅ UI MEJORADA)
                // =========================================================
                AppTab.STATS -> {
                    val invested = portfolioState.positions.sumOf { it.invested }
                    val valueNow = portfolioState.positions.sumOf { it.valueNow }
                    val totalValue = portfolioState.cash + valueNow

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(sectionGap),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "📊 Estadísticas",
                                    color = p.textStrong,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Posiciones: ${statistics.totalPositions}",
                                    color = p.textSoft,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        item {
                            FlowRowCompat(
                                modifier = Modifier.fillMaxWidth(),
                                mainAxisSpacing = 10.dp,
                                crossAxisSpacing = 10.dp
                            ) {
                                StatMini(
                                    title = "Ventas con beneficio",
                                    value = "${statistics.profitableSells}/${statistics.totalSells}",
                                    p = p
                                )
                                StatMini(
                                    title = "Tasa de éxito",
                                    value = "${fmt2(statistics.successRate)}%",
                                    p = p
                                )
                                StatMini(
                                    title = "Rentabilidad media",
                                    value = "${fmt2(statistics.averageProfitability)}%",
                                    p = p
                                )
                                StatMini(
                                    title = "PnL actual",
                                    value = "${fmt2(portfolioState.pnlEuro)}€",
                                    valueColor = when {
                                        portfolioState.pnlEuro > 0.0001 -> p.success
                                        portfolioState.pnlEuro < -0.0001 -> p.danger
                                        else -> p.neutral
                                    },
                                    p = p
                                )
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = p.surface1),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, p.strokeSoft, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("Resumen", color = p.textStrong, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(10.dp))
                                    Divider(color = p.strokeSoft)
                                    Spacer(Modifier.height(10.dp))

                                    StatRow("Saldo", "${fmt2(portfolioState.cash)} €", p)
                                    StatRow("Invertido", "${fmt2(invested)} €", p)
                                    StatRow("Valor posiciones", "${fmt2(valueNow)} €", p)
                                    StatRow("Valor total", "${fmt2(totalValue)} €", p)

                                    Spacer(Modifier.height(10.dp))
                                    Divider(color = p.strokeSoft)
                                    Spacer(Modifier.height(10.dp))

                                    val signPct = if (portfolioState.pnlPercent >= 0) "+" else ""
                                    StatRow(
                                        "PnL (%)",
                                        "$signPct${fmt2(portfolioState.pnlPercent)}%",
                                        p,
                                        valueColor = when {
                                            portfolioState.pnlPercent > 0.0001 -> p.success
                                            portfolioState.pnlPercent < -0.0001 -> p.danger
                                            else -> p.neutral
                                        }
                                    )

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tip: usa CHARTS para ver evolución de precio y valor.",
                                        color = p.textMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = p.surface1),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, p.strokeSoft, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("Mejores / Peores", color = p.textStrong, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(10.dp))
                                    Divider(color = p.strokeSoft)
                                    Spacer(Modifier.height(10.dp))

                                    StatRow("Mejor venta", statistics.bestTransaction?.ticker ?: "--", p)
                                    StatRow("Peor venta", statistics.worstTransaction?.ticker ?: "--", p)
                                    StatRow("Acción más rentable", statistics.mostProfitableStock?.ticker ?: "--", p)
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

/* =========================
   CHARTS helpers
   ========================= */

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    p: AppPalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = p.surface1),
        shape = shape,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .border(1.dp, p.strokeSoft, shape)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = p.textStrong, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(subtitle, color = p.textSoft, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
            Divider(color = p.strokeSoft)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

private fun chartSubtitle(points: List<Double>, suffix: String): String {
    if (points.size < 2) return "—"
    val first = points.first()
    val last = points.last()
    val diff = last - first
    val sign = if (diff >= 0) "+" else ""
    val pct = if (abs(first) < 1e-9) 0.0 else (diff / first) * 100.0
    return "$sign${fmt2(diff)}$suffix · $sign${fmt2(pct)}%"
}

@Composable
private fun LineChart(
    points: List<Double>,
    line: Color,
    stroke: Color,
    inner: Color,
    textSoft: Color,
    textMuted: Color
) {
    if (points.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(inner)
                .border(1.dp, stroke, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin datos suficientes…", color = textMuted, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    val range = (max - min).let { if (it < 1e-9) 1.0 else it }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(inner)
                .border(1.dp, stroke, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // grid simple
                val gridLines = 4
                for (i in 1 until gridLines) {
                    val y = h * i / gridLines
                    drawLine(
                        color = stroke.copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val path = Path()
                points.forEachIndexed { i, v ->
                    val x = (i.toFloat() / (points.size - 1).toFloat()) * w
                    val yNorm = ((v - min) / range).toFloat()
                    val y = h - (yNorm * h)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = line,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // punto final
                val lastIdx = points.lastIndex
                val lastX = (lastIdx.toFloat() / (points.size - 1).toFloat()) * w
                val lastYNorm = ((points.last() - min) / range).toFloat()
                val lastY = h - (lastYNorm * h)
                drawCircle(color = line, radius = 6f, center = Offset(lastX, lastY))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("min: ${fmt2(min)}", color = textSoft, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("max: ${fmt2(max)}", color = textSoft, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/* =========================
   STATS helpers
   ========================= */

@Composable
private fun StatMini(
    title: String,
    value: String,
    p: AppPalette,
    valueColor: Color = p.textStrong
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .widthIn(min = 150.dp)
            .clip(shape)
            .background(p.surface1)
            .border(1.dp, p.strokeSoft, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(title, color = p.textMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, p: AppPalette, valueColor: Color = p.textSoft) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.textMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(6.dp))
}

/* =========================
   Existing helpers
   ========================= */

@Composable
private fun ActionPill(
    text: String,
    bg: Color,
    fg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SmallChip(
    text: String,
    selected: Boolean,
    bg: Color,
    fg: Color,
    stroke: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, stroke, shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * FlowRow sin dependencia extra:
 * - en Compose 1.6+ existe FlowRow en foundation.layout (experimental).
 * - Para no romper, hago uno “compat” muy simple (wrap manual con Row+Column)
 *   que funciona bien con pocos chips.
 */
@Composable
private fun FlowRowCompat(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    content: @Composable () -> Unit
) {
    // Si ya usas FlowRow real, puedes sustituir este helper por FlowRow directamente.
    // Aquí lo dejo simple: una Column con una Row (para tu uso actual ya vale).
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun HeaderCompact(
    isOpen: Boolean,
    simSpeed: Double,
    trend: MarketTrend,
    stocksCount: Int,
    success: Color,
    danger: Color,
    textStrong: Color,
    textSoft: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bolsa Cotarelo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${if (isOpen) "ABIERTO" else "CERRADO"} · x${fmt2(simSpeed)} · ${trendLabel(trend)} · $stocksCount acciones",
                style = MaterialTheme.typography.bodySmall,
                color = textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val chipBg = if (isOpen) Color(0xFF0B2A1F) else Color(0xFF351218)
        val chipStroke = if (isOpen) success.copy(alpha = 0.45f) else danger.copy(alpha = 0.45f)
        val chipShape = RoundedCornerShape(999.dp)

        Card(
            shape = chipShape,
            colors = CardDefaults.cardColors(containerColor = chipBg),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.border(1.dp, chipStroke, chipShape)
        ) {
            Text(
                text = if (isOpen) "OPEN" else "CLOSED",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEAF2FF),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun CompactTopBarUltra(
    cash: Double,
    value: Double,
    pnlEuro: Double,
    pnlPercent: Double,
    isOpen: Boolean,
    isPaused: Boolean,
    simSpeed: Double,
    surface: Color,
    inner: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    textMuted: Color,
    success: Color,
    danger: Color,
    neutral: Color,
    onToggleOpen: () -> Unit,
    onTogglePause: () -> Unit,
    onSetSpeed: (Double) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    val pnlColor = when {
        pnlEuro > 0.0001 -> success
        pnlEuro < -0.0001 -> danger
        else -> neutral
    }
    val signEuro = if (pnlEuro >= 0) "+" else ""
    val signPct = if (pnlPercent >= 0) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, shape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPill("Saldo", "${fmt2(cash)} €", Modifier.weight(1f), inner, stroke, textMuted, textStrong)
                MiniPill("Valor", "${fmt2(value)} €", Modifier.weight(1f), inner, stroke, textMuted, textStrong)
                MiniPill(
                    "PnL",
                    "$signEuro${fmt2(pnlEuro)}€",
                    Modifier.weight(1f),
                    inner,
                    stroke,
                    textMuted,
                    pnlColor,
                    sub = "$signPct${fmt2(pnlPercent)}%"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlChip(
                    text = if (isOpen) "CERRAR" else "ABRIR",
                    bg = (if (isOpen) danger else success).copy(alpha = 0.16f),
                    stroke = (if (isOpen) danger else success).copy(alpha = 0.38f),
                    fg = if (isOpen) danger else success,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleOpen
                )
                ControlChip(
                    text = if (isPaused) "REANUDAR" else "PAUSAR",
                    bg = neutral.copy(alpha = 0.14f),
                    stroke = neutral.copy(alpha = 0.30f),
                    fg = Color(0xFFE3ECFF),
                    modifier = Modifier.weight(1f),
                    onClick = onTogglePause
                )

                SpeedInline(
                    current = simSpeed,
                    textSoft = textSoft,
                    strong = textStrong,
                    muted = textMuted,
                    stroke = stroke,
                    inner = inner,
                    onSetSpeed = onSetSpeed
                )
            }
        }
    }
}

@Composable
private fun MiniPill(
    title: String,
    value: String,
    modifier: Modifier,
    surface: Color,
    stroke: Color,
    titleColor: Color,
    valueColor: Color,
    sub: String? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(surface)
            .border(1.dp, stroke, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = titleColor, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
            if (sub != null) {
                Text(text = sub, style = MaterialTheme.typography.labelSmall, color = valueColor, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ControlChip(
    text: String,
    bg: Color,
    stroke: Color,
    fg: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, stroke, shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun SpeedInline(
    current: Double,
    textSoft: Color,
    strong: Color,
    muted: Color,
    stroke: Color,
    inner: Color,
    onSetSpeed: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(inner)
            .border(1.dp, stroke, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "x${fmt2(current)}",
            style = MaterialTheme.typography.labelSmall,
            color = textSoft,
            maxLines = 1
        )
        SpeedDot("0.5", 0.5, current, strong, muted, stroke, inner, onSetSpeed)
        SpeedDot("1", 1.0, current, strong, muted, stroke, inner, onSetSpeed)
        SpeedDot("2", 2.0, current, strong, muted, stroke, inner, onSetSpeed)
        SpeedDot("5", 5.0, current, strong, muted, stroke, inner, onSetSpeed)
    }
}

@Composable
private fun SpeedDot(
    label: String,
    speed: Double,
    current: Double,
    strong: Color,
    muted: Color,
    stroke: Color,
    inner: Color,
    onSetSpeed: (Double) -> Unit
) {
    val selected = abs(current - speed) < 1e-6
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) strong.copy(alpha = 0.14f) else inner
    val br = if (selected) strong.copy(alpha = 0.42f) else stroke
    val fg = if (selected) strong else muted

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, br, shape)
            .clickable { onSetSpeed(speed) }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun FeaturedMini(
    featured: StockSnapshot?,
    show: Boolean,
    onToggle: () -> Unit,
    brand: Color,
    brand2: Color,
    surface: Color,
    inner: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    textMuted: Color,
    pctColor: (Double) -> Color,
    arrow: (Double) -> String
) {
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, shape)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.verticalGradient(listOf(brand, brand2)))
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = featured?.name ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ticker: ${featured?.ticker ?: "--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val pct = featured?.changePercent ?: 0.0
                val c = pctColor(pct)
                val badgeShape = RoundedCornerShape(999.dp)
                Box(
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(c.copy(alpha = 0.14f))
                        .border(1.dp, c.copy(alpha = 0.32f), badgeShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${arrow(pct)} ${fmt2(pct)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = c,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            if (show) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = featured?.currentPrice?.let { "${fmt2(it)} €" } ?: "--",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textStrong,
                    maxLines = 1
                )

                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(inner)
                        .border(1.dp, stroke, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    val line = buildString {
                        append("Apertura: ")
                        append(featured?.openPrice?.let { fmt2(it) } ?: "--")
                        append("  ·  Máx: ")
                        append(featured?.highPrice?.let { fmt2(it) } ?: "--")
                        append("  ·  Mín: ")
                        append(featured?.lowPrice?.let { fmt2(it) } ?: "--")
                    }
                    Text(line, style = MaterialTheme.typography.bodySmall, color = textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Tap para ver detalle", style = MaterialTheme.typography.labelSmall, color = textMuted)
            }
        }
    }
}

@Composable
private fun NewsTickerScrollable(
    news: List<NewsEvent>,
    expanded: Boolean,
    onToggle: () -> Unit,
    surface: Color,
    inner: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    neutral: Color,
    success: Color,
    danger: Color
) {
    val shape = RoundedCornerShape(16.dp)
    val last = news.lastOrNull()

    val impactColor = when {
        last == null -> neutral
        last.impactPercent > 0.05 -> success
        last.impactPercent < -0.05 -> danger
        else -> neutral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, shape)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(impactColor.copy(alpha = 0.95f))
                )
                Spacer(Modifier.width(10.dp))

                Text(
                    text = if (last == null) "Sin noticias todavía…" else last.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val label = if (expanded) "Ocultar" else "Ver"
                Text(
                    text = "$label · ${news.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSoft
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Divider(color = stroke)
                Spacer(Modifier.height(10.dp))

                if (news.isEmpty()) {
                    Text("Sin noticias todavía…", style = MaterialTheme.typography.bodySmall, color = textSoft)
                } else {
                    val ordered = news.asReversed()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 2.dp)
                    ) {
                        items(items = ordered, key = { it.hashCode() }) { item ->
                            val c = when {
                                item.impactPercent > 0.05 -> success
                                item.impactPercent < -0.05 -> danger
                                else -> neutral
                            }
                            val rowShape = RoundedCornerShape(12.dp)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(inner)
                                    .border(1.dp, stroke, rowShape)
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(22.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(c.copy(alpha = 0.95f))
                                    )
                                    Spacer(Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textStrong,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Sector: ${item.sector}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSoft,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val badgeShape = RoundedCornerShape(999.dp)
                                    Box(
                                        modifier = Modifier
                                            .clip(badgeShape)
                                            .background(c.copy(alpha = 0.14f))
                                            .border(1.dp, c.copy(alpha = 0.32f), badgeShape)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        val sign = if (item.impactPercent >= 0) "+" else ""
                                        Text(
                                            text = "$sign${fmt2(item.impactPercent)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
