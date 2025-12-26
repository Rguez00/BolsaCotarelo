package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.core.config.InitialData
import org.example.project.core.util.fmt2
import org.example.project.data.repository.InMemoryAlertsRepository
import org.example.project.data.repository.InMemoryMarketRepository
import org.example.project.data.repository.InMemoryPortfolioRepository
import org.example.project.domain.model.StockSnapshot
import org.example.project.domain.model.calculateStatistics
import org.example.project.domain.strategy.DipReference
import org.example.project.domain.strategy.InMemoryStrategiesRepository
import org.example.project.domain.strategy.StrategyRule
import org.example.project.engine.MarketEngine
import org.example.project.presentation.strategies.StrategiesConfigDialog
import org.example.project.presentation.ui.TradeDialog
import org.example.project.presentation.vm.PortfolioViewModel
import kotlin.math.abs

@Composable
fun AppRoot() {
    val appScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // ✅ CSV saver (archivo real: Android SAF / Desktop FileDialog)
    val csvSaver = rememberCsvFileSaver()

    val marketRepo = remember { InMemoryMarketRepository(InitialData.defaultStocks()) }
    val portfolioRepo = remember { InMemoryPortfolioRepository(marketRepo) }
    val strategiesRepo = remember { InMemoryStrategiesRepository() }
    val alertsRepo = remember { InMemoryAlertsRepository(marketRepo, externalScope = appScope) }

    val engine = remember {
        MarketEngine(
            marketRepo = marketRepo,
            portfolioRepo = portfolioRepo,
            strategiesRepo = strategiesRepo,
            externalScope = appScope
        )
    }

    LaunchedEffect(Unit) {
        strategiesRepo.upsert(
            StrategyRule.AutoBuyDip(
                id = 1, ticker = "NBS", dropPercent = 2.0,
                reference = DipReference.OPEN, budgetEuro = 250.0, cooldownMs = 12_000L
            )
        )
        strategiesRepo.upsert(
            StrategyRule.TakeProfit(
                id = 2, ticker = "NBS", profitPercent = 3.0,
                sellFraction = 1.0, cooldownMs = 12_000L
            )
        )
        strategiesRepo.upsert(
            StrategyRule.StopLoss(
                id = 3, ticker = "NBS", lossPercent = 3.0,
                sellFraction = 1.0, cooldownMs = 12_000L
            )
        )
    }

    val portfolioVm = remember {
        PortfolioViewModel(
            repo = portfolioRepo,
            canTradeProvider = { engine.marketState.value.isOpen && !engine.marketState.value.isPaused }
        )
    }

    LaunchedEffect(Unit) { engine.startAllTickers() }

    DisposableEffect(Unit) {
        onDispose {
            engine.close()
            portfolioRepo.close()
            portfolioVm.close()
            alertsRepo.close()
        }
    }

    val marketState by engine.marketState.collectAsState()
    val portfolioState by portfolioVm.portfolioState.collectAsState()
    val alertsState by alertsRepo.alertsState.collectAsState()

    val canTrade = marketState.isOpen && !marketState.isPaused

    // ---------------- UI STATE ----------------
    var tabKey by rememberSaveable { mutableStateOf(AppTab.MARKET.name) }
    val tab = AppTab.valueOf(tabKey)

    var showStrategiesDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTicker by rememberSaveable { mutableStateOf("NBS") }

    var showFeatured by rememberSaveable { mutableStateOf(false) }
    var showNewsExpanded by rememberSaveable { mutableStateOf(false) }
    var showPortfolioExpanded by rememberSaveable { mutableStateOf(true) }

    var showCreateAlert by rememberSaveable { mutableStateOf(false) }
    var banner by rememberSaveable { mutableStateOf<String?>(null) }

    // ✅ Export CSV UI state (fallback copiar)
    var showExportCsv by rememberSaveable { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") } // ✅ NO saveable (puede ser enorme)

    LaunchedEffect(alertsState.triggered.size) {
        banner = alertsState.triggered.lastOrNull()?.message
    }

    val featured: StockSnapshot? = marketState.stocks.maxByOrNull { it.changePercent }
    val p = remember { AppPalette.darkFintechWhiteBackdrop() }

    fun pctColor(pct: Double) = when {
        pct > 0.0001 -> p.success
        pct < -0.0001 -> p.danger
        else -> p.neutral
    }
    fun arrow(pct: Double) = when {
        pct > 0.0001 -> "▲"
        pct < -0.0001 -> "▼"
        else -> "•"
    }

    val safeToggleOpen: () -> Unit = {
        runCatching { engine.setMarketOpen(!marketState.isOpen) }.onFailure { it.printStackTrace() }
    }
    val safeTogglePause: () -> Unit = {
        runCatching { engine.setPaused(!marketState.isPaused) }.onFailure { it.printStackTrace() }
    }

    val statistics by remember {
        derivedStateOf {
            calculateStatistics(
                transactions = portfolioState.transactions,
                positions = portfolioState.positions
            )
        }
    }

    // charts history
    val maxPoints = 120
    val priceHistory = remember { mutableStateMapOf<String, MutableList<Double>>() }
    val valueHistory = remember { mutableStateListOf<Double>() }

    LaunchedEffect(marketState.stocks, portfolioState.cash, portfolioState.positions) {
        for (s in marketState.stocks) {
            val t = s.ticker
            val list = priceHistory.getOrPut(t) { mutableListOf() }
            val last = list.lastOrNull()
            if (last == null || abs(last - s.currentPrice) > 1e-6) {
                list.add(s.currentPrice)
                if (list.size > maxPoints) list.removeAt(0)
            }
        }

        val total = portfolioState.cash + portfolioState.positions.sumOf { it.valueNow }
        val lastTotal = valueHistory.lastOrNull()
        if (lastTotal == null || abs(lastTotal - total) > 1e-6) {
            valueHistory.add(total)
            if (valueHistory.size > maxPoints) valueHistory.removeAt(0)
        }
    }

    // ✅ CSV builder
    fun buildPortfolioCsv(): String {
        fun esc(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

        val totalValue = portfolioState.cash + portfolioState.positions.sumOf { it.valueNow }
        val sb = StringBuilder()

        sb.appendLine("section,key,value")
        sb.appendLine("summary,generated_at,${esc(kotlinx.datetime.Clock.System.now().toString())}")
        sb.appendLine("summary,cash_eur,${fmt2(portfolioState.cash)}")
        sb.appendLine("summary,portfolio_value_eur,${fmt2(portfolioState.portfolioValue)}")
        sb.appendLine("summary,total_value_eur,${fmt2(totalValue)}")
        sb.appendLine("summary,pnl_eur,${fmt2(portfolioState.pnlEuro)}")
        sb.appendLine("summary,pnl_percent,${fmt2(portfolioState.pnlPercent)}")
        sb.appendLine()

        sb.appendLine("ticker,quantity,invested_eur,value_now_eur,pnl_eur,pnl_percent")
        for (pos in portfolioState.positions) {
            sb.appendLine(
                listOf(
                    esc(pos.ticker),
                    pos.quantity.toString(),
                    fmt2(pos.invested),
                    fmt2(pos.valueNow),
                    fmt2(pos.pnlEuro),
                    fmt2(pos.pnlPercent)
                ).joinToString(",")
            )
        }
        sb.appendLine()

        sb.appendLine("tx_id,type,ticker,quantity,net_total_eur")
        for (tx in portfolioState.transactions) {
            sb.appendLine(
                listOf(
                    tx.id.toString(),
                    esc(tx.type.name),
                    esc(tx.ticker),
                    tx.quantity.toString(),
                    fmt2(tx.netTotal)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    // ✅ Export: intenta guardar archivo real; si se cancela/falla, abre diálogo para copiar
    val onExportCsv: () -> Unit = {
        val text = buildPortfolioCsv()
        csvText = text

        val rawTs = kotlinx.datetime.Clock.System.now().toString()
        val safeTs = rawTs
            .replace(":", "-")
            .replace(".", "-")
            .replace("Z", "")
        val fileName = "portfolio_$safeTs.csv"

        csvSaver.saveCsv(
            suggestedFileName = fileName,
            csvText = text
        ) { ok, msg ->
            if (ok) {
                banner = "✅ CSV guardado"
            } else {
                // Cancelado o error -> fallback para copiar
                banner = msg ?: "⚠️ No se pudo guardar. Puedes copiar el CSV."
                showExportCsv = true
            }
        }
    }

    AppTheme(p) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 900.dp

            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (!isWide) {
                        BottomTabs(
                            selected = tab,
                            onSelect = { tabKey = it.name },
                            surface = p.surface0,
                            stroke = p.strokeSoft,
                            textSoft = p.textSoft,
                            textStrong = p.textStrong,
                            brand = p.brand
                        )
                    }
                }
            ) { pad ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)
                        )
                        .padding(pad),
                    color = Color.Gray
                ) {
                    val outerPad = 6.dp
                    val innerPadH = 8.dp
                    val innerPadV = 8.dp
                    val sectionGap = 8.dp

                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(outerPad),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LeftRail(
                                selected = tab,
                                onSelect = { tabKey = it.name },
                                surface = p.surface0,
                                strokeSoft = p.strokeSoft,
                                textSoft = p.textSoft,
                                textStrong = p.textStrong,
                                brand = p.brand
                            )

                            MainCard(
                                modifier = Modifier.weight(1f),
                                p = p,
                                sectionGap = sectionGap,
                                innerPadH = innerPadH,
                                innerPadV = innerPadV,
                                marketState = marketState,
                                portfolioState = portfolioState,
                                tab = tab,
                                featured = featured,
                                selectedTicker = selectedTicker,
                                onSelectTicker = { selectedTicker = it },
                                showFeatured = showFeatured,
                                onToggleFeatured = { showFeatured = !showFeatured },
                                showNewsExpanded = showNewsExpanded,
                                onToggleNews = { showNewsExpanded = !showNewsExpanded },
                                showPortfolioExpanded = showPortfolioExpanded,
                                onTogglePortfolio = { showPortfolioExpanded = !showPortfolioExpanded },
                                pctColor = { pctColor(it) },
                                arrow = { arrow(it) },
                                onToggleOpen = safeToggleOpen,
                                onTogglePause = safeTogglePause,
                                onSetSpeed = { engine.setSimSpeed(it) },
                                canTrade = canTrade,
                                onBuy = { t -> if (canTrade) portfolioVm.openTrade(t, PortfolioViewModel.Mode.BUY) },
                                onSell = { t -> if (canTrade) portfolioVm.openTrade(t, PortfolioViewModel.Mode.SELL) },
                                priceHistory = priceHistory,
                                valueHistory = valueHistory,
                                alertRules = alertsState.rules,
                                triggeredAlerts = alertsState.triggered,
                                banner = banner,
                                onDismissBanner = { banner = null },
                                onCreateAlert = { showCreateAlert = true },
                                onUpsertAlert = { rule -> appScope.launch { alertsRepo.upsertRule(rule) } },
                                onDeleteAlert = { id -> appScope.launch { alertsRepo.removeRule(id) } },
                                onOpenStrategies = { showStrategiesDialog = true },
                                onExportPortfolioCsv = onExportCsv,
                                statistics = statistics
                            )
                        }
                    } else {
                        MainCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(outerPad),
                            p = p,
                            sectionGap = sectionGap,
                            innerPadH = innerPadH,
                            innerPadV = innerPadV,
                            marketState = marketState,
                            portfolioState = portfolioState,
                            tab = tab,
                            featured = featured,
                            selectedTicker = selectedTicker,
                            onSelectTicker = { selectedTicker = it },
                            showFeatured = showFeatured,
                            onToggleFeatured = { showFeatured = !showFeatured },
                            showNewsExpanded = showNewsExpanded,
                            onToggleNews = { showNewsExpanded = !showNewsExpanded },
                            showPortfolioExpanded = showPortfolioExpanded,
                            onTogglePortfolio = { showPortfolioExpanded = !showPortfolioExpanded },
                            pctColor = { pctColor(it) },
                            arrow = { arrow(it) },
                            onToggleOpen = safeToggleOpen,
                            onTogglePause = safeTogglePause,
                            onSetSpeed = { engine.setSimSpeed(it) },
                            canTrade = canTrade,
                            onBuy = { t -> if (canTrade) portfolioVm.openTrade(t, PortfolioViewModel.Mode.BUY) },
                            onSell = { t -> if (canTrade) portfolioVm.openTrade(t, PortfolioViewModel.Mode.SELL) },
                            priceHistory = priceHistory,
                            valueHistory = valueHistory,
                            alertRules = alertsState.rules,
                            triggeredAlerts = alertsState.triggered,
                            banner = banner,
                            onDismissBanner = { banner = null },
                            onCreateAlert = { showCreateAlert = true },
                            onUpsertAlert = { rule -> appScope.launch { alertsRepo.upsertRule(rule) } },
                            onDeleteAlert = { id -> appScope.launch { alertsRepo.removeRule(id) } },
                            onOpenStrategies = { showStrategiesDialog = true },
                            onExportPortfolioCsv = onExportCsv,
                            statistics = statistics
                        )
                    }

                    TradeDialog(
                        vm = portfolioVm,
                        dialogSurface = p.surface0,
                        innerSurface = p.surface1,
                        stroke = p.stroke,
                        textStrong = p.textStrong,
                        textSoft = p.textSoft,
                        textMuted = p.textMuted,
                        success = p.success,
                        danger = p.danger,
                        neutral = p.neutral
                    )

                    if (showCreateAlert) {
                        CreateAlertDialog(
                            defaultTicker = selectedTicker.ifBlank { marketState.stocks.firstOrNull()?.ticker.orEmpty() },
                            tickers = marketState.stocks.map { it.ticker },
                            surface = p.surface0,
                            stroke = p.stroke,
                            textStrong = p.textStrong,
                            textSoft = p.textSoft,
                            neutral = p.neutral,
                            brand = p.brand,
                            onDismiss = { showCreateAlert = false },
                            onCreate = { rule ->
                                appScope.launch { alertsRepo.upsertRule(rule) }
                                showCreateAlert = false
                                banner = "✅ Alerta creada: ${rule.ticker} · ${alertUiLabel(rule)} ${fmt2(alertUiThreshold(rule))}"
                            }
                        )
                    }

                    if (showStrategiesDialog) {
                        StrategiesConfigDialog(
                            strategiesRepo = strategiesRepo,
                            tickers = marketState.stocks.map { it.ticker },
                            initialTicker = selectedTicker.ifBlank { marketState.stocks.firstOrNull()?.ticker.orEmpty() },
                            onClose = { showStrategiesDialog = false }
                        )
                    }

                    // ✅ Fallback: diálogo para copiar el CSV si el guardado se cancela/falla
                    if (showExportCsv) {
                        AlertDialog(
                            onDismissRequest = { showExportCsv = false },
                            title = { Text("Export Portfolio a CSV") },
                            text = {
                                OutlinedTextField(
                                    value = csvText,
                                    onValueChange = { csvText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 220.dp, max = 360.dp),
                                    label = { Text("CSV") },
                                    minLines = 10
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(csvText))
                                        banner = "✅ CSV copiado al portapapeles"
                                        showExportCsv = false
                                    }
                                ) { Text("Copiar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExportCsv = false }) { Text("Cerrar") }
                            }
                        )
                    }
                }
            }
        }
    }
}
