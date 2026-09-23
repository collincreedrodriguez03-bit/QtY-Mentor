package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TradeStatus
import com.example.model.TradeType
import com.example.ui.components.CandlestickChart
import com.example.ui.components.PaperTradeDialog
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun PracticeScreen(viewModel: MainViewModel) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Paper Trading Sim, 1: Quiz Exercises
    val virtualBalance by viewModel.virtualBalance.collectAsState()
    val paperTrades by viewModel.paperTrades.collectAsState()
    val btcPrice by viewModel.btcPrice.collectAsState()

    val openTrades = paperTrades.filter { it.status == TradeStatus.OPEN }
    val closedTrades = paperTrades.filter { it.status == TradeStatus.CLOSED }
    val wins = closedTrades.filter { (it.pnl ?: 0f) > 0f }
    val winRate = if (closedTrades.isNotEmpty()) (wins.size.toFloat() / closedTrades.size) * 100f else 0f
    val totalPnl = closedTrades.sumOf { (it.pnl ?: 0f).toDouble() }.toFloat()

    // Calculate total unrealized PnL from open trades
    val totalUnrealizedPnl = openTrades.sumOf { trade ->
        val pnl = if (trade.type == TradeType.LONG) {
            (btcPrice - trade.entryPrice) * (trade.size / trade.entryPrice)
        } else {
            (trade.entryPrice - btcPrice) * (trade.size / trade.entryPrice)
        }
        pnl.toDouble()
    }.toFloat()

    val totalEquity = virtualBalance + totalUnrealizedPnl

    var showTradeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Switcher Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedSubTab = 0 },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSubTab == 0) QtYBlue else QtYSurfaceVariant)
            ) {
                Text("Paper Trading Sim", fontSize = 12.sp)
            }
            Button(
                onClick = { selectedSubTab = 1 },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSubTab == 1) QtYBlue else QtYSurfaceVariant)
            ) {
                Text("Setup Quiz", fontSize = 12.sp)
            }
        }

        if (selectedSubTab == 0) {
            // Balance & Equity Metrics Card
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Account Equity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$${"%.2f".format(totalEquity)}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Virtual Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$${"%.2f".format(virtualBalance)}", style = MaterialTheme.typography.titleMedium, color = TradingGreen)
                        }
                    }

                    Divider(color = QtYOutline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Realized P/L", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$${"%.2f".format(totalPnl)}", style = MaterialTheme.typography.bodyMedium, color = if (totalPnl >= 0) TradingGreen else TradingRed)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unrealized P/L", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$${"%.2f".format(totalUnrealizedPnl)}", style = MaterialTheme.typography.bodyMedium, color = if (totalUnrealizedPnl >= 0) TradingGreen else TradingRed)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${"%.1f".format(winRate)}% (${wins.size}/${closedTrades.size})", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }

                    Button(
                        onClick = { showTradeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TradingGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Practice Position", fontSize = 13.sp)
                    }
                }
            }

            Text("Open Positions (${openTrades.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

            if (openTrades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active positions. Execute a paper trade to practice strategy execution.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(openTrades) { trade ->
                    val unrealizedPnl = if (trade.type == TradeType.LONG) {
                        (btcPrice - trade.entryPrice) * (trade.size / trade.entryPrice)
                    } else {
                        (trade.entryPrice - btcPrice) * (trade.size / trade.entryPrice)
                    }
                    val isLong = trade.type == TradeType.LONG

                    Card(
                        colors = CardDefaults.cardColors(containerColor = QtYSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, QtYOutline, RoundedCornerShape(10.dp))
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
                                    Text("Entry: $${"%.2f".format(trade.entryPrice)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                }
                                Text(
                                    text = "PnL: $${"%.2f".format(unrealizedPnl)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (unrealizedPnl >= 0) TradingGreen else TradingRed
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size: $${"%.1f".format(trade.size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("SL: $${"%.1f".format(trade.stopLoss)}", style = MaterialTheme.typography.bodySmall, color = TradingRed)
                                Text("TP: $${"%.1f".format(trade.takeProfit)}", style = MaterialTheme.typography.bodySmall, color = TradingGreen)
                            }

                            if (trade.note.isNotBlank()) {
                                Text("Note: ${trade.note}", style = MaterialTheme.typography.labelSmall, color = QtYBlue)
                            }

                            Button(
                                onClick = { viewModel.closePaperTrade(trade) },
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
        } else {
            // Setup Quiz Exercises UI
            val exercises by viewModel.practiceExercises.collectAsState()
            val currentIndex by viewModel.currentPracticeIndex.collectAsState()
            val selectedAnswer by viewModel.practiceSelectedAnswer.collectAsState()
            val feedback by viewModel.practiceResultFeedback.collectAsState()

            val exercise = exercises.getOrNull(currentIndex)
            if (exercise != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = QtYSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QtYOutline, RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(exercise.title, style = MaterialTheme.typography.titleSmall, color = QtYBlue)
                        Text(exercise.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                val subCandles = exercise.candles.take(exercise.hiddenIndex)
                CandlestickChart(
                    candles = subCandles,
                    drawings = emptyList(),
                    activeIndicators = emptySet(),
                    isBeginnerMode = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Text(exercise.question, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

                if (selectedAnswer == null) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.options.forEachIndexed { optIndex, optionText ->
                            OutlinedButton(
                                onClick = { viewModel.submitPracticeAnswer(optIndex) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(optionText, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = QtYSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(feedback ?: "", style = MaterialTheme.typography.bodyMedium, color = TradingGreen)
                            Button(
                                onClick = { viewModel.nextPracticeExercise() },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QtYBlue)
                            ) {
                                Text("Next Setup Exercise")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTradeDialog) {
        PaperTradeDialog(
            currentPrice = btcPrice,
            onDismiss = { showTradeDialog = false },
            onSubmit = { type, size, sl, tp, reason, note ->
                viewModel.openPaperTrade(type, size, sl, tp, reason, note, source = "LIVE", timeframe = "1h")
            }
        )
    }
}
