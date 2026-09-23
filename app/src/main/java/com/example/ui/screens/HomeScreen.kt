package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.ConnectionStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val btcPrice by viewModel.btcPrice.collectAsState()
    val priceChange24h by viewModel.priceChange24h.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsState()
    val qtySignal by viewModel.qtySignal.collectAsState()
    val paperTrades by viewModel.paperTrades.collectAsState()
    val virtualBalance by viewModel.virtualBalance.collectAsState()
    val isStale by viewModel.isStale.collectAsState()

    val closedTrades = paperTrades.filter { it.status == com.example.model.TradeStatus.CLOSED }
    val wins = closedTrades.filter { (it.pnl ?: 0f) > 0f }
    val winRate = if (closedTrades.isNotEmpty()) (wins.size.toFloat() / closedTrades.size) * 100f else 0f
    val totalPnl = closedTrades.sumOf { (it.pnl ?: 0f).toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Hero & Live Market Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("QtY Mentor", style = MaterialTheme.typography.titleMedium, color = QtYBlue)
                        Surface(color = QtYBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("TELEMETRY", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = QtYBlue)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                }

                Divider(color = QtYOutline)

                // Price & 24h Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BTC / USDT", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (btcPrice > 0f) "$${"%.2f".format(btcPrice)}" else "$--",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isNegative = priceChange24h.startsWith("-")
                        Text(
                            text = "24h $priceChange24h",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNegative) TradingRed else TradingGreen
                        )
                        Text(
                            text = "Updated $lastUpdateTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Compact Quick Action Shortcuts Grid (Chart, Practice, Replay, Journal, Learn)
        Text("Quick Navigation", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactNavChip("Chart", Icons.Default.ShowChart, Modifier.weight(1f)) { viewModel.setTab(AppTab.CHART) }
            CompactNavChip("Practice", Icons.Default.School, Modifier.weight(1f)) { viewModel.setTab(AppTab.PRACTICE) }
            CompactNavChip("Replay", Icons.Default.Replay, Modifier.weight(1f)) { viewModel.setTab(AppTab.REPLAY) }
            CompactNavChip("Journal", Icons.Default.Book, Modifier.weight(1f)) { viewModel.setTab(AppTab.JOURNAL) }
            CompactNavChip("Learn", Icons.Default.MenuBook, Modifier.weight(1f)) { viewModel.setTab(AppTab.LEARN) }
        }

        // QtY Signal Integration Card
        val sig = qtySignal
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = QtYPurple, modifier = Modifier.size(18.dp))
                        Text("Active QtY Prediction Signal", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                    if (sig != null) {
                        Surface(color = QtYPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("${sig.confidencePct}% Confidence", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = QtYPurple)
                        }
                    }
                }

                if (sig != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Direction: ${sig.direction} (${sig.timeframe.label})", style = MaterialTheme.typography.bodyMedium, color = if (sig.direction.name == "LONG") TradingGreen else TradingRed)
                            Text("Entry: $${"%.1f".format(sig.entryZoneMin)} - $${"%.1f".format(sig.entryZoneMax)}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SL: $${"%.1f".format(sig.stopLoss)}", style = MaterialTheme.typography.bodySmall, color = TradingRed)
                            Text("TP: $${"%.1f".format(sig.target)}", style = MaterialTheme.typography.bodySmall, color = TradingGreen)
                        }
                    }
                    Text("Reason: ${sig.reason}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("Telemetry Note: Based on recent order-block backtested data (sample size: 200 candles). Past performance does not guarantee future results.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                    Button(
                        onClick = { viewModel.executeQtYSignal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QtYPurple)
                    ) {
                        Text("Execute Signal in Practice Trade", fontSize = 13.sp)
                    }
                } else {
                    Text("Waiting for incoming telemetry predictions...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedButton(
                        onClick = { viewModel.generateSampleQtYSignal() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simulate QtY Prediction Signal", fontSize = 12.sp)
                    }
                }
            }
        }

        // Practice Performance Bar
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Practice Balance & Performance", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Virtual Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%.2f".format(virtualBalance)}", style = MaterialTheme.typography.titleMedium, color = TradingGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Realized P/L", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%.2f".format(totalPnl)}", style = MaterialTheme.typography.titleMedium, color = if (totalPnl >= 0) TradingGreen else TradingRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${"%.1f".format(winRate)}% (${wins.size}/${closedTrades.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun CompactNavChip(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = QtYSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(1.dp, QtYOutline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = label, tint = QtYBlue, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 11.sp, color = TextPrimary)
        }
    }
}
