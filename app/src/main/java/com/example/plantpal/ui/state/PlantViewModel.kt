package com.example.plantpal.ui.state

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plantpal.BuildConfig
import com.example.plantpal.PerenualDetails
import com.example.plantpal.PerenualSearchResult
import com.example.plantpal.PerenualService
import com.example.plantpal.PlantRepository
import com.example.plantpal.WeatherService
import com.example.plantpal.data.local.PlantEntity
import com.example.plantpal.data.local.PlantDatabase
import com.example.plantpal.data.local.WateringLogEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlantViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantDatabase.getDatabase(application)
    private val repository = PlantRepository(db.plantDao(), db.wateringLogDao())
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val plants: StateFlow<List<PlantEntity>> = repository.allPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    val weatherState: StateFlow<WeatherInfo?> = _weatherState

    private val _searchResults = MutableStateFlow<List<PerenualSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PerenualSearchResult>> = _searchResults

    data class WeatherInfo(
        val city: String,
        val temp: Double,
        val humidity: Int,
        val recommendation: String
    )

    fun searchPerenual(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            println("Perenual API key is blank")
            return
        }
        viewModelScope.launch {
            try {
                val response = PerenualService.api.searchPlants(
                    apiKey = BuildConfig.PERENUAL_API_KEY,
                    query = query
                )
                _searchResults.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            }
        }
    }

    suspend fun getPerenualDetails(id: Int): PerenualDetails? {
        return try {
            PerenualService.api.getPlantDetails(id, BuildConfig.PERENUAL_API_KEY)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPlant(plantId: Int): Flow<PlantEntity?> = repository.getPlantById(plantId)

    fun getLogs(plantId: Int): Flow<List<WateringLogEntity>> = repository.getLogsForPlant(plantId)

    @SuppressLint("MissingPermission")
    fun refreshWeather(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        viewModelScope.launch {
                            try {
                                val weather = WeatherService.api.getCurrentWeather(
                                    location.latitude,
                                    location.longitude,
                                    apiKey
                                )
                                val recommendation = generateRecommendation(weather.main.temp, weather.main.humidity)
                                _weatherState.value = WeatherInfo(
                                    city = weather.name,
                                    temp = weather.main.temp,
                                    humidity = weather.main.humidity,
                                    recommendation = recommendation
                                )
                            } catch (e: Exception) { }
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun generateRecommendation(temp: Double, humidity: Int): String {
        return when {
            temp > 28 && humidity < 40 -> "High heat & low humidity: Your plants are drying out fast! Water more frequently."
            temp < 15 -> "Cooler temperatures: Water needs are lower. Check soil before watering."
            humidity > 70 -> "High humidity: Evaporation is slower. Be careful not to overwater."
            else -> "Conditions are stable. Follow your regular watering schedule."
        }
    }

    fun addPlant(
        name: String,
        nickname: String,
        species: String,
        location: String,
        lightNeeds: String,
        wateringFrequencyDays: Int,
        careInstructions: String,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.addPlant(
                PlantEntity(
                    name = name,
                    nickname = nickname,
                    species = species,
                    location = location,
                    lightNeeds = lightNeeds,
                    wateringFrequencyDays = wateringFrequencyDays,
                    careInstructions = careInstructions,
                    imageUrl = imageUrl
                )
            )
        }
    }

    fun deletePlant(plant: PlantEntity) {
        viewModelScope.launch {
            repository.deletePlant(plant)
        }
    }

    fun waterPlant(plant: PlantEntity) {
        viewModelScope.launch {
            repository.waterPlant(plant, wateredOn = "Today")
        }
    }
}