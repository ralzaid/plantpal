package com.example.plantpal.ui.state

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantpal.BuildConfig
import com.example.plantpal.PlantRepository
import com.example.plantpal.data.local.NotificationHelper
import com.example.plantpal.data.local.PerenualDetails
import com.example.plantpal.data.local.PerenualSearchResult
import com.example.plantpal.data.local.PerenualService
import com.example.plantpal.data.local.PlantDatabase
import com.example.plantpal.data.local.PlantEntity
import com.example.plantpal.data.local.UserEntity
import com.example.plantpal.data.local.WateringLogEntity
import com.example.plantpal.data.local.WeatherService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlantDatabase.getDatabase(application)

    private val repository = PlantRepository(
        db.plantDao(),
        db.wateringLogDao(),
        db.conditionLogDao(),
        db.healthCheckDao(),
        db.environmentLogDao(),
        db.reminderDao()
    )

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername

    private val _currentPassword = MutableStateFlow<String?>(null)

    private val _hasSavedHomeLocation = MutableStateFlow(false)
    val hasSavedHomeLocation: StateFlow<Boolean> = _hasSavedHomeLocation

    val plants: StateFlow<List<PlantEntity>> =
        _currentUserId
            .flatMapLatest { userId ->
                if (userId == null) flowOf(emptyList())
                else repository.getPlantsForUser(userId)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

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

    fun loginOrCreateLocalUser(username: String, password: String) {
        val cleanedUsername = username.trim()
        if (cleanedUsername.isBlank()) return

        _currentUsername.value = cleanedUsername
        _currentPassword.value = password

        viewModelScope.launch {
            val userId = ensureCurrentUser()
            if (userId != null) {
                refreshSavedLocationFlag(userId)
            }
        }
    }

    fun logout() {
        _currentUserId.value = null
        _currentUsername.value = null
        _currentPassword.value = null
        _hasSavedHomeLocation.value = false
    }

    private suspend fun ensureCurrentUser(): Int? {
        val existingId = _currentUserId.value
        if (existingId != null) return existingId

        val username = _currentUsername.value?.trim().orEmpty()
        val password = _currentPassword.value.orEmpty()

        if (username.isBlank()) return null

        val existingUser = db.userDao().getUserByUsername(username)

        val resolvedId = if (existingUser != null) {
            existingUser.id
        } else {
            db.userDao().insertUser(
                UserEntity(
                    username = username,
                    passwordHash = password,
                    experienceLevel = "",
                    numberOfPlants = 0,
                    latitude = null,
                    longitude = null
                )
            ).toInt()
        }

        _currentUserId.value = resolvedId
        refreshSavedLocationFlag(resolvedId)
        return resolvedId
    }

    private suspend fun refreshSavedLocationFlag(userId: Int) {
        _hasSavedHomeLocation.value = db.userDao().hasSavedLocation(userId) == true
    }

    @SuppressLint("MissingPermission")
    fun captureAndSaveUserLocationIfMissing() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            val alreadyHasLocation = db.userDao().hasSavedLocation(userId) == true

            if (alreadyHasLocation) {
                _hasSavedHomeLocation.value = true
                return@launch
            }

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        db.userDao().updateUserLocation(
                            userId = userId,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        _hasSavedHomeLocation.value = true
                    }
                }
            }
        }
    }

    fun searchPerenual(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
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
            PerenualService.api.getPlantDetails(
                id,
                BuildConfig.PERENUAL_API_KEY
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPlant(plantId: Int): Flow<PlantEntity?> =
        repository.getPlantById(plantId)

    fun getLogs(plantId: Int): Flow<List<WateringLogEntity>> =
        repository.getLogsForPlant(plantId)

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

                                val recommendation = generateRecommendation(
                                    weather.main.temp,
                                    weather.main.humidity
                                )

                                _weatherState.value = WeatherInfo(
                                    city = weather.name,
                                    temp = weather.main.temp,
                                    humidity = weather.main.humidity,
                                    recommendation = recommendation
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateRecommendation(temp: Double, humidity: Int): String {
        return when {
            temp > 28 && humidity < 40 ->
                "High heat and low humidity: your plants are drying out fast. Water more frequently."
            temp < 15 ->
                "Cooler temperatures: water needs are lower. Check soil before watering."
            humidity > 70 ->
                "High humidity: evaporation is slower. Be careful not to overwater."
            else ->
                "Conditions are stable. Follow your regular watering schedule."
        }
    }

    fun addPlant(
        name: String,
        species: String,
        plantType: String,
        wateringFrequencyDays: Int,
        careInstructions: String
    ) {
        viewModelScope.launch {
            val userId = ensureCurrentUser() ?: return@launch

            repository.addPlant(
                PlantEntity(
                    userId = userId,
                    name = name,
                    species = species,
                    plantType = plantType,
                    careInstructions = careInstructions,
                    wateringFrequencyDays = wateringFrequencyDays,
                    lastWateredDate = ""
                )
            )

            NotificationHelper.sendNotification(
                getApplication(),
                "Demo weather alert 🌬️",
                "High wind warning. Keep plants indoor to avoid damage."
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
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.waterPlant(plant, wateredOn = today)
        }
    }
}