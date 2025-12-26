package org.example.project

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
internal fun BottomTabs(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    surface: Color,
    stroke: Color,
    textSoft: Color,
    textStrong: Color,
    brand: Color
) {
    NavigationBar(
        containerColor = surface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, stroke)
    ) {
        AppTab.values().forEach { tab ->
            val isSel = tab == selected
            NavigationBarItem(
                selected = isSel,
                onClick = { onSelect(tab) },
                icon = { Text(text = tab.glyph, style = MaterialTheme.typography.titleSmall) },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
internal fun LeftRail(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    surface: Color,
    strokeSoft: Color,
    textSoft: Color,
    textStrong: Color,
    brand: Color
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .width(180.dp)
            .fillMaxSize()
            .border(1.dp, strokeSoft, shape)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Secciones",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textStrong
            )
            Spacer(Modifier.height(10.dp))

            NavigationRail(
                containerColor = Color.Transparent,
                header = {
                    Text(text = "Bolsa", color = brand, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                }
            ) {
                AppTab.values().forEach { tab ->
                    val isSel = tab == selected
                    NavigationRailItem(
                        selected = isSel,
                        onClick = { onSelect(tab) },
                        icon = { Text(tab.glyph) },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}
