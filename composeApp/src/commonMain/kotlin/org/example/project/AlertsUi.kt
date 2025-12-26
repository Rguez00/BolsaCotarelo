package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.example.project.core.util.fmt2
import org.example.project.domain.model.AlertRule
import org.example.project.domain.model.AlertTriggered
import org.example.project.domain.model.StockSnapshot

// ==============================
// Helpers UI (labels + threshold)
// ==============================
internal fun alertUiLabel(r: AlertRule): String = when (r) {
    is AlertRule.PriceAbove -> "Precio ≥"
    is AlertRule.PriceBelow -> "Precio ≤"
    is AlertRule.PercentChangeAbove -> "% ≥"
    is AlertRule.PercentChangeBelow -> "% ≤"
}

internal fun alertUiThreshold(r: AlertRule): Double = when (r) {
    is AlertRule.PriceAbove -> r.threshold
    is AlertRule.PriceBelow -> r.threshold
    is AlertRule.PercentChangeAbove -> r.thresholdPercent
    is AlertRule.PercentChangeBelow -> r.thresholdPercent
}

private fun toggleEnabled(r: AlertRule): AlertRule = when (r) {
    is AlertRule.PriceAbove -> r.copy(enabled = !r.enabled)
    is AlertRule.PriceBelow -> r.copy(enabled = !r.enabled)
    is AlertRule.PercentChangeAbove -> r.copy(enabled = !r.enabled)
    is AlertRule.PercentChangeBelow -> r.copy(enabled = !r.enabled)
}

private fun resetRule(r: AlertRule): AlertRule = when (r) {
    is AlertRule.PriceAbove -> r.copy(enabled = true, triggered = false, triggeredAtMillis = null)
    is AlertRule.PriceBelow -> r.copy(enabled = true, triggered = false, triggeredAtMillis = null)
    is AlertRule.PercentChangeAbove -> r.copy(enabled = true, triggered = false, triggeredAtMillis = null)
    is AlertRule.PercentChangeBelow -> r.copy(enabled = true, triggered = false, triggeredAtMillis = null)
}

private fun normalizeTicker(raw: String): String = raw.trim().uppercase()

// ==============================
// ALERTS PANEL
// ==============================
@Composable
internal fun AlertsPanel(
    marketStocks: List<StockSnapshot>,
    canTrade: Boolean,
    alertRules: List<AlertRule>,
    triggeredAlerts: List<AlertTriggered>,
    banner: String?,
    onDismissBanner: () -> Unit,
    onCreate: () -> Unit,
    onUpsert: (AlertRule) -> Unit,
    onDelete: (Long) -> Unit,
    surface: Color,
    inner: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    textMuted: Color,
    brand: Color,
    neutral: Color,
    success: Color,
    danger: Color
) {
    val shape = RoundedCornerShape(16.dp)

    // Normalizamos la key para que "abc" y "ABC" coincidan.
    val byTicker = remember(marketStocks) {
        marketStocks.associateBy { normalizeTicker(it.ticker) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, shape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Alertas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textStrong,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(brand.copy(alpha = 0.14f))
                        .border(1.dp, brand.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (canTrade) "Mercado OK" else "Mercado pausado/cerrado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (canTrade) success else neutral,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = banner != null) {
                val b = banner.orEmpty()
                val bShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bShape)
                        .background(brand.copy(alpha = 0.12f))
                        .border(1.dp, brand.copy(alpha = 0.25f), bShape)
                        .clickable { onDismissBanner() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = b,
                        color = textStrong,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCreate,
                    modifier = Modifier.weight(1f)
                ) { Text("Nueva alerta") }

                val active = alertRules.count { it.enabled && !it.triggered }
                val trig = alertRules.count { it.triggered }
                MiniStatPill("Activas", "$active", inner, stroke, textMuted, textStrong, Modifier.weight(1f))
                MiniStatPill("Disparadas", "$trig", inner, stroke, textMuted, textStrong, Modifier.weight(1f))
            }

            Divider(color = stroke)

            Text(
                "Reglas",
                color = textSoft,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (alertRules.isEmpty()) {
                Text(
                    "No hay alertas. Crea una para probar 🔔",
                    color = textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alertRules
                        .sortedWith(compareBy<AlertRule> { it.triggered }.thenByDescending { it.id })
                        .forEach { a ->
                            val s = byTicker[normalizeTicker(a.ticker)]

                            val currentLabel = when (a) {
                                is AlertRule.PriceAbove, is AlertRule.PriceBelow ->
                                    s?.currentPrice?.let { fmt2(it) } ?: "--"

                                is AlertRule.PercentChangeAbove, is AlertRule.PercentChangeBelow ->
                                    s?.changePercent?.let { "${fmt2(it)}%" } ?: "--"
                            }

                            val stateColor = when {
                                a.triggered -> danger
                                a.enabled -> success
                                else -> neutral
                            }

                            AlertRow(
                                a = a,
                                current = currentLabel,
                                stateColor = stateColor,
                                surface = inner,
                                stroke = stroke,
                                textStrong = textStrong,
                                textSoft = textSoft,
                                textMuted = textMuted,
                                onToggle = { if (!a.triggered) onUpsert(toggleEnabled(a)) },
                                onReset = { onUpsert(resetRule(a)) },
                                onDelete = { onDelete(a.id) }
                            )
                        }
                }
            }

            Divider(color = stroke)

            Text(
                "Historial",
                color = textSoft,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (triggeredAlerts.isEmpty()) {
                Text(
                    "Aún no se ha disparado ninguna alerta.",
                    color = textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    triggeredAlerts.takeLast(8).reversed().forEach { f ->
                        FiredRow(
                            f = f,
                            surface = inner,
                            stroke = stroke,
                            textStrong = textStrong,
                            textSoft = textSoft,
                            neutral = neutral
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatPill(
    title: String,
    value: String,
    surface: Color,
    stroke: Color,
    textMuted: Color,
    textStrong: Color,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(surface)
            .border(1.dp, stroke, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = textMuted, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, color = textStrong, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun AlertRow(
    a: AlertRule,
    current: String,
    stateColor: Color,
    surface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    textMuted: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val status = when {
        a.triggered -> "DISPARADA"
        a.enabled -> "ACTIVA"
        else -> "PAUSADA"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, stroke, shape)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(stateColor.copy(alpha = 0.95f))
                )
                Spacer(Modifier.width(10.dp))

                Text(
                    text = "${a.ticker} · ${alertUiLabel(a)} ${fmt2(alertUiThreshold(a))}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textStrong,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(stateColor.copy(alpha = 0.14f))
                        .border(1.dp, stateColor.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable { if (a.triggered) onReset() else onToggle() }
                ) {
                    Text(
                        text = if (a.triggered) "RESET" else status,
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "✖",
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onDelete() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    color = textMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "Actual: $current  ·  Estado: $status",
                style = MaterialTheme.typography.labelSmall,
                color = textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (a.triggered && a.triggeredAtMillis != null) {
                Text(
                    text = "Disparada (ms): ${a.triggeredAtMillis}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textMuted
                )
            }
        }
    }
}

@Composable
private fun FiredRow(
    f: AlertTriggered,
    surface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    neutral: Color
) {
    val shape = RoundedCornerShape(12.dp)

    val valueAtFire = when (f.rule) {
        is AlertRule.PriceAbove, is AlertRule.PriceBelow -> f.currentPrice
        is AlertRule.PercentChangeAbove, is AlertRule.PercentChangeBelow -> f.currentChangePercent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, stroke, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "🔔 ${f.rule.ticker} · ${alertUiLabel(f.rule)} ${fmt2(alertUiThreshold(f.rule))}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Valor al disparar: ${fmt2(valueAtFire)} · ${f.timestamp.toString().take(19)}",
                style = MaterialTheme.typography.labelSmall,
                color = textSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==============================
// CREATE ALERT DIALOG
// ==============================
private enum class AlertUiType(val label: String) {
    PRICE_ABOVE("Precio ≥"),
    PRICE_BELOW("Precio ≤"),
    PCT_ABOVE("% ≥"),
    PCT_BELOW("% ≤")
}

@Composable
internal fun CreateAlertDialog(
    defaultTicker: String,
    tickers: List<String>,
    surface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    neutral: Color,
    brand: Color,
    onDismiss: () -> Unit,
    onCreate: (AlertRule) -> Unit
) {
    var ticker by remember { mutableStateOf(defaultTicker.ifBlank { tickers.firstOrNull().orEmpty() }) }
    var type by remember { mutableStateOf(AlertUiType.PRICE_ABOVE) }
    var thresholdText by remember { mutableStateOf("100") }
    var error by remember { mutableStateOf<String?>(null) }

    fun parseDoubleOrNull(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

    val dialogShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(14.dp)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = dialogShape,
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, stroke, dialogShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nueva alerta",
                    color = textStrong,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text("Ticker", color = textSoft, style = MaterialTheme.typography.labelSmall)

                if (tickers.isEmpty()) {
                    Text("No hay tickers.", color = neutral, style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tickers.take(8).forEach { t ->
                            val sel = t == ticker
                            val bg = if (sel) brand.copy(alpha = 0.16f) else Color.Transparent
                            val br = if (sel) brand.copy(alpha = 0.35f) else stroke
                            val fg = if (sel) Color(0xFFEAF1FF) else textSoft

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(bg)
                                    .border(1.dp, br, RoundedCornerShape(999.dp))
                                    .clickable { ticker = t; error = null }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(t, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Text("Tipo", color = textSoft, style = MaterialTheme.typography.labelSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // entries (Kotlin moderno)
                    AlertUiType.entries.forEach { t ->
                        val sel = t == type
                        val bg = if (sel) brand.copy(alpha = 0.16f) else Color.Transparent
                        val br = if (sel) brand.copy(alpha = 0.35f) else stroke
                        val fg = if (sel) Color(0xFFEAF1FF) else textSoft

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(bg)
                                .border(1.dp, br, RoundedCornerShape(999.dp))
                                .clickable { type = t; error = null }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(t.label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it; error = null },
                    label = { Text("Umbral (ej: 105.5 ó 0.8)", color = textSoft) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = innerShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brand.copy(alpha = 0.55f),
                        unfocusedBorderColor = stroke,
                        cursorColor = brand,
                        focusedLabelColor = brand,
                        unfocusedLabelColor = textSoft
                    )
                )

                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = Color(0xFFFB7185),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar", color = textSoft)
                    }

                    Button(
                        onClick = {
                            val th = parseDoubleOrNull(thresholdText)
                            if (ticker.isBlank()) { error = "Ticker vacío"; return@Button }
                            if (th == null) { error = "Umbral inválido"; return@Button }

                            val t = normalizeTicker(ticker)

                            val rule: AlertRule = when (type) {
                                AlertUiType.PRICE_ABOVE -> AlertRule.PriceAbove(ticker = t, threshold = th)
                                AlertUiType.PRICE_BELOW -> AlertRule.PriceBelow(ticker = t, threshold = th)
                                AlertUiType.PCT_ABOVE -> AlertRule.PercentChangeAbove(ticker = t, thresholdPercent = th)
                                AlertUiType.PCT_BELOW -> AlertRule.PercentChangeBelow(ticker = t, thresholdPercent = th)
                            }

                            onCreate(rule)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand,
                            contentColor = Color(0xFF001018)
                        )
                    ) { Text("Crear", maxLines = 1) }
                }
            }
        }
    }
}
