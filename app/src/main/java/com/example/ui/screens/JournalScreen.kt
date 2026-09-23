package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaperTrade
import com.example.model.TradeStatus
import com.example.model.TradeType
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun JournalScreen(viewModel: MainViewModel) {
    val paperTrades by viewModel.paperTrades.collectAsState()
    val balance by viewModel.virtualBalance.collectAsState()

    var sourceFilter by remember { mutableStateOf("ALL") }
    var resultFilter by remember { mutableStateOf("ALL") }

    val filteredTrades = paperTrades.filter { trade ->
        val matchSource = sourceFilter == "ALL" || trade.source == sourceFilter
        val matchResult = when (resultFilter) {
            "WINS" -> (trade.pnl ?: 0f) > 0f
            "LOSSES" -> (trade.pnl ?: 0f) < 0f
            else -> true
        }
        matchSource && matchResult
    }

    val closedTrades = paperTrades.filter { it.status == TradeStatus.CLOSED }
    val wins = closedTrades.filter { (it.pnl ?: 0f) > 0f }
    val losses = closedTrades.filter { (it.pnl ?: 0f) < 0f }
    val winRate = if (closedTrades.isNotEmpty()) (wins.size.toFloat() / closedTrades.size) * 100f else 0f
    val totalPnl = closedTrades.sumOf { (it.pnl ?: 0f).toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Statistics Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Journal & Performance Telemetry", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Virtual Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%.2f".format(balance)}", style = MaterialTheme.typography.titleMedium, color = TradingGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Realized P/L", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("$${"%.2f".format(totalPnl)}", style = MaterialTheme.typography.titleMedium, color = if (totalPnl >= 0) TradingGreen else TradingRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${"%.1f".format(winRate)}% (${wins.size}W / ${losses.size}L)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }
        }

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            listOf("ALL", "LIVE", "REPLAY").forEach { src ->
                val isSel = sourceFilter == src
                FilterChip(
                    selected = isSel,
                    onClick = { sourceFilter = src },
                    label = { Text("Src: $src", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QtYBlue,
                        selectedLabelColor = TextPrimary,
                        containerColor = QtYSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }

            listOf("ALL", "WINS", "LOSSES").forEach { res ->
                val isSel = resultFilter == res
                FilterChip(
                    selected = isSel,
                    onClick = { resultFilter = res },
                    label = { Text(res, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QtYBlue,
                        selectedLabelColor = TextPrimary,
                        containerColor = QtYSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Text("Trade History (${filteredTrades.size})", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

        if (filteredTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching journal trades found.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTrades) { trade ->
                    JournalTradeCard(trade = trade, onCloseTrade = { viewModel.closePaperTrade(it) })
                }
            }
        }
    }
}

@Composable
fun JournalTradeCard(trade: PaperTrade, onCloseTrade: (PaperTrade) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isLong = trade.type == TradeType.LONG
    val isClosed = trade.status == TradeStatus.CLOSED
    val pnl = trade.pnl ?: 0f
    val isWin = pnl > 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = QtYSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QtYOutline, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isLong) TradingGreen.copy(alpha = 0.2f) else TradingRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trade.type.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLong) TradingGreen else TradingRed
                        )
                    }
                    Surface(color = QtYSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = trade.source,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Text("Entry: $${"%.1f".format(trade.entryPrice)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isClosed) {
                        Surface(
                            color = if (isWin) TradingGreen.copy(alpha = 0.2f) else TradingRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isWin) "WIN +$${"%.2f".format(pnl)}" else "LOSS $${"%.2f".format(pnl)}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isWin) TradingGreen else TradingRed
                            )
                        }
                    } else {
                        Surface(color = QtYPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("OPEN", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = QtYPurple)
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Divider(color = QtYOutline)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Position Size: $${"%.1f".format(trade.size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Reason: ${trade.reason}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stop Loss: $${"%.1f".format(trade.stopLoss)}", style = MaterialTheme.typography.bodySmall, color = TradingRed)
                        Text("Take Profit: $${"%.1f".format(trade.takeProfit)}", style = MaterialTheme.typography.bodySmall, color = TradingGreen)
                    }
                    if (isClosed && trade.exitPrice != null) {
                        Text("Exit Price: $${"%.1f".format(trade.exitPrice)} | Return: ${"%.2f".format(trade.returnPct ?: 0f)}%", style = MaterialTheme.typography.bodySmall, color = if (isWin) TradingGreen else TradingRed)
                    }
                    if (trade.note.isNotBlank()) {
                        Text("Journal Note: ${trade.note}", style = MaterialTheme.typography.labelSmall, color = QtYBlue)
                    }
                    if (!isClosed) {
                        Button(
                            onClick = { onCloseTrade(trade) },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TradingRed)
                        ) {
                            Text("Close Position", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
