package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.MarketTrend
import org.example.project.presentation.mode.ThemeMode

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

        fun lightFintechWhiteBackdrop(): AppPalette = AppPalette(
            surface0 = Color(0xFFF8FAFC),
            surface1 = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE2E8F0),

            stroke = Color(0xFF94A3B8),
            strokeSoft = Color(0xFFCBD5E1),

            brand = Color(0xFF0284C7),
            brand2 = Color(0xFF7C3AED),

            success = Color(0xFF059669),
            danger = Color(0xFFDC2626),
            neutral = Color(0xFF64748B),

            textStrong = Color(0xFF0F172A),
            textSoft = Color(0xFF475569),
            textMuted = Color(0xFF64748B)
        )
    }
}

@Composable
internal fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val palette = when (themeMode) {
        ThemeMode.DARK -> AppPalette.darkFintechWhiteBackdrop()
        ThemeMode.LIGHT -> AppPalette.lightFintechWhiteBackdrop()
        ThemeMode.SYSTEM -> AppPalette.darkFintechWhiteBackdrop()
    }


    val scheme = darkColorScheme(
        primary = palette.brand,
        secondary = palette.brand2,
        surface = palette.surface0,
        error = palette.danger,
        onSurface = palette.textStrong,
        onPrimary = Color(0xFF001018),
        onSecondary = Color.White,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

internal fun trendLabel(t: MarketTrend): String = when (t) {
    MarketTrend.BULLISH -> "ALCISTA"
    MarketTrend.BEARISH -> "BAJISTA"
    MarketTrend.NEUTRAL -> "NEUTRAL"
}
