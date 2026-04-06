package com.example.plantpal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlantRepositoryTest {

    @Test
    fun allPlants_exposesPlantsFromDao() = runBlocking {
        val fern = PlantEntity(id = 1, name = "Fern", careInstructions = "Keep soil moist")
        val pothos = PlantEntity(id = 2, name = "Pothos", careInstructions = "Water weekly")
        val plantDao = FakePlantDao(initialPlants = listOf(pothos, fern))
        val repository = PlantRepository(plantDao, FakeWateringLogDao())

        assertEquals(listOf(fern, pothos), repository.allPlants.first())
    }

    @Test
    fun addPlant_insertsPlantIntoDao() = runBlocking {
        val plantDao = FakePlantDao()
        val repository = PlantRepository(plantDao, FakeWateringLogDao())
        val plant = PlantEntity(name = "Snake Plant", careInstructions = "Let soil dry between watering")

        repository.addPlant(plant)

        assertEquals(listOf(plant), plantDao.insertedPlants)
        assertEquals(listOf(plant), repository.allPlants.first())
    }

    @Test
    fun waterPlant_updatesPlantAndCreatesLog() = runBlocking {
        val originalPlant = PlantEntity(
            id = 7,
            name = "Aloe Vera",
            careInstructions = "Full sun",
            lastWateredDate = "Not watered yet"
        )
        val plantDao = FakePlantDao(initialPlants = listOf(originalPlant))
        val logDao = FakeWateringLogDao()
        val repository = PlantRepository(plantDao, logDao)

        repository.waterPlant(originalPlant, "2026-04-05")

        val updatedPlant = repository.getPlantById(7).first()
        val createdLog = repository.getLogsForPlant(7).first().single()

        assertEquals("2026-04-05", updatedPlant?.lastWateredDate)
        assertEquals(7, createdLog.plantId)
        assertEquals("2026-04-05", createdLog.wateredOn)
        assertEquals("Watered", createdLog.note)
    }

    private class FakePlantDao(initialPlants: List<PlantEntity> = emptyList()) : PlantDao {
        private val plantsFlow = MutableStateFlow(initialPlants.sortedBy { it.name })
        val insertedPlants = mutableListOf<PlantEntity>()

        override fun getAllPlants(): Flow<List<PlantEntity>> = plantsFlow

        override fun getPlantById(plantId: Int): Flow<PlantEntity?> {
            return plantsFlow.map { plants -> plants.firstOrNull { it.id == plantId } }
        }

        override suspend fun insertPlant(plant: PlantEntity) {
            insertedPlants += plant
            plantsFlow.value = (plantsFlow.value + plant).sortedBy { it.name }
        }

        override suspend fun updatePlant(plant: PlantEntity) {
            plantsFlow.value = plantsFlow.value
                .map { existing -> if (existing.id == plant.id) plant else existing }
                .sortedBy { it.name }
        }

        override suspend fun deletePlant(plant: PlantEntity) {
            plantsFlow.value = plantsFlow.value.filterNot { it.id == plant.id }
        }
    }

    private class FakeWateringLogDao : WateringLogDao {
        private val logsFlow = MutableStateFlow<List<WateringLogEntity>>(emptyList())

        override fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>> {
            return logsFlow.map { logs -> logs.filter { it.plantId == plantId } }
        }

        override suspend fun insertLog(log: WateringLogEntity) {
            logsFlow.value = listOf(log) + logsFlow.value
        }
    }
}
