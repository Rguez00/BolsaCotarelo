package org.example.project.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

data class PieSlice(val label: String, val value: Double, val color: Color)

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val nonZero = slices.filter { it.value > 0.0 }
    val total = nonZero.sumOf { it.value }.takeIf { it > 0.0 } ?: 0.0

    Column(modifier = modifier.fillMaxSize()) {

        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }

        if (nonZero.isEmpty() || total <= 0.0) {
            // ✅ Estado vacío visible (para que no parezca “bug”)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin datos para mostrar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Fuerza un tamaño mínimo para evitar Canvas colapsado
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 80.dp)
            ) {
                val sizePx = min(size.width, size.height)
                val topLeft = Offset(
                    (size.width - sizePx) / 2f,
                    (size.height - sizePx) / 2f
                )
                val rect = Rect(topLeft, androidx.compose.ui.geometry.Size(sizePx, sizePx))

                var startAngle = -90f
                for (slice in nonZero) {
                    val sweep = (slice.value / total * 360.0).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                    startAngle += sweep
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Limita leyenda para que no rompa el layout
                val legendItems = nonZero.take(6)
                legendItems.forEach { s ->
                    LegendRow(label = s.label, value = s.value, total = total, color = s.color)
                }
                if (nonZero.size > 6) {
                    Text(
                        "+${nonZero.size - 6} sectores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, value: Double, total: Double, color: Color) {
    val pct = ((value / total) * 100.0).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Canvas(Modifier.size(10.dp)) { drawRect(color) }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text("$pct%", style = MaterialTheme.typography.bodySmall)
    }
}
