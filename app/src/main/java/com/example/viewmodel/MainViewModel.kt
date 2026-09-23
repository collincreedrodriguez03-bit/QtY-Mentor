package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.ConnectionStatus
import com.example.repository.GeminiRepository
import com.example.repository.MarketDataRepository
import com.example.repository.StorageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val marketRepo = MarketDataRepository()
    private val storageRepo = StorageRepository(application)
    private val geminiRepo = GeminiRepository()

    // Navigation Tab
    val currentTab = MutableStateFlow(AppTab.HOME)

    // Shared Authoritative Market Data State Flow
    val marketDataState = marketRepo.marketDataState

    val timeFrame = MutableStateFlow(TimeFrame.H1)
    val btcPrice = marketDataState.map { it.btcPrice }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)
    val priceChange24h = marketDataState.map { it.priceChange24h }.stateIn(viewModelScope, SharingStarted.Eagerly, "--")
    val volume24h = marketDataState.map { it.volume24h }.stateIn(viewModelScope, SharingStarted.Eagerly, "--")
    val connectionStatus = marketDataState.map { it.status }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionStatus.CONNECTING)
    val isStale = marketDataState.map { it.isStale }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val lastUpdateTime = marketDataState.map {
        if (it.lastUpdateTimeMs == 0L) "Connecting..."
        else android.text.format.DateFormat.format("HH:mm:ss", it.lastUpdateTimeMs).toString()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Connecting...")
    val candles = marketDataState.map { it.candles }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Indicators toggles
    val activeIndicators = MutableStateFlow(setOf(IndicatorType.VOLUME, IndicatorType.EMA_20))

    // Beginner Mode
    val isBeginnerMode = MutableStateFlow(true)

    // Drawings
    val drawings = storageRepo.drawingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeDrawingTool = MutableStateFlow<DrawingType?>(null)

    // Paper/Practice Trading ($1,000 starting balance)
    val virtualBalance = MutableStateFlow(1000.0f)
    val paperTrades = storageRepo.paperTradesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // QtY Signal Integration
    val qtySignal = MutableStateFlow<QtYSignal?>(null)

    // AI Explanation
    val aiExplanation = MutableStateFlow<String?>(null)
    val isAiLoading = MutableStateFlow(false)

    // Replay Mode State (Using Real Historical Klines)
    val replayCandles = MutableStateFlow<List<Candle>>(emptyList())
    val replayIndex = MutableStateFlow(20)
    val isReplaying = MutableStateFlow(false)
    val replaySpeed = MutableStateFlow(1f) // 0.5x, 1x, 2x, 5x, 10x
    val isReplayLoading = MutableStateFlow(false)
    val replayTimeFrame = MutableStateFlow(TimeFrame.H1)
    val replaySelectedPreset = MutableStateFlow("RECENT")

    // Practice Exercises State
    val practiceExercises = MutableStateFlow<List<PracticeExercise>>(emptyList())
    val currentPracticeIndex = MutableStateFlow(0)
    val practiceSelectedAnswer = MutableStateFlow<Int?>(null)
    val practiceResultFeedback = MutableStateFlow<String?>(null)

    private var replayJob: Job? = null

    init {
        marketRepo.startLiveDataStream(timeFrame.value)
        loadReplayHistoricalData(TimeFrame.H1)
        generateSampleQtYSignal()
        initPracticeExercises()
    }

    fun setTab(tab: AppTab) {
        currentTab.value = tab
    }

    fun setTimeFrame(tf: TimeFrame) {
        timeFrame.value = tf
        marketRepo.startLiveDataStream(tf)
    }

    fun toggleIndicator(indicator: IndicatorType) {
        val current = activeIndicators.value.toMutableSet()
        if (current.contains(indicator)) current.remove(indicator) else current.add(indicator)
        activeIndicators.value = current
    }

    fun toggleBeginnerMode() {
        isBeginnerMode.value = !isBeginnerMode.value
    }

    fun setActiveDrawingTool(tool: DrawingType?) {
        activeDrawingTool.value = tool
    }

    fun addDrawing(drawing: DrawingObject) {
        viewModelScope.launch {
            storageRepo.saveDrawing(drawing)
            activeDrawingTool.value = null
        }
    }

    fun deleteDrawing(id: String) {
        viewModelScope.launch {
            storageRepo.deleteDrawing(id)
        }
    }

    fun generateSampleQtYSignal() {
        val current = btcPrice.value.let { if (it > 0f) it else 64250f }
        qtySignal.value = QtYSignal(
            direction = TradeType.LONG,
            confidencePct = 87.5f,
            timeframe = timeFrame.value,
            entryZoneMin = current * 0.995f,
            entryZoneMax = current * 1.002f,
            stopLoss = current * 0.985f,
            target = current * 1.035f,
            reason = "Bullish Order Block & EMA 20 Bounce",
            evidence = "High volume absorption at support with RSI recovery from oversold."
        )
    }

    fun executeQtYSignal() {
        val sig = qtySignal.value ?: return
        openPaperTrade(
            type = sig.direction,
            size = 250f,
            stopLoss = sig.stopLoss,
            takeProfit = sig.target,
            reason = TradeReason.QTY_SIGNAL,
            note = "Executed from QtY Signal: ${sig.reason}",
            source = "LIVE",
            timeframe = sig.timeframe.label
        )
    }

    fun explainCurrentChart() {
        viewModelScope.launch {
            isAiLoading.value = true
            val currentCandles = candles.value
            val summary = if (currentCandles.isNotEmpty()) {
                val recent = currentCandles.takeLast(20)
                "Recent 20 candles: Start=${recent.first().open}, End=${recent.last().close}, High=${recent.maxOf { it.high }}, Low=${recent.minOf { it.low }}"
            } else {
                "No chart data available."
            }
            val result = geminiRepo.explainChart(summary, isBeginnerMode.value)
            aiExplanation.value = result
            isAiLoading.value = false
        }
    }

    fun clearAiExplanation() {
        aiExplanation.value = null
    }

    // Paper Trading actions
    fun openPaperTrade(
        type: TradeType,
        size: Float,
        stopLoss: Float,
        takeProfit: Float,
        reason: TradeReason,
        note: String,
        source: String = "LIVE",
        timeframe: String = "1h"
    ) {
        viewModelScope.launch {
            val entry = if (source == "LIVE") btcPrice.value else btcPrice.value
            val trade = PaperTrade(
                type = type,
                entryPrice = if (entry > 0f) entry else 64250f,
                size = size,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                reason = reason,
                note = note,
                status = TradeStatus.OPEN,
                source = source,
                timeframe = timeframe
            )
            storageRepo.savePaperTrade(trade)
        }
    }

    fun closePaperTrade(trade: PaperTrade) {
        viewModelScope.launch {
            val exit = btcPrice.value.let { if (it > 0f) it else trade.entryPrice }
            val pnl = if (trade.type == TradeType.LONG) {
                (exit - trade.entryPrice) * (trade.size / trade.entryPrice)
            } else {
                (trade.entryPrice - exit) * (trade.size / trade.entryPrice)
            }
            val returnPct = (pnl / trade.size) * 100f
            val closed = trade.copy(
                exitPrice = exit,
                pnl = pnl,
                returnPct = returnPct,
                status = TradeStatus.CLOSED
            )
            storageRepo.savePaperTrade(closed)
            virtualBalance.value += pnl
        }
    }

    fun resetVirtualBalance(newBalance: Float = 1000.0f) {
        virtualBalance.value = newBalance
    }

    // Replay Mode Setup - Real Historical Klines
    fun loadReplayHistoricalData(tf: TimeFrame = replayTimeFrame.value, startTimeMs: Long? = null, presetName: String = "RECENT") {
        viewModelScope.launch {
            isReplayLoading.value = true
            replayJob?.cancel()
            isReplaying.value = false
            replayTimeFrame.value = tf
            replaySelectedPreset.value = presetName

            val historical = marketRepo.fetchHistoricalKlines(tf, limit = 300, startTime = startTimeMs)
            if (historical.isNotEmpty()) {
                replayCandles.value = historical
                replayIndex.value = (20).coerceAtMost(historical.size - 1)
            }
            isReplayLoading.value = false
        }
    }

    fun setReplaySpeed(speed: Float) {
        replaySpeed.value = speed
        if (isReplaying.value) {
            toggleReplayPlay() // stop
            toggleReplayPlay() // restart with new speed
        }
    }

    fun advanceReplay(count: Int = 1) {
        val list = replayCandles.value
        if (list.isEmpty()) return
        val target = (replayIndex.value + count).coerceAtMost(list.size - 1)
        replayIndex.value = target
        checkReplayTradesProgress()
    }

    fun rewindReplay(count: Int = 1) {
        val list = replayCandles.value
        if (list.isEmpty()) return
        val target = (replayIndex.value - count).coerceAtLeast(0)
        replayIndex.value = target
    }

    fun restartReplay() {
        replayJob?.cancel()
        isReplaying.value = false
        if (replayCandles.value.isNotEmpty()) {
            replayIndex.value = (20).coerceAtMost(replayCandles.value.size - 1)
        }
    }

    fun toggleReplayPlay() {
        isReplaying.value = !isReplaying.value
        if (isReplaying.value) {
            replayJob?.cancel()
            replayJob = viewModelScope.launch {
                while (isReplaying.value) {
                    val delayMs = (1000L / replaySpeed.value).toLong().coerceAtLeast(100L)
                    delay(delayMs)
                    val list = replayCandles.value
                    if (replayIndex.value + 1 < list.size) {
                        replayIndex.value += 1
                        checkReplayTradesProgress()
                    } else {
                        isReplaying.value = false
                        break
                    }
                }
            }
        } else {
            replayJob?.cancel()
        }
    }

    fun openReplayTrade(type: TradeType, size: Float, stopLoss: Float, takeProfit: Float, reason: TradeReason, note: String) {
        val list = replayCandles.value
        if (list.isEmpty() || replayIndex.value >= list.size) return
        val currentCandle = list[replayIndex.value]
        viewModelScope.launch {
            val trade = PaperTrade(
                type = type,
                entryPrice = currentCandle.close,
                size = size,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                reason = reason,
                note = note,
                status = TradeStatus.OPEN,
                source = "REPLAY",
                timeframe = replayTimeFrame.value.label,
                timestamp = currentCandle.timestamp
            )
            storageRepo.savePaperTrade(trade)
        }
    }

    private fun checkReplayTradesProgress() {
        val list = replayCandles.value
        if (list.isEmpty() || replayIndex.value >= list.size) return
        val currentCandle = list[replayIndex.value]

        viewModelScope.launch {
            val activeReplayTrades = paperTrades.value.filter { it.status == TradeStatus.OPEN && it.source == "REPLAY" }
            for (trade in activeReplayTrades) {
                val isLong = trade.type == TradeType.LONG
                val hitSl = if (isLong) currentCandle.low <= trade.stopLoss else currentCandle.high >= trade.stopLoss
                val hitTp = if (isLong) currentCandle.high >= trade.takeProfit else currentCandle.low <= trade.takeProfit

                if (hitSl && hitTp) {
                    // Both SL and TP crossed in same candle!
                    // Conservative Rule: Trigger Stop-Loss first to prevent false profit assumptions
                    val pnl = if (isLong) {
                        (trade.stopLoss - trade.entryPrice) * (trade.size / trade.entryPrice)
                    } else {
                        (trade.entryPrice - trade.stopLoss) * (trade.size / trade.entryPrice)
                    }
                    val returnPct = (pnl / trade.size) * 100f
                    val closed = trade.copy(
                        exitPrice = trade.stopLoss,
                        pnl = pnl,
                        returnPct = returnPct,
                        status = TradeStatus.CLOSED,
                        timestamp = currentCandle.timestamp,
                        note = "${trade.note} [Exit: SL applied (Ambiguous intra-candle SL/TP hit)]".trim()
                    )
                    storageRepo.savePaperTrade(closed)
                    virtualBalance.value += pnl
                } else if (hitSl) {
                    val exitPrice = trade.stopLoss
                    val pnl = if (isLong) {
                        (exitPrice - trade.entryPrice) * (trade.size / trade.entryPrice)
                    } else {
                        (trade.entryPrice - exitPrice) * (trade.size / trade.entryPrice)
                    }
                    val returnPct = (pnl / trade.size) * 100f
                    val closed = trade.copy(
                        exitPrice = exitPrice,
                        pnl = pnl,
                        returnPct = returnPct,
                        status = TradeStatus.CLOSED,
                        timestamp = currentCandle.timestamp,
                        note = "${trade.note} [Exit: Stop-Loss Hit]".trim()
                    )
                    storageRepo.savePaperTrade(closed)
                    virtualBalance.value += pnl
                } else if (hitTp) {
                    val exitPrice = trade.takeProfit
                    val pnl = if (isLong) {
                        (exitPrice - trade.entryPrice) * (trade.size / trade.entryPrice)
                    } else {
                        (trade.entryPrice - exitPrice) * (trade.size / trade.entryPrice)
                    }
                    val returnPct = (pnl / trade.size) * 100f
                    val closed = trade.copy(
                        exitPrice = exitPrice,
                        pnl = pnl,
                        returnPct = returnPct,
                        status = TradeStatus.CLOSED,
                        timestamp = currentCandle.timestamp,
                        note = "${trade.note} [Exit: Take-Profit Hit]".trim()
                    )
                    storageRepo.savePaperTrade(closed)
                    virtualBalance.value += pnl
                }
            }
        }
    }

    // Practice mode exercises
    private fun initPracticeExercises() {
        viewModelScope.launch {
            val realHistorical = marketRepo.fetchHistoricalKlines(TimeFrame.H1, limit = 100)
            val exerciseCandles = if (realHistorical.size >= 80) realHistorical else emptyList()
            practiceExercises.value = listOf(
                PracticeExercise(
                    id = "1",
                    title = "Breakout & Retest Setup",
                    description = "Price consolidated near horizontal resistance. Observe the breakout candle.",
                    modeType = PracticeModeType.PRICE_ACTION_PREDICTION,
                    candles = exerciseCandles,
                    hiddenIndex = 50.coerceAtMost(exerciseCandles.size - 1),
                    question = "What direction will BTC move after testing support?",
                    options = listOf("UP (Bullish Continuation)", "DOWN (Bearish Rejection)", "NO TRADE / RANGE"),
                    correctAnswerIndex = 0,
                    explanation = "Correct! After breaking resistance and retesting it as support with strong volume, buyers stepped in for upward continuation."
                ),
                PracticeExercise(
                    id = "2",
                    title = "Resistance Rejection",
                    description = "BTC approached major resistance with shrinking momentum.",
                    modeType = PracticeModeType.PRICE_ACTION_PREDICTION,
                    candles = exerciseCandles,
                    hiddenIndex = 65.coerceAtMost(exerciseCandles.size - 1),
                    question = "Will price bounce or get rejected at resistance?",
                    options = listOf("UP (Breakout)", "DOWN (Rejection)", "NO TRADE"),
                    correctAnswerIndex = 1,
                    explanation = "Correct! Failing momentum at major resistance indicates seller dominance and rejection."
                )
            )
        }
    }

    fun submitPracticeAnswer(index: Int) {
        practiceSelectedAnswer.value = index
        val current = practiceExercises.value.getOrNull(currentPracticeIndex.value)
        if (current != null) {
            if (index == current.correctAnswerIndex) {
                practiceResultFeedback.value = "Correct! ${current.explanation}"
            } else {
                practiceResultFeedback.value = "Incorrect. ${current.explanation}"
            }
        }
    }

    fun nextPracticeExercise() {
        practiceSelectedAnswer.value = null
        practiceResultFeedback.value = null
        if (currentPracticeIndex.value + 1 < practiceExercises.value.size) {
            currentPracticeIndex.value += 1
        } else {
            currentPracticeIndex.value = 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        marketRepo.close()
    }
}

enum class AppTab {
    HOME, CHART, PRACTICE, REPLAY, JOURNAL, LEARN
}
