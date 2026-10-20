package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SandyInteractionDao {
    @Query("SELECT * FROM sandy_interactions ORDER BY timestamp DESC")
    fun getAllInteractionsFlow(): Flow<List<SandyInteractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: SandyInteractionEntity)

    @Query("DELETE FROM sandy_interactions WHERE id = :id")
    suspend fun deleteInteractionById(id: String)

    @Query("DELETE FROM sandy_interactions")
    suspend fun clearAllInteractions()
}
