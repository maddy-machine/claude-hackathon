package com.runanywhere.startup_hackathon20

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event)

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<Event>>
}
