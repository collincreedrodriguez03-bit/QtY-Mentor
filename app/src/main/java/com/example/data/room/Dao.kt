package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM drawings")
    fun getAllDrawings(): Flow<List<DrawingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: DrawingEntity)

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun deleteDrawing(id: String)

    @Query("SELECT * FROM paper_trades ORDER BY timestamp DESC")
    fun getAllPaperTrades(): Flow<List<PaperTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaperTrade(trade: PaperTradeEntity)

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingsEntity)
}
