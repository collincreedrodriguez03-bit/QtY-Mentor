package com.example.repository

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.DrawingEntity
import com.example.data.room.PaperTradeEntity
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).appDao()

    val drawingsFlow: Flow<List<DrawingObject>> = dao.getAllDrawings().map { list ->
        list.map {
            DrawingObject(
                id = it.id,
                type = DrawingType.valueOf(it.type),
                startXIndex = it.startXIndex,
                startPrice = it.startPrice,
                endXIndex = it.endXIndex,
                endPrice = it.endPrice,
                colorHex = it.colorHex,
                label = it.label
            )
        }
    }

    suspend fun saveDrawing(drawing: DrawingObject) {
        dao.insertDrawing(
            DrawingEntity(
                id = drawing.id,
                type = drawing.type.name,
                startXIndex = drawing.startXIndex,
                startPrice = drawing.startPrice,
                endXIndex = drawing.endXIndex,
                endPrice = drawing.endPrice,
                colorHex = drawing.colorHex,
                label = drawing.label
            )
        )
    }

    suspend fun deleteDrawing(id: String) {
        dao.deleteDrawing(id)
    }

    val paperTradesFlow: Flow<List<PaperTrade>> = dao.getAllPaperTrades().map { list ->
        list.map {
            PaperTrade(
                id = it.id,
                type = TradeType.valueOf(it.type),
                entryPrice = it.entryPrice,
                exitPrice = it.exitPrice,
                size = it.size,
                stopLoss = it.stopLoss,
                takeProfit = it.takeProfit,
                pnl = it.pnl,
                returnPct = it.returnPct,
                reason = TradeReason.valueOf(it.reason),
                note = it.note,
                timestamp = it.timestamp,
                status = TradeStatus.valueOf(it.status),
                source = it.source,
                timeframe = it.timeframe
            )
        }
    }

    suspend fun savePaperTrade(trade: PaperTrade) {
        dao.insertPaperTrade(
            PaperTradeEntity(
                id = trade.id,
                type = trade.type.name,
                entryPrice = trade.entryPrice,
                exitPrice = trade.exitPrice,
                size = trade.size,
                stopLoss = trade.stopLoss,
                takeProfit = trade.takeProfit,
                pnl = trade.pnl,
                returnPct = trade.returnPct,
                reason = trade.reason.name,
                note = trade.note,
                timestamp = trade.timestamp,
                status = trade.status.name,
                source = trade.source,
                timeframe = trade.timeframe
            )
        )
    }
}
