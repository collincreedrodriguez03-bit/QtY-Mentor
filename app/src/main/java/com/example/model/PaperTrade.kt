package com.example.model

enum class TradeType {
    LONG, SHORT
}

enum class TradeStatus {
    OPEN, CLOSED
}

enum class TradeReason {
    BREAKOUT,
    RETEST,
    SUPPORT_BOUNCE,
    RESISTANCE_REJECTION,
    TREND_CONTINUATION,
    REVERSAL,
    QTY_SIGNAL,
    OTHER
}

data class PaperTrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TradeType,
    val entryPrice: Float,
    val exitPrice: Float? = null,
    val size: Float, // in USD
    val stopLoss: Float,
    val takeProfit: Float,
    val pnl: Float? = null,
    val returnPct: Float? = null,
    val reason: TradeReason,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: TradeStatus = TradeStatus.OPEN,
    val source: String = "LIVE", // LIVE, REPLAY, PRACTICE
    val timeframe: String = "1h"
)
