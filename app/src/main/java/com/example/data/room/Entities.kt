package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawings")
data class DrawingEntity(
    @PrimaryKey val id: String,
    val type: String,
    val startXIndex: Int,
    val startPrice: Float,
    val endXIndex: Int,
    val endPrice: Float,
    val colorHex: String,
    val label: String
)

@Entity(tableName = "paper_trades")
data class PaperTradeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val entryPrice: Float,
    val exitPrice: Float?,
    val size: Float,
    val stopLoss: Float,
    val takeProfit: Float,
    val pnl: Float?,
    val returnPct: Float?,
    val reason: String,
    val note: String,
    val timestamp: Long,
    val status: String,
    val source: String = "LIVE",
    val timeframe: String = "1h"
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
