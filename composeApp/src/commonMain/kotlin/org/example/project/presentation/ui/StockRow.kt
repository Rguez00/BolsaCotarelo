package org.example.project.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.StockSnapshot

@Composable
fun StockRow(
    stock: StockSnapshot,
    // ✅ Paleta inyectada desde App.kt (para consistencia visual)
    surface: Color,
    stroke: Color,
    textStrong: Color,
    textSoft: Color,
    neutral: Color,
    success: Color,
    danger: Color,
    modifier: Modifier = Modifier
) {
    val baseColor = when {
        stock.changePercent > 0.0001 -> success
        stock.changePercent < -0.0001 -> danger
        else -> neutral
    }

    val accent by animateColorAsState(baseColor, label = "stockRowAccent")

    val arrow = when {
        stock.changePercent > 0.0001 -> "▲"
        stock.changePercent < -0.0001 -> "▼"
        else -> "•"
    }

    val shape = RoundedCornerShape(16.dp)
    val badgeShape = RoundedCornerShape(999.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, stroke, shape)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent pill
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.9f))
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.ticker,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textStrong
                )
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSoft
                )
            }

            Text(
                text = String.format("%.2f €", stock.currentPrice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textStrong,
                modifier = Modifier.width(90.dp)
            )

            Box(
                modifier = Modifier
                    .width(86.dp)
                    .clip(badgeShape)
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.25f), badgeShape)
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$arrow ${String.format("%.2f%%", stock.changePercent)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
