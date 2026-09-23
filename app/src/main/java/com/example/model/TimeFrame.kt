package com.example.model

enum class TimeFrame(val label: String, val binanceInterval: String) {
    M1("1m", "1m"),
    M3("3m", "3m"),
    M5("5m", "5m"),
    M15("15m", "15m"),
    M30("30m", "30m"),
    H1("1h", "1h"),
    H4("4h", "4h"),
    D1("1d", "1d")
}
