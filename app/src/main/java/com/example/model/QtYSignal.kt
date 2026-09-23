package com.example.model

data class QtYSignal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val direction: TradeType,
    val confidencePct: Float,
    val timeframe: TimeFrame,
    val timestamp: Long = System.currentTimeMillis(),
    val entryZoneMin: Float,
    val entryZoneMax: Float,
    val stopLoss: Float,
    val target: Float,
    val reason: String,
    val evidence: String,
    val sourceSystem: String = "QtY Prediction Engine v2.4"
)
