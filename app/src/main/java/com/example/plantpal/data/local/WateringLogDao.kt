package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY wateredOn DESC")
    fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>>

    @Insert
    suspend fun insertLog(log: WateringLogEntity)
}

