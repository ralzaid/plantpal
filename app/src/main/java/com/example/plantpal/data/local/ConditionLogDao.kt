package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConditionLogDao {
    @Query("SELECT * FROM condition_logs WHERE plantId = :plantId ORDER BY loggedOn DESC")
    fun getConditionLogsForPlant(plantId: Int): Flow<List<ConditionLogEntity>>

    @Insert
    suspend fun insertConditionLog(log: ConditionLogEntity)
}