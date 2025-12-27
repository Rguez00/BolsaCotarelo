package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.project.core.config.InitialData
import org.example.project.data.repository.InMemoryAlertsRepository
import org.example.project.data.repository.InMemoryMarketRepository
import org.example.project.data.repository.InMemoryPortfolioRepository
import org.example.project.domain.model.StockSnapshot
import org.example.project.domain.model.calculateStatistics
import org.example.project.domain.strategy.DipReference
import org.example.project.domain.strategy.InMemoryStrategiesRepository
import org.example.project.domain.strategy.StrategyRule
import org.example.project.engine.MarketEngine
import org.example.project.platform.rememberAlertNotifier
import org.example.project.platform.rememberPortfolioJsonFileIO
import org.example.project.presentation.strategies.StrategiesConfigDialog
import org.example.project.presentation.ui.PortfolioStateMenuButton
import org.example.project.presentation.ui.TradeDialog
import org.example.project.presentation.vm.PortfolioViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val appScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val jsonStore = rememberJsonStore("portfolio.json")
    val jsonFileIO = rememberPortfolioJsonFileIO()

    var hasLoaded by remember { mutableStateOf(false) }
    var lastWritten by remember { mutableStateOf<String?>(null) }

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

    suspend fun persistNow() {
        if (!hasLoaded) return

        val text = portfolioRepo.exportStateJson().getOrNull() ?: return
        if (text != lastWritten) {
            jsonStore.write(text)
            lastWritten = text
        }
    }

    // ✅ LOAD (usa el contrato importStateJson)
    LaunchedEffect(Unit) {
        val raw = runCatching { jsonStore.read() }.getOrNull()

        if (!raw.isNullOrBlank()) {
            portfolioRepo.importStateJson(raw)
                .onFailure { it.printStackTrace() }
            lastWritten = raw
        }

        hasLoaded = true
    }

    // ✅ AUTO-SAVE: cada cambio de estado => persistencia en IO
    LaunchedEffect(Unit) {
        snapshotFlow { portfolioRepo.portfolioState.value }
            .collectLatest {
                if (!hasLoaded) return@collectLatest
                appScope.launch(Dispatchers.IO) {
                    runCatching { persistNow() }
                        .onFailure { it.printStackTrace() }
                }
            }
    }

    // ✅ Guardar al ir a background (Android)
    PlatformSaveOnStop(enabled = hasLoaded) {
        persistNow()
    }

    // ✅ Reglas por defecto
    LaunchedEffect(Unit) {
        strategiesRepo.upsert(
            StrategyRule.AutoBuyDip(
                id = 1,
                ticker = "NBS",
                dropPercent = 2.0,
                reference = DipReference.OPEN,
                budgetEuro = 250.0,
                cooldownMs = 12_000L
            )
        )
        strategiesRepo.upsert(
            StrategyRule.TakeProfit(
                id = 2,
                ticker = "NBS",
                profitPercent = 3.0,
                sellFraction = 1.0,
                cooldownMs = 12_000L
            )
        )
        strategiesRepo.upsert(
            StrategyRule.StopLoss(
                id = 3,
                ticker = "NBS",
                lossPercent = 3.0,
                sellFraction = 1.0,
                cooldownMs = 12_000L
            )
        )
    }

    val portfolioVm = remember {
        PortfolioViewModel(
            repo = portfolioRepo,
            canTradeProvider = { engine.marketState.value.isOpen && !engine.marketState.value.isPaused }
        )
    }

    LaunchedEffect(Unit) {
        engine.start()  // ← ESTO es lo que falta
    }
    // ✅ Close (best-effort)
    DisposableEffect(Unit) {
        onDispose {
            runBlocking {
                runCatching { persistNow() }
                    .onFailure { it.printStackTrace() }
            }
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

    // UI STATE
    var tabKey by rememberSaveable { mutableStateOf(AppTab.MARKET.name) }
    val tab = AppTab.valueOf(tabKey)

    var showStrategiesDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTicker by rememberSaveable { mutableStateOf("NBS") }

    var showFeatured by rememberSaveable { mutableStateOf(false) }
    var showNewsExpanded by rememberSaveable { mutableStateOf(false) }
    var showPortfolioExpanded by rememberSaveable { mutableStateOf(true) }

    var showCreateAlert by rememberSaveable { mutableStateOf(false) }
    var banner by rememberSaveable { mutableStateOf<String?>(null) }

    var showExportCsv by rememberSaveable { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") }

    // ✅ JSON menu dialogs
    var showExportJson by rememberSaveable { mutableStateOf(false) }
    var showImportJson by rememberSaveable { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }

    val notifier = rememberAlertNotifier() // <-- crea este expect/actual o tu wrapper android

    LaunchedEffect(alertsState.triggered.size) {
        val last = alertsState.triggered.lastOrNull() ?: return@LaunchedEffect
        banner = last.message

        // ✅ dispara notificación del sistema
        notifier.notifyPriceAlert(
            title = "Alerta de precio",
            message = last.message
        )

        // opcional
        notifier.beep()
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

    val onExportCsv: () -> Unit = {
        appScope.launch {
            val text = runCatching { portfolioRepo.exportTransactionsCsv() }
                .getOrElse { e ->
                    banner = "⚠️ Error generando CSV: ${e.message ?: "desconocido"}"
                    return@launch
                }

            csvText = text

            val rawTs = kotlinx.datetime.Clock.System.now().toString()
            val safeTs = rawTs.replace(":", "-").replace(".", "-").replace("Z", "")
            val fileName = "transactions_$safeTs.csv"

            csvSaver.saveCsv(fileName, text) { ok, msg ->
                if (ok) banner = "✅ CSV de transacciones guardado"
                else {
                    banner = msg ?: "⚠️ No se pudo guardar. Puedes copiar el CSV."
                    showExportCsv = true
                }
            }
        }
    }

    // ✅ Menu actions (JSON portfolio)
    val onSavePortfolioNow: () -> Unit = {
        appScope.launch(Dispatchers.IO) {
            runCatching { persistNow() }
                .onSuccess { banner = "✅ Portfolio guardado" }
                .onFailure { e -> banner = "⚠️ Error al guardar: ${e.message ?: "desconocido"}" }
        }
    }

    val onExportPortfolioJson: () -> Unit = {
        appScope.launch {
            val text = portfolioRepo.exportStateJson()
                .getOrElse { e ->
                    banner = "⚠️ Error exportando JSON: ${e.message ?: "desconocido"}"
                    return@launch
                }
            jsonText = text
            showExportJson = true
        }
    }

    val onImportPortfolioJson: () -> Unit = {
        jsonText = ""
        showImportJson = true
    }

    val onResetPortfolio: () -> Unit = {
        appScope.launch {
            portfolioRepo.clearState()
                .onSuccess {
                    banner = "✅ Portfolio reseteado"
                    appScope.launch(Dispatchers.IO) { runCatching { persistNow() } }
                }
                .onFailure { e ->
                    banner = "⚠️ Error al resetear: ${e.message ?: "desconocido"}"
                }
        }
    }

    // ✅ Guardar como archivo (JSON)
    val onSaveAsJsonFile: () -> Unit = {
        appScope.launch {
            val text = portfolioRepo.exportStateJson()
                .getOrElse { e ->
                    banner = "⚠️ Error exportando JSON: ${e.message ?: "desconocido"}"
                    return@launch
                }

            val rawTs = kotlinx.datetime.Clock.System.now().toString()
            val safeTs = rawTs.replace(":", "-").replace(".", "-").replace("Z", "")
            val fileName = "portfolio_$safeTs.json"

            jsonFileIO.saveJson(fileName, text) { ok, msg ->
                banner = if (ok) "✅ Portfolio guardado en archivo"
                else msg ?: "⚠️ No se pudo guardar"
            }
        }
    }

    // ✅ Abrir archivo (JSON)
    val onOpenJsonFile: () -> Unit = {
        jsonFileIO.openJson { ok, json, msg ->
            if (!ok || json.isNullOrBlank()) {
                banner = msg ?: "⚠️ No se pudo abrir"
                return@openJson
            }

            appScope.launch {
                portfolioRepo.importStateJson(json)
                    .onSuccess {
                        banner = "✅ Portfolio cargado desde archivo"
                        appScope.launch(Dispatchers.IO) { runCatching { persistNow() } }
                    }
                    .onFailure { e ->
                        banner = "⚠️ JSON inválido: ${e.message ?: "desconocido"}"
                    }
            }
        }
    }

    AppTheme(p) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 900.dp

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text("BolsaCotarelo") },
                        actions = {
                            PortfolioStateMenuButton(
                                onSaveNow = onSavePortfolioNow,
                                onExportJson = onExportPortfolioJson,
                                onImportJson = onImportPortfolioJson,
                                onReset = onResetPortfolio,
                                onSaveAsJsonFile = onSaveAsJsonFile,
                                onOpenJsonFile = onOpenJsonFile
                            )
                        }
                    )
                },
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
                            modifier = Modifier.fillMaxSize().padding(outerPad),
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
                            modifier = Modifier.fillMaxSize().padding(outerPad),
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

                    // ✅ ✅ ✅ ESTE ES EL CAMBIO QUE FALTABA
                    if (showCreateAlert) {
                        CreateAlertDialog(
                            defaultTicker = selectedTicker,
                            tickers = marketState.stocks.map { it.ticker },

                            // ✅ estilos que te está pidiendo
                            surface = p.surface0,
                            stroke = p.stroke,
                            textStrong = p.textStrong,
                            textSoft = p.textSoft,
                            neutral = p.neutral,
                            brand = p.brand,

                            onDismiss = { showCreateAlert = false },
                            onCreate = { rule ->
                                appScope.launch {
                                    runCatching { alertsRepo.upsertRule(rule) }
                                        .onSuccess {
                                            banner = "✅ Alerta creada"
                                            showCreateAlert = false
                                        }
                                        .onFailure { e ->
                                            banner = "⚠️ No se pudo crear la alerta: ${e.message ?: "desconocido"}"
                                        }
                                }
                            }
                        )
                    }

                    // ✅ ✅ ✅ FIN DEL CAMBIO

                    if (showStrategiesDialog) {
                        StrategiesConfigDialog(
                            strategiesRepo = strategiesRepo,
                            tickers = marketState.stocks.map { it.ticker },
                            initialTicker = selectedTicker.ifBlank { marketState.stocks.firstOrNull()?.ticker.orEmpty() },
                            onClose = { showStrategiesDialog = false }
                        )
                    }

                    if (showExportCsv) {
                        AlertDialog(
                            onDismissRequest = { showExportCsv = false },
                            title = { Text("Exportar transacciones a CSV") },
                            text = {
                                OutlinedTextField(
                                    value = csvText,
                                    onValueChange = { csvText = it },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 360.dp),
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
                            dismissButton = { TextButton(onClick = { showExportCsv = false }) { Text("Cerrar") } }
                        )
                    }

                    if (showExportJson) {
                        AlertDialog(
                            onDismissRequest = { showExportJson = false },
                            title = { Text("Exportar portfolio (JSON)") },
                            text = {
                                OutlinedTextField(
                                    value = jsonText,
                                    onValueChange = { jsonText = it },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                                    label = { Text("JSON") },
                                    minLines = 10
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(jsonText))
                                        banner = "✅ JSON copiado al portapapeles"
                                        showExportJson = false
                                    }
                                ) { Text("Copiar") }
                            },
                            dismissButton = { TextButton(onClick = { showExportJson = false }) { Text("Cerrar") } }
                        )
                    }

                    if (showImportJson) {
                        AlertDialog(
                            onDismissRequest = { showImportJson = false },
                            title = { Text("Importar portfolio (JSON)") },
                            text = {
                                OutlinedTextField(
                                    value = jsonText,
                                    onValueChange = { jsonText = it },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                                    label = { Text("Pega aquí el JSON") },
                                    minLines = 10
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        appScope.launch {
                                            val raw = jsonText.trim()
                                            if (raw.isBlank()) {
                                                banner = "⚠️ El JSON está vacío"
                                                return@launch
                                            }

                                            portfolioRepo.importStateJson(raw)
                                                .onSuccess {
                                                    banner = "✅ Portfolio cargado"
                                                    appScope.launch(Dispatchers.IO) { runCatching { persistNow() } }
                                                    showImportJson = false
                                                }
                                                .onFailure { e ->
                                                    banner = "⚠️ JSON inválido: ${e.message ?: "desconocido"}"
                                                }
                                        }
                                    }
                                ) { Text("Cargar") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showImportJson = false }) { Text("Cancelar") }
                            }
                        )
                    }
                }
            }
        }
    }
}
