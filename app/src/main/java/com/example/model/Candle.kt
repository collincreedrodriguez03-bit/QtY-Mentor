package com.example.model

data class Candle(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
) {
    val isGreen: Boolean get() = close >= open
}
