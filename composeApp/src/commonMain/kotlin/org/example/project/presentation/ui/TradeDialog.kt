package org.example.project.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.presentation.vm.PortfolioViewModel
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun TradeDialog(
    vm: PortfolioViewModel,

    // Paleta (tu estilo dark fintech)
    dialogSurface: Color,
    innerSurface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    textMuted: Color,
    success: Color,
    danger: Color,
    neutral: Color
) {
    if (!vm.dialogOpen) return

    val mode = vm.mode
    val accent = when (mode) {
        PortfolioViewModel.Mode.BUY -> success
        PortfolioViewModel.Mode.SELL -> danger
    }

    val title = when (mode) {
        PortfolioViewModel.Mode.BUY -> "Comprar"
        PortfolioViewModel.Mode.SELL -> "Vender"
    }

    // ✅ Para evitar “error rojo” mientras el usuario borra y reescribe
    val qtyBlank = vm.quantityText.isBlank()

    AlertDialog(
        onDismissRequest = { vm.closeTrade() },

        title = {
            Column {
                Text(
                    text = "$title · ${vm.ticker}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = textStrong
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Confirmación previa (incluye comisión 0.5%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSoft
                )
            }
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =========================
                // Cantidad
                // =========================
                OutlinedTextField(
                    value = vm.quantityText,
                    onValueChange = { raw ->
                        // Permitimos vacío para poder borrar y reescribir sin “pelear”
                        val filtered = raw.filter { it.isDigit() }.take(6)
                        vm.updateQuantityText(filtered)
                    },
                    label = { Text("Cantidad", color = textSoft) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // =========================
                // Estado “busy”
                // =========================
                if (vm.isBusy) {
                    val busyShape = RoundedCornerShape(14.dp)
                    Card(
                        shape = busyShape,
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accent.copy(alpha = 0.20f), busyShape)
                    ) {
                        Text(
                            text = "Procesando operación…",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textStrong
                        )
                    }
                }

                // =========================
                // ✅ Error (si lo hay)
                // - NO lo enseñamos si el campo está vacío
                // - NO lo enseñamos si ya hay operación realizada (lastTx != null)
                // =========================
                val showError = !qtyBlank && vm.error != null && vm.lastTx == null
                if (showError) {
                    val errShape = RoundedCornerShape(14.dp)
                    Card(
                        shape = errShape,
                        colors = CardDefaults.cardColors(containerColor = danger.copy(alpha = 0.12f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, danger.copy(alpha = 0.25f), errShape)
                    ) {
                        Text(
                            text = vm.error ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textStrong
                        )
                    }
                }

                // =========================
                // Preview (si existe)
                // =========================
                val p = vm.preview
                if (p != null) {
                    val prevShape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, stroke, prevShape)
                    ) {
                        Card(
                            shape = prevShape,
                            colors = CardDefaults.cardColors(containerColor = innerSurface),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Previsualización",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textStrong,
                                        modifier = Modifier.weight(1f)
                                    )

                                    val chipShape = RoundedCornerShape(999.dp)
                                    Box(
                                        modifier = Modifier
                                            .background(accent.copy(alpha = 0.14f), chipShape)
                                            .border(1.dp, accent.copy(alpha = 0.25f), chipShape)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (mode == PortfolioViewModel.Mode.BUY) "BUY" else "SELL",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accent
                                        )
                                    }
                                }

                                Divider(color = stroke)

                                PreviewRow("Precio / acción", fmtEuro(p.pricePerShare), textSoft, textStrong)
                                PreviewRow("Bruto", fmtEuro(p.grossTotal), textSoft, textStrong)
                                PreviewRow("Comisión (0.5%)", fmtEuro(p.commission), textSoft, textStrong)

                                Divider(color = stroke)

                                val netLabel =
                                    if (mode == PortfolioViewModel.Mode.BUY) "Total a pagar" else "Total a recibir"
                                PreviewRow(netLabel, fmtEuro(p.netTotal), textMuted, accent)
                            }
                        }
                    }
                } else if (vm.lastTx == null) {
                    // ✅ Solo mostramos el texto de ayuda si NO hay operación realizada
                    Text(
                        text = if (qtyBlank) {
                            "Introduce una cantidad para ver el cálculo."
                        } else {
                            "Introduce una cantidad válida para ver el cálculo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = neutral
                    )
                }

                // =========================
                // Última transacción (si se ejecutó)
                // =========================
                val last = vm.lastTx
                if (last != null) {
                    val lastShape = RoundedCornerShape(16.dp)
                    Card(
                        shape = lastShape,
                        colors = CardDefaults.cardColors(containerColor = dialogSurface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, stroke, lastShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Operación realizada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textStrong
                            )
                            Text(
                                text = "ID: ${last.id} · ${last.type} · ${last.ticker} x${last.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSoft
                            )
                            Text(
                                text = "Neto: ${fmtEuro(last.netTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            // ✅ No confirmes si ya hay lastTx (evita doble confirmación)
            val canConfirm = (vm.preview != null) && (vm.error == null) && !vm.isBusy && (vm.lastTx == null)
            TextButton(
                onClick = { vm.confirm() },
                enabled = canConfirm
            ) {
                Text(
                    text = if (vm.isBusy) "Procesando…" else "Confirmar",
                    color = if (canConfirm) accent else neutral,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = { vm.closeTrade() },
                enabled = !vm.isBusy
            ) {
                Text("Cerrar", color = if (!vm.isBusy) textSoft else neutral)
            }
        },

        containerColor = dialogSurface
    )
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// =========================================================
// Formato € KMP-safe (sin String.format / Locale)
// =========================================================
private fun fmtEuro(v: Double): String = "${fmt2(v)} €"

private fun fmt2(value: Double): String = fmtFixed(value, 2)

private fun fmtFixed(value: Double, decimals: Int): String {
    // Evita "-0.00" cuando estás muy cerca de 0
    val safe = if (abs(value) < 0.0005) 0.0 else value

    val sign = if (safe < 0) "-" else ""
    val absValue = abs(safe)

    val pow10 = pow10(decimals)
    val scaled = (absValue * pow10.toDouble()).roundToLong()

    val intPart = scaled / pow10
    val fracPart = (scaled % pow10).toInt()

    return if (decimals == 0) {
        "$sign$intPart"
    } else {
        "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
    }
}

private fun pow10(decimals: Int): Long {
    var p = 1L
    repeat(decimals.coerceAtLeast(0)) { p *= 10L }
    return p
}
