package com.example.plantpal.data.repos

import com.example.plantpal.data.local.ConditionLogDao
import com.example.plantpal.data.local.ConditionLogEntity
import com.example.plantpal.data.local.EnvironmentLogDao
import com.example.plantpal.data.local.EnvironmentLogEntity
import com.example.plantpal.data.local.HealthCheckDao
import com.example.plantpal.data.local.HealthCheckEntity
import com.example.plantpal.data.local.PlantDao
import com.example.plantpal.data.local.PlantEntity
import com.example.plantpal.data.local.PlantObservationDao
import com.example.plantpal.data.local.PlantObservationEntity
import com.example.plantpal.data.local.ReminderDao
import com.example.plantpal.data.local.ReminderEntity
import com.example.plantpal.data.local.WateringLogDao
import com.example.plantpal.data.local.WateringLogEntity
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val plantDao: PlantDao,
    private val wateringLogDao: WateringLogDao,
    private val conditionLogDao: ConditionLogDao,
    private val healthCheckDao: HealthCheckDao,
    private val environmentLogDao: EnvironmentLogDao,
    private val reminderDao: ReminderDao,
    private val plantObservationDao: PlantObservationDao
) {
    fun getPlantsForUser(userId: Int): Flow<List<PlantEntity>> =
        plantDao.getPlantsForUser(userId)

    fun getPlantById(id: Int): Flow<PlantEntity?> =
        plantDao.getPlantById(id)

    fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>> =
        wateringLogDao.getLogsForPlant(plantId)

    fun getConditionLogsForPlant(plantId: Int): Flow<List<ConditionLogEntity>> =
        conditionLogDao.getConditionLogsForPlant(plantId)

    fun getHealthChecksForPlant(plantId: Int): Flow<List<HealthCheckEntity>> =
        healthCheckDao.getHealthChecksForPlant(plantId)

    fun getEnvironmentLogsForPlant(plantId: Int): Flow<List<EnvironmentLogEntity>> =
        environmentLogDao.getEnvironmentLogsForPlant(plantId)

    fun getRemindersForPlant(plantId: Int): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersForPlant(plantId)

    fun getObservationsForPlant(plantId: Int): Flow<List<PlantObservationEntity>> =
        plantObservationDao.getObservationsForPlant(plantId)

    suspend fun addPlant(plant: PlantEntity) {
        plantDao.insertPlant(plant)
    }

    suspend fun updatePlant(plant: PlantEntity) =
        plantDao.updatePlant(plant)

    suspend fun deletePlant(plant: PlantEntity) =
        plantDao.deletePlant(plant)

    suspend fun waterPlant(plant: PlantEntity, wateredOn: String) {
        plantDao.updatePlant(plant.copy(lastWateredDate = wateredOn))
        wateringLogDao.insertLog(
            WateringLogEntity(
                plantId = plant.id,
                wateredOn = wateredOn
            )
        )
    }

    suspend fun addConditionLog(log: ConditionLogEntity) =
        conditionLogDao.insertConditionLog(log)

    suspend fun addHealthCheck(check: HealthCheckEntity) =
        healthCheckDao.insertHealthCheck(check)

    suspend fun addEnvironmentLog(log: EnvironmentLogEntity) =
        environmentLogDao.insertEnvironmentLog(log)

    suspend fun addReminder(reminder: ReminderEntity) =
        reminderDao.insertReminder(reminder)

    suspend fun addObservation(observation: PlantObservationEntity) =
        plantObservationDao.insertObservation(observation)
}