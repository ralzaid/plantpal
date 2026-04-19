package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantObservationDao {
    @Query("SELECT * FROM plant_observations WHERE plantId = :plantId ORDER BY observedOn DESC")
    fun getObservationsForPlant(plantId: Int): Flow<List<PlantObservationEntity>>

    @Insert
    suspend fun insertObservation(observation: PlantObservationEntity)
}