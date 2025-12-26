package org.example.project.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

data class BarItem(val label: String, val value: Double)

@Composable
fun ProfitBarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    positiveColor: Color = Color(0xFF4CAF50),
    negativeColor: Color = Color(0xFFF44336),
    zeroColor: Color = Color(0xFF9E9E9E)
) {
    val maxAbs = max(1.0, items.maxOfOrNull { abs(it.value) } ?: 1.0)

    Column(modifier) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // axis
            drawLine(Color.LightGray, start = androidx.compose.ui.geometry.Offset(0f, midY), end = androidx.compose.ui.geometry.Offset(w, midY), strokeWidth = 2f)

            if (items.isEmpty()) return@Canvas

            val barWidth = w / (items.size * 1.6f)
            val gap = barWidth * 0.6f

            items.forEachIndexed { i, item ->
                val x = (gap / 2f) + i * (barWidth + gap)
                val norm = (item.value / maxAbs).toFloat()
                val barHeight = (abs(norm) * (h * 0.45f))

                val color = when {
                    item.value > 0 -> positiveColor
                    item.value < 0 -> negativeColor
                    else -> zeroColor
                }

                val top = if (item.value >= 0) (midY - barHeight) else midY
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        // Etiquetas (si hay muchas, mejor limitar/top N)
        items.take(8).forEach {
            Text("${it.label}: ${"%.2f".format(it.value)}€", style = MaterialTheme.typography.bodySmall)
        }
    }
}

