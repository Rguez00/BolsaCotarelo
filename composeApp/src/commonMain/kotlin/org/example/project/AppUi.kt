package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.MarketTrend

internal enum class AppTab(val label: String, val glyph: String) {
    MARKET("Mercado", "📈"),
    PORTFOLIO("Portfolio", "💼"),
    CHARTS("Gráficos", "📊"),
    ALERTS("Alertas", "🔔"),
    STATS("Estadísticas", "📈")
}

internal data class AppPalette(
    val surface0: Color,
    val surface1: Color,
    val surface2: Color,
    val stroke: Color,
    val strokeSoft: Color,
    val brand: Color,
    val brand2: Color,
    val success: Color,
    val danger: Color,
    val neutral: Color,
    val textStrong: Color,
    val textSoft: Color,
    val textMuted: Color
) {
    companion object {
        fun darkFintechWhiteBackdrop(): AppPalette = AppPalette(
            surface0 = Color(0xFF0B1428),
            surface1 = Color(0xFF0F1D38),
            surface2 = Color(0xFF152A4C),
            stroke = Color(0x40FFFFFF),
            strokeSoft = Color(0x2AFFFFFF),
            brand = Color(0xFF22D3EE),
            brand2 = Color(0xFF7C3AED),
            success = Color(0xFF2DD4BF),
            danger = Color(0xFFFB7185),
            neutral = Color(0xFF94A3B8),
            textStrong = Color(0xFFEAF1FF),
            textSoft = Color(0xFFB7C6DE),
            textMuted = Color(0xFF8EA5C6)
        )
    }
}

@Composable
internal fun AppTheme(p: AppPalette, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = p.brand,
        secondary = p.brand2,
        surface = p.surface0,
        error = p.danger,
        onSurface = p.textStrong,
        onPrimary = Color(0xFF001018),
        onSecondary = Color.White,
        onError = Color.White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

internal fun trendLabel(t: MarketTrend): String = when (t) {
    MarketTrend.BULLISH -> "ALCISTA"
    MarketTrend.BEARISH -> "BAJISTA"
    MarketTrend.NEUTRAL -> "NEUTRAL"
}
