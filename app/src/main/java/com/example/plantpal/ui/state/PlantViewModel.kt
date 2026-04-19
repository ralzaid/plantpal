package com.example.plantpal.ui.state

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantpal.BuildConfig
import com.example.plantpal.data.repos.PlantRepository
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

    private val sessionPrefs = application.getSharedPreferences(
        SESSION_PREFS,
        Context.MODE_PRIVATE
    )

    private val db = PlantDatabase.getDatabase(application)

    private val repository = PlantRepository(
        db.plantDao(),
        db.wateringLogDao(),
        db.conditionLogDao(),
        db.healthCheckDao(),
        db.environmentLogDao(),
        db.reminderDao(),
        db.plantObservationDao()
    )

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername

    private val _currentPassword = MutableStateFlow<String?>(null)

    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady: StateFlow<Boolean> = _isSessionReady

    private val _hasSavedHomeLocation = MutableStateFlow(false)
    val hasSavedHomeLocation: StateFlow<Boolean> = _hasSavedHomeLocation

    private val _locationErrorMessage = MutableStateFlow<String?>(null)
    val locationErrorMessage: StateFlow<String?> = _locationErrorMessage

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage

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

    init {
        restoreSession()
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun loginLocalUser(
        username: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        val cleanedUsername = username.trim()
        if (cleanedUsername.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Enter your username and password."
            return
        }

        viewModelScope.launch {
            val existingUser = db.userDao().getUserByUsername(cleanedUsername)

            when {
                existingUser == null -> {
                    _authErrorMessage.value = "No account exists for that username."
                }

                existingUser.passwordHash != password -> {
                    _authErrorMessage.value = "Incorrect password."
                }

                else -> {
                    _currentUsername.value = cleanedUsername
                    _currentPassword.value = password
                    _currentUserId.value = existingUser.id
                    persistSession(existingUser.id, cleanedUsername)
                    _authErrorMessage.value = null
                    refreshSavedLocationFlag(existingUser.id)
                    onSuccess()
                }
            }
        }
    }

    fun registerLocalUser(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanedUsername = username.trim()

        if (cleanedUsername.isBlank() || password.isBlank()) {
            onError("Enter a username and password.")
            return
        }

        viewModelScope.launch {
            val existingUser = db.userDao().getUserByUsername(cleanedUsername)

            if (existingUser != null) {
                onError("An account with that username already exists.")
                return@launch
            }

            db.userDao().insertUser(
                UserEntity(
                    username = cleanedUsername,
                    passwordHash = password,
                    experienceLevel = "",
                    numberOfPlants = 0,
                    latitude = null,
                    longitude = null
                )
            )

            onSuccess()
        }
    }

    fun logout() {
        _currentUserId.value = null
        _currentUsername.value = null
        _currentPassword.value = null
        _hasSavedHomeLocation.value = false
        _locationErrorMessage.value = null
        _authErrorMessage.value = null
        clearPersistedSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            try {
                val savedUserId = sessionPrefs.getInt(SESSION_USER_ID, -1)
                val savedUsername = sessionPrefs.getString(SESSION_USERNAME, null)

                if (savedUserId > 0 && !savedUsername.isNullOrBlank()) {
                    val savedUser = db.userDao().getUserById(savedUserId)
                    if (savedUser != null && savedUser.username == savedUsername) {
                        _currentUserId.value = savedUser.id
                        _currentUsername.value = savedUser.username
                        _currentPassword.value = null
                        _authErrorMessage.value = null
                        refreshSavedLocationFlag(savedUser.id)
                    } else {
                        clearPersistedSession()
                    }
                }
            } catch (e: ClassCastException) {
                e.printStackTrace()
                // Handle legacy/corrupted preference values from previous app versions.
                clearPersistedSession()
            } catch (e: Exception) {
                e.printStackTrace()
                clearPersistedSession()
            } finally {
                _isSessionReady.value = true
            }
        }
    }

    private fun persistSession(userId: Int, username: String) {
        sessionPrefs.edit()
            .putInt(SESSION_USER_ID, userId)
            .putString(SESSION_USERNAME, username)
            .apply()
    }

    private fun clearPersistedSession() {
        sessionPrefs.edit().clear().apply()
    }

    fun clearLocationError() {
        _locationErrorMessage.value = null
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

            _locationErrorMessage.value = null

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    saveUserLocation(userId, location)
                } else {
                    _locationErrorMessage.value =
                        "We couldn’t access your current location. Please try again later."
                }
            }.addOnFailureListener { error ->
                error.printStackTrace()
                _locationErrorMessage.value =
                    "We couldn’t access your current location. Please try again later."
            }
        }
    }

    private fun saveUserLocation(userId: Int, location: Location) {
        viewModelScope.launch {
            db.userDao().updateUserLocation(
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude
            )
            _hasSavedHomeLocation.value = true
            _locationErrorMessage.value = null
        }
    }

    fun searchPerenual(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.PERENUAL_API_KEY.trim()
                if (apiKey.isBlank()) {
                    _searchResults.value = emptyList()
                    return@launch
                }

                val response = PerenualService.api.searchPlants(
                    apiKey = apiKey,
                    query = query
                )
                _searchResults.value = if (response.isSuccessful) {
                    response.body()?.data.orEmpty()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            }
        }
    }

    suspend fun getPerenualDetails(id: Int): PerenualDetails? {
        return try {
            val response = PerenualService.api.getPlantDetails(
                id,
                BuildConfig.PERENUAL_API_KEY.trim()
            )
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPlant(plantId: Int): Flow<PlantEntity?> =
        repository.getPlantById(plantId)

    fun getLogs(plantId: Int): Flow<List<WateringLogEntity>> =
        repository.getLogsForPlant(plantId)

    fun refreshWeatherFromSavedHomeLocation(apiKey: String) {
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            val user = db.userDao().getUserById(userId) ?: return@launch

            val lat = user.latitude ?: return@launch
            val lon = user.longitude ?: return@launch

            try {
                val weather = WeatherService.api.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
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
                "High wind warning. Keep plants indoors to avoid damage. "
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

    private companion object {
        const val SESSION_PREFS = "plantpal_session"
        const val SESSION_USER_ID = "session_user_id"
        const val SESSION_USERNAME = "session_username"
    }
}