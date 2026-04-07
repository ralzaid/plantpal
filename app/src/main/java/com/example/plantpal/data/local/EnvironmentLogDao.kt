package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentLogDao {
    @Query("SELECT * FROM environment_logs WHERE plantId = :plantId ORDER BY recordedOn DESC")
    fun getEnvironmentLogsForPlant(plantId: Int): Flow<List<EnvironmentLogEntity>>

    @Insert
    suspend fun insertEnvironmentLog(log: EnvironmentLogEntity)
}