package org.example.project.presentation.strategies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.example.project.domain.strategy.DipReference
import org.example.project.domain.strategy.StrategiesRepository
import org.example.project.domain.strategy.StrategyRule
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height



@Composable
fun StrategiesConfigDialog(
    strategiesRepo: StrategiesRepository,
    tickers: List<String>,
    initialTicker: String,
    onClose: () -> Unit,
    // puedes ajustar colores a tu paleta si quieres
    surface: Color = Color(0xFF0B1220),
    stroke: Color = Color(0xFF22314D),
    textStrong: Color = Color(0xFFEAF1FF),
    textSoft: Color = Color(0xFFB9C5DD),
    brand: Color = Color(0xFF4EA1FF),
    success: Color = Color(0xFF34D399),
    danger: Color = Color(0xFFFB7185),
) {
    val rules by strategiesRepo.rules.collectAsState()

    // Selector interno de ticker
    var ticker by remember { mutableStateOf(initialTicker.trim().uppercase()) }
    var tickerMenu by remember { mutableStateOf(false) }

    fun findAutoBuy(t: String) = rules.filterIsInstance<StrategyRule.AutoBuyDip>()
        .firstOrNull { it.ticker?.equals(t, ignoreCase = true) == true }

    fun findTakeProfit(t: String) = rules.filterIsInstance<StrategyRule.TakeProfit>()
        .firstOrNull { it.ticker?.equals(t, ignoreCase = true) == true }

    fun findStopLoss(t: String) = rules.filterIsInstance<StrategyRule.StopLoss>()
        .firstOrNull { it.ticker?.equals(t, ignoreCase = true) == true }

    val autoBuy = findAutoBuy(ticker)
    val takeProfit = findTakeProfit(ticker)
    val stopLoss = findStopLoss(ticker)

    // Cuando cambias ticker, recargamos valores de ese ticker
    var buyDipPct by remember(ticker) { mutableStateOf(autoBuy?.dropPercent ?: 2.0) }
    var takeProfitPct by remember(ticker) { mutableStateOf(takeProfit?.profitPercent ?: 3.0) }
    var stopLossPct by remember(ticker) { mutableStateOf(stopLoss?.lossPercent ?: 3.0) }

    // Valores base (los que había al abrir / al cambiar ticker)
    val originalBuyDip = autoBuy?.dropPercent ?: 2.0
    val originalTakeProfit = takeProfit?.profitPercent ?: 3.0
    val originalStopLoss = stopLoss?.lossPercent ?: 3.0

    val hasUnsavedChanges =
        (abs(buyDipPct - originalBuyDip) > 0.0001) ||
                (abs(takeProfitPct - originalTakeProfit) > 0.0001) ||
                (abs(stopLossPct - originalStopLoss) > 0.0001)

    // IDs deterministas por ticker+tipo
    fun idFor(kind: Int): Int = (ticker.hashCode() * 10) + kind
    val idAutoBuy = autoBuy?.id ?: idFor(1)
    val idTakeProfit = takeProfit?.id ?: idFor(2)
    val idStopLoss = stopLoss?.id ?: idFor(3)

    val dialogShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(12.dp)

    fun fmtPct(x: Double): String = "${"%.2f".format(x)}%"

    // Ejemplos simples con base 100€
    fun exampleBuy(pct: Double): String {
        val from = 100.0
        val to = from * (1.0 - pct / 100.0)
        return "Ejemplo: si estaba a 100 €, comprará en ~${to.roundToInt()} €."
    }

    fun exampleTakeProfit(pct: Double): String {
        val from = 100.0
        val to = from * (1.0 + pct / 100.0)
        return "Ejemplo: si compraste a 100 €, venderá en ~${to.roundToInt()} €."
    }

    fun exampleStopLoss(pct: Double): String {
        val from = 100.0
        val to = from * (1.0 - pct / 100.0)
        return "Ejemplo: si compraste a 100 €, venderá en ~${to.roundToInt()} €."
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = dialogShape,
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)   // ✅ CLAVE: limita altura del diálogo
                .widthIn(max = 540.dp)
            // Alternativa si prefieres fijo:
            // .heightIn(max = 650.dp)
        ) {
            val scroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .border(1.dp, stroke, dialogShape)
                    .padding(14.dp)
            ) {
                // ✅ CONTENIDO CON SCROLL (ocupa el hueco disponible, sin expulsar footer)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)   // ✅ CLAVE
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Text(
                        text = "Estrategias automáticas",
                        color = textStrong,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    // ✅ Resumen: deja claro si está "pendiente" o "actual"
                    Text(
                        text = if (hasUnsavedChanges) {
                            "Configuración pendiente: Compra si cae ${fmtPct(buyDipPct)} · " +
                                    "Vende con +${fmtPct(takeProfitPct)} · " +
                                    "Protege con -${fmtPct(stopLossPct)}"
                        } else {
                            "Configuración actual: Compra si cae ${fmtPct(buyDipPct)} · " +
                                    "Vende con +${fmtPct(takeProfitPct)} · " +
                                    "Protege con -${fmtPct(stopLossPct)}"
                        },
                        color = textSoft,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Selector de ticker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Empresa (ticker)", color = textSoft, style = MaterialTheme.typography.labelSmall)

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, stroke, innerShape)
                                    .clickable { tickerMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    ticker.ifBlank { "Selecciona..." },
                                    color = textStrong,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("▾", color = textSoft)
                            }

                            DropdownMenu(
                                expanded = tickerMenu,
                                onDismissRequest = { tickerMenu = false }
                            ) {
                                tickers
                                    .map { it.trim().uppercase() }
                                    .distinct()
                                    .sorted()
                                    .forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = { ticker = t; tickerMenu = false }
                                        )
                                    }
                            }
                        }
                    }

                    // ==========================
                    // 1) Compra en caídas
                    // ==========================
                    SectionCard(
                        title = "Compra automática en caídas",
                        subtitle = "Compra ${ticker} cuando baje un porcentaje mínimo.",
                        accent = brand,
                        surface = surface,
                        stroke = stroke,
                        textStrong = textStrong,
                        textSoft = textSoft
                    ) {
                        Text(
                            text = "¿Cuánto debe caer para comprar?",
                            color = textSoft,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = buyDipPct.toFloat(),
                            onValueChange = { buyDipPct = it.toDouble() },
                            valueRange = 0.1f..10f
                        )
                        Text(
                            "Caída mínima: ${fmtPct(buyDipPct)}",
                            color = textStrong,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(exampleBuy(buyDipPct), color = textSoft, style = MaterialTheme.typography.bodySmall)
                    }

                    // ==========================
                    // 2) Venta con beneficio
                    // ==========================
                    SectionCard(
                        title = "Venta automática con beneficio",
                        subtitle = "Vende cuando estés ganando lo suficiente.",
                        accent = success,
                        surface = surface,
                        stroke = stroke,
                        textStrong = textStrong,
                        textSoft = textSoft
                    ) {
                        Text(
                            text = "¿Con qué beneficio quieres vender?",
                            color = textSoft,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = takeProfitPct.toFloat(),
                            onValueChange = { takeProfitPct = it.toDouble() },
                            valueRange = 0.1f..10f
                        )
                        Text(
                            "Objetivo de beneficio: +${fmtPct(takeProfitPct)}",
                            color = textStrong,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(exampleTakeProfit(takeProfitPct), color = textSoft, style = MaterialTheme.typography.bodySmall)
                    }

                    // ==========================
                    // 3) Stop-loss
                    // ==========================
                    SectionCard(
                        title = "Protección contra pérdidas (Stop-Loss)",
                        subtitle = "Vende si el precio cae demasiado para limitar pérdidas.",
                        accent = danger,
                        surface = surface,
                        stroke = stroke,
                        textStrong = textStrong,
                        textSoft = textSoft
                    ) {
                        Text(
                            text = "¿Cuánta pérdida máxima aceptas?",
                            color = textSoft,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = stopLossPct.toFloat(),
                            onValueChange = { stopLossPct = it.toDouble() },
                            valueRange = 0.1f..10f
                        )
                        Text(
                            "Límite de pérdida: -${fmtPct(stopLossPct)}",
                            color = textStrong,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(exampleStopLoss(stopLossPct), color = textSoft, style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = "Los cambios no se aplicarán hasta que pulses «Aplicar».",
                        color = textSoft.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // ✅ FOOTER FIJO (siempre visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar", color = textSoft) }

                    Button(
                        onClick = {
                            if (ticker.isBlank()) return@Button

                            strategiesRepo.upsert(
                                StrategyRule.AutoBuyDip(
                                    id = idAutoBuy,
                                    ticker = ticker,
                                    dropPercent = buyDipPct,
                                    reference = DipReference.OPEN,
                                    budgetEuro = autoBuy?.budgetEuro ?: 250.0,
                                    cooldownMs = autoBuy?.cooldownMs ?: 12_000L
                                )
                            )
                            strategiesRepo.upsert(
                                StrategyRule.TakeProfit(
                                    id = idTakeProfit,
                                    ticker = ticker,
                                    profitPercent = takeProfitPct,
                                    sellFraction = takeProfit?.sellFraction ?: 1.0,
                                    cooldownMs = takeProfit?.cooldownMs ?: 12_000L
                                )
                            )
                            strategiesRepo.upsert(
                                StrategyRule.StopLoss(
                                    id = idStopLoss,
                                    ticker = ticker,
                                    lossPercent = stopLossPct,
                                    sellFraction = stopLoss?.sellFraction ?: 1.0,
                                    cooldownMs = stopLoss?.cooldownMs ?: 12_000L
                                )
                            )

                            onClose()
                        },
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUnsavedChanges) brand else brand.copy(alpha = 0.35f),
                            contentColor = Color(0xFF001018),
                            disabledContainerColor = brand.copy(alpha = 0.35f),
                            disabledContentColor = Color(0xFF001018).copy(alpha = 0.65f)
                        )
                    ) {
                        Text(if (hasUnsavedChanges) "Aplicar cambios" else "Sin cambios", maxLines = 1)
                    }
                }
            }
        }
    }

}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    accent: Color,
    surface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, stroke, shape)
            .background(surface.copy(alpha = 0.80f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = textStrong, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = textSoft, style = MaterialTheme.typography.bodySmall)

        // pequeña línea de acento (visual semántico)
        Spacer(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(vertical = 2.dp)
        )

        content()
    }
}
