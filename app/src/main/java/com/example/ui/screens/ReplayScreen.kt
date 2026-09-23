package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IndicatorType
import com.example.model.TimeFrame
import com.example.model.TradeStatus
import com.example.ui.components.CandlestickChart
import com.example.ui.components.PaperTradeDialog
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReplayScreen(viewModel: MainViewModel) {
    val replayCandles by viewModel.replayCandles.collectAsState()
    val replayIndex by viewModel.replayIndex.collectAsState()
    val isReplaying by viewModel.isReplaying.collectAsState()
    val replaySpeed by viewModel.replaySpeed.collectAsState()
    val isReplayLoading by viewModel.isReplayLoading.collectAsState()
    val paperTrades by viewModel.paperTrades.collectAsState()
    val replayTimeFrame by viewModel.replayTimeFrame.collectAsState()
    val replaySelectedPreset by viewModel.replaySelectedPreset.collectAsState()

    val currentDisplayCandles = if (replayCandles.isNotEmpty()) {
        replayCandles.take((replayIndex + 1).coerceAtMost(replayCandles.size))
    } else {
        emptyList()
    }

    val currentCandle = currentDisplayCandles.lastOrNull()
    val currentPrice = currentCandle?.close ?: 0f

    val formattedTimestamp = remember(currentCandle) {
        if (currentCandle != null && currentCandle.timestamp > 0) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(currentCandle.timestamp))
        } else {
            "No Date Loaded"
        }
    }

    var showReplayTradeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Historical Preset Header
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Historical Replay", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            text = if (currentPrice > 0f) "$${"%.2f".format(currentPrice)}" else "Select Preset",
                            style = MaterialTheme.typography.titleMedium,
                            color = QtYBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formattedTimestamp, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = if (replayCandles.isNotEmpty()) "${replayIndex + 1} / ${replayCandles.size} Klines" else "0 Klines",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }

                // Preset Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preset:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                    val presets = listOf(
                        Triple("RECENT", "Recent", null as Long?),
                        Triple("MARCH_2024", "Mar 24 ATH", 1709251200000L),
                        Triple("AUG_2024", "Aug 24 Dip", 1722470400000L),
                        Triple("JAN_2024", "Jan 24 ETF", 1704844800000L)
                    )

                    presets.forEach { (key, label, timeMs) ->
                        val isSel = replaySelectedPreset == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.loadReplayHistoricalData(replayTimeFrame, timeMs, key) },
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

        // Timeframe Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Interval:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            TimeFrame.values().forEach { tf ->
                val isSel = replayTimeFrame == tf
                FilterChip(
                    selected = isSel,
                    onClick = { viewModel.loadReplayHistoricalData(tf, null, replaySelectedPreset) },
                    label = { Text(tf.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QtYBlue,
                        selectedLabelColor = TextPrimary,
                        containerColor = QtYSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        if (isReplayLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = QtYBlue)
                    Text("Fetching Real Historical BTC Klines...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            // Main Replay Chart
            CandlestickChart(
                candles = currentDisplayCandles,
                drawings = emptyList(),
                activeIndicators = setOf(IndicatorType.EMA_20, IndicatorType.VOLUME),
                isBeginnerMode = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Compact Replay Controls Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = QtYSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QtYOutline, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Playback & Step Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.restartReplay() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Restart", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.rewindReplay(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "-1 Candle", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = { viewModel.toggleReplayPlay() },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReplaying) TradingRed else QtYBlue
                        )
                    ) {
                        Icon(
                            imageVector = if (isReplaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isReplaying) "Pause" else "Play", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.advanceReplay(1) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+1", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.advanceReplay(5) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+5", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.advanceReplay(10) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+10", fontSize = 11.sp)
                    }
                }

                // Speed & Trade Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spd:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        listOf(0.5f, 1f, 2f, 5f, 10f).forEach { spd ->
                            val isSel = replaySpeed == spd
                            Surface(
                                color = if (isSel) QtYBlue else QtYSurfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clickable { viewModel.setReplaySpeed(spd) }
                            ) {
                                Text(
                                    text = "${spd}x",
                                    fontSize = 10.sp,
                                    color = if (isSel) TextPrimary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showReplayTradeDialog = true },
                        enabled = currentPrice > 0f,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TradingGreen)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replay Trade", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showReplayTradeDialog && currentPrice > 0f) {
        PaperTradeDialog(
            currentPrice = currentPrice,
            onDismiss = { showReplayTradeDialog = false },
            onSubmit = { type, size, sl, tp, reason, note ->
                viewModel.openReplayTrade(type, size, sl, tp, reason, note)
            }
        )
    }
}
