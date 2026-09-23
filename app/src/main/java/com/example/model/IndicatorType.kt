package com.example.model

enum class IndicatorType(val label: String) {
    EMA_9("EMA 9"),
    EMA_20("EMA 20"),
    EMA_50("EMA 50"),
    EMA_200("EMA 200"),
    RSI_14("RSI 14"),
    MACD("MACD"),
    VWAP("VWAP"),
    BOLLINGER_BANDS("Bollinger Bands"),
    VOLUME("Volume"),
    MARKET_STRUCTURE("Market Structure (HH/HL/LH/LL)"),
    BREAKOUT_DETECTOR("Possible Breakout")
}
