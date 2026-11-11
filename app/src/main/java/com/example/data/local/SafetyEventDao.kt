package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SafetyEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyEventDao {
    @Query("SELECT * FROM safety_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SafetyEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SafetyEvent): Long

    @Query("DELETE FROM safety_events")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM safety_events WHERE severityLevel >= 2")
    fun getHazardEventsCount(): Flow<Int>
}
