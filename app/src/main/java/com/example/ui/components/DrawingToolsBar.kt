package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingType
import com.example.ui.theme.*

@Composable
fun DrawingToolsBar(
    activeTool: DrawingType?,
    onToolSelected: (DrawingType?) -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        color = QtYSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QtYOutline, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tools:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            val tools = listOf(
                DrawingType.HORIZONTAL_LINE to "H-Line",
                DrawingType.TREND_LINE to "Trend",
                DrawingType.RECTANGLE_ZONE to "Zone",
                DrawingType.SUPPORT_LINE to "Support",
                DrawingType.RESISTANCE_LINE to "Resistance",
                DrawingType.BREAKOUT_MARKER to "Breakout",
                DrawingType.ENTRY_MARKER to "Entry",
                DrawingType.STOP_LOSS_MARKER to "Stop Loss",
                DrawingType.TARGET_MARKER to "Target"
            )

            tools.forEach { (type, label) ->
                val isSelected = activeTool == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onToolSelected(if (isSelected) null else type) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QtYBlue,
                        selectedLabelColor = TextPrimary,
                        containerColor = QtYSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }
    }
}
