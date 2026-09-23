package com.example.model

enum class DrawingType {
    HORIZONTAL_LINE,
    TREND_LINE,
    RECTANGLE_ZONE,
    SUPPORT_LINE,
    RESISTANCE_LINE,
    BREAKOUT_MARKER,
    ENTRY_MARKER,
    STOP_LOSS_MARKER,
    TARGET_MARKER
}

data class DrawingObject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: DrawingType,
    val startXIndex: Int,
    val startPrice: Float,
    val endXIndex: Int,
    val endPrice: Float,
    val colorHex: String = "#2962FF",
    val label: String = ""
)
