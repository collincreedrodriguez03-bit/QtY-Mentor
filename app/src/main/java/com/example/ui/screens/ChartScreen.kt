package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IndicatorType
import com.example.model.TimeFrame
import com.example.repository.ConnectionStatus
import com.example.ui.components.CandlestickChart
import com.example.ui.components.DrawingToolsBar
import com.example.ui.components.ExplainDialog
import com.example.ui.components.PaperTradeDialog
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun ChartScreen(viewModel: MainViewModel) {
    val candles by viewModel.candles.collectAsState()
    val drawings by viewModel.drawings.collectAsState()
    val activeIndicators by viewModel.activeIndicators.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()
    val btcPrice by viewModel.btcPrice.collectAsState()
    val priceChange24h by viewModel.priceChange24h.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsState()
    val isStale by viewModel.isStale.collectAsState()
    val activeTool by viewModel.activeDrawingTool.collectAsState()

    val aiExplanation by viewModel.aiExplanation.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showPaperTradeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Compact Ticker Header Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BTC/USDT", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (btcPrice > 0f) "$${"%.2f".format(btcPrice)}" else "$--",
                            style = MaterialTheme.typography.titleMedium,
                            color = TradingGreen
                        )
                        if (isStale) {
                            Badge(containerColor = TradingRed.copy(alpha = 0.2f)) {
                                Text("STALE", color = TradingRed, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val isNeg = priceChange24h.startsWith("-")
                    Text("24h: $priceChange24h", style = MaterialTheme.typography.bodySmall, color = if (isNeg) TradingRed else TradingGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val statusText = when {
                            isStale -> "STALE"
                            else -> connectionStatus.name
                        }
                        val statusColor = when (connectionStatus) {
                            ConnectionStatus.LIVE -> if (isStale) TradingRed else TradingGreen
                            ConnectionStatus.CONNECTING -> QtYBlue
                            ConnectionStatus.RECONNECTING -> TradingYellow
                            ConnectionStatus.OFFLINE -> TradingRed
                        }
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                }
            }
        }

        // Indicators Bar with Clear ON/OFF Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IndicatorType.values().forEach { indicator ->
                val isActive = activeIndicators.contains(indicator)
                FilterChip(
                    selected = isActive,
                    onClick = { viewModel.toggleIndicator(indicator) },
                    label = {
                        Text(
                            text = if (isActive) "${indicator.label} • ON" else "${indicator.label} • OFF",
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QtYBlue,
                        selectedLabelColor = TextPrimary,
                        containerColor = QtYSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Drawing Tools Bar
        DrawingToolsBar(activeTool = activeTool, onToolSelected = { viewModel.setActiveDrawingTool(it) })

        // Main TradingView Style Candlestick Chart View
        CandlestickChart(
            candles = candles,
            drawings = drawings,
            activeIndicators = activeIndicators,
            isBeginnerMode = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Timeframe Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Interval:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            TimeFrame.values().forEach { tf ->
                val isSel = timeFrame == tf
                Surface(
                    color = if (isSel) QtYBlue else QtYSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { viewModel.setTimeFrame(tf) }
                ) {
                    Text(
                        text = tf.label,
                        fontSize = 12.sp,
                        color = if (isSel) TextPrimary else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Action Bar: Paper Trade & AI Chart Analysis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showPaperTradeDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TradingGreen)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paper Trade", fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.explainCurrentChart() },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QtYPurple)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Explain Chart", fontSize = 12.sp)
            }
        }
    }

    if (showPaperTradeDialog) {
        PaperTradeDialog(
            currentPrice = btcPrice,
            onDismiss = { showPaperTradeDialog = false },
            onSubmit = { type, size, sl, tp, reason, note ->
                viewModel.openPaperTrade(type, size, sl, tp, reason, note)
            }
        )
    }

    if (aiExplanation != null || isAiLoading) {
        ExplainDialog(
            explanation = aiExplanation,
            isLoading = isAiLoading,
            onDismiss = { viewModel.clearAiExplanation() }
        )
    }
}
