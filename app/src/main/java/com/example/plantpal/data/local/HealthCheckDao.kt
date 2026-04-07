package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthCheckDao {
    @Query("SELECT * FROM health_checks WHERE plantId = :plantId ORDER BY checkedOn DESC")
    fun getHealthChecksForPlant(plantId: Int): Flow<List<HealthCheckEntity>>

    @Insert
    suspend fun insertHealthCheck(check: HealthCheckEntity)
}