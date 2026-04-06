package com.example.plantpal.data.local

import android.app.Application
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import com.example.plantpal.data.local.PlantDatabase


@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY id DESC")
    fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>>

    @Insert
    suspend fun insertLog(log: WateringLogEntity)
}

