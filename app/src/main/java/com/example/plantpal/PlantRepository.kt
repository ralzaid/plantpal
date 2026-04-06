package com.example.plantpal

import com.example.plantpal.data.local.WateringLogEntity
import com.example.plantpal.data.local.WateringLogDao
import com.example.plantpal.data.local.PlantEntity
import com.example.plantpal.data.local.PlantDao
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val plantDao: PlantDao,
    private val wateringLogDao: WateringLogDao
) {
    val allPlants: Flow<List<PlantEntity>> = plantDao.getAllPlants()

    fun getPlantById(id: Int): Flow<PlantEntity?> = plantDao.getPlantById(id)

    fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>> =
        wateringLogDao.getLogsForPlant(plantId)

    suspend fun addPlant(plant: PlantEntity) = plantDao.insertPlant(plant)

    suspend fun deletePlant(plant: PlantEntity) = plantDao.deletePlant(plant)

    suspend fun waterPlant(plant: PlantEntity, wateredOn: String) {
        plantDao.updatePlant(plant.copy(lastWateredDate = wateredOn))
        wateringLogDao.insertLog(
            WateringLogEntity(
                plantId = plant.id,
                wateredOn = wateredOn,
                note = "Watered"
            )
        )
    }
}