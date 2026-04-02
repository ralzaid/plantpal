package com.example.plantpal

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import retrofit2.http.Path

// =====================================================
// DATA LAYER (Room)
// =====================================================

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val nickname: String = "",
    val species: String = "",
    val location: String = "Indoor",
    val lightNeeds: String = "Bright indirect light",
    val wateringFrequencyDays: Int = 7,
    val careInstructions: String,
    val lastWateredDate: String = "Not watered yet",
    val imageUrl: String? = null
)

@Entity(
    tableName = "watering_logs",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId")]
)
data class WateringLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val wateredOn: String,
    val note: String = ""
)

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name ASC")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :plantId LIMIT 1")
    fun getPlantById(plantId: Int): Flow<PlantEntity?>

    @Insert
    suspend fun insertPlant(plant: PlantEntity)

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Delete
    suspend fun deletePlant(plant: PlantEntity)
}

@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY id DESC")
    fun getLogsForPlant(plantId: Int): Flow<List<WateringLogEntity>>

    @Insert
    suspend fun insertLog(log: WateringLogEntity)
}

@Database(
    entities = [PlantEntity::class, WateringLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao

    companion object {
        @Volatile
        private var INSTANCE: PlantDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN imageUrl TEXT")
            }
        }

        fun getDatabase(app: Application): PlantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    app,
                    PlantDatabase::class.java,
                    "plantpal_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// =====================================================
// PERENUAL API MODELS
// =====================================================

@Serializable
data class PerenualSearchResponse(
    val data: List<PerenualSearchResult>
)

@Serializable
data class PerenualSearchResult(
    val id: Int,
    @SerialName("common_name") val commonName: String,
    @SerialName("scientific_name") val scientificName: List<String>? = null,
    @SerialName("default_image") val defaultImage: PerenualImage? = null
)

@Serializable
data class PerenualImage(
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("original_url") val originalUrl: String? = null
)

@Serializable
data class PerenualDetails(
    val id: Int,
    @SerialName("common_name") val commonName: String,
    val description: String? = null,
    val watering: String? = null,
    val sunlight: List<String>? = null,
    @SerialName("default_image") val defaultImage: PerenualImage? = null
)

interface PerenualApi {
    @GET("species-list")
    suspend fun searchPlants(
        @retrofit2.http.Query("key") apiKey: String,
        @retrofit2.http.Query("q") query: String
    ): PerenualSearchResponse

    @GET("species/details/{id}")
    suspend fun getPlantDetails(
        @Path("id") id: Int,
        @retrofit2.http.Query("key") apiKey: String
    ): PerenualDetails
}

object PerenualService {
    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://perenual.com/api/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: PerenualApi = retrofit.create(PerenualApi::class.java)
}

// =====================================================
// WEATHER API
// =====================================================

@Serializable
data class WeatherResponse(
    val main: MainData,
    val name: String
)

@Serializable
data class MainData(
    val temp: Double,
    val humidity: Int
)

interface OpenWeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @retrofit2.http.Query("lat") lat: Double,
        @retrofit2.http.Query("lon") lon: Double,
        @retrofit2.http.Query("appid") apiKey: String,
        @retrofit2.http.Query("units") units: String = "metric"
    ): WeatherResponse
}

object WeatherService {
    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: OpenWeatherApi = retrofit.create(OpenWeatherApi::class.java)
}

// =====================================================
// REPOSITORY
// =====================================================

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

// =====================================================
// VIEWMODEL
// =====================================================

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
                _searchResults.value = emptyList()
            }
        }
    }

    suspend fun getPerenualDetails(id: Int): PerenualDetails? {
        return try {
            PerenualService.api.getPlantDetails(id, BuildConfig.PERENUAL_API_KEY)
        } catch (e: Exception) {
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

// =====================================================
// UI STATE / NAVIGATION
// =====================================================

sealed class Screen {
    data object PlantList : Screen()
    data object AddPlant : Screen()
    data class PlantDetail(val plantId: Int) : Screen()
}

// =====================================================
// ACTIVITY
// =====================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PlantViewModel::class.java]

        setContent {
            MaterialTheme {
                PlantPalApp(viewModel)
            }
        }
    }
}

// =====================================================
// ROOT APP
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantPalApp(viewModel: PlantViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.PlantList) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentScreen) {
                            is Screen.PlantList -> "PlantPal"
                            is Screen.AddPlant -> "Add Plant"
                            is Screen.PlantDetail -> "Plant Details"
                        }
                    )
                },
                navigationIcon = {
                    if (currentScreen !is Screen.PlantList) {
                        IconButton(onClick = { currentScreen = Screen.PlantList }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentScreen is Screen.PlantList) {
                        IconButton(onClick = { currentScreen = Screen.AddPlant }) {
                            Icon(Icons.Default.Add, contentDescription = "Add plant")
                        }
                    } else {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val screen = currentScreen) {
                is Screen.PlantList -> PlantListScreen(
                    viewModel = viewModel,
                    onAddPlant = { currentScreen = Screen.AddPlant },
                    onPlantClick = { plantId -> currentScreen = Screen.PlantDetail(plantId) }
                )

                is Screen.AddPlant -> AddPlantScreen(
                    viewModel = viewModel,
                    onSave = { name, nickname, species, location, light, wateringDays, care, image ->
                        viewModel.addPlant(
                            name = name,
                            nickname = nickname,
                            species = species,
                            location = location,
                            lightNeeds = light,
                            wateringFrequencyDays = wateringDays,
                            careInstructions = care,
                            imageUrl = image
                        )
                        currentScreen = Screen.PlantList
                    }
                )

                is Screen.PlantDetail -> PlantDetailScreen(
                    plantId = screen.plantId,
                    viewModel = viewModel,
                    onDelete = {
                        currentScreen = Screen.PlantList
                    }
                )
            }
        }
    }
}

// =====================================================
// LIST SCREEN
// =====================================================

@Composable
fun PlantListScreen(
    viewModel: PlantViewModel,
    onAddPlant: () -> Unit,
    onPlantClick: (Int) -> Unit
) {
    val plants by viewModel.plants.collectAsState()
    val weather by viewModel.weatherState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshWeather(BuildConfig.OPENWEATHER_API_KEY)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (weather != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Weather in ${weather!!.city}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${weather!!.temp}°C | ${weather!!.humidity}% Humidity")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        weather!!.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (plants.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No plants yet", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your plants will be stored locally, so you won’t need to enter them again.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddPlant) {
                    Text("Add your first plant")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plants) { plant ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlantClick(plant.id) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (plant.imageUrl != null) {
                                AsyncImage(
                                    model = plant.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column {
                                Text(
                                    text = plant.nickname.ifBlank { plant.name },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Plant: ${plant.name}")
                                Text("Location: ${plant.location}")
                                Text("Last watered: ${plant.lastWateredDate}")
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// ADD PLANT SCREEN
// =====================================================

@Composable
fun AddPlantScreen(
    viewModel: PlantViewModel,
    onSave: (String, String, String, String, String, Int, String, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Indoor") }
    var lightNeeds by remember { mutableStateOf("Bright indirect light") }
    var wateringDays by remember { mutableStateOf("7") }
    var careInstructions by remember {
        mutableStateOf("Keep soil slightly moist but not soggy. Check the top inch of soil before watering.")
    }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchPerenual(it)
                },
                label = { Text("Search for a plant online...") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }

        if (searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
            items(searchResults.take(5)) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                val details = viewModel.getPerenualDetails(result.id)
                                if (details != null) {
                                    name = details.commonName
                                    species = result.scientificName?.firstOrNull() ?: ""
                                    imageUrl = details.defaultImage?.originalUrl ?: details.defaultImage?.thumbnail
                                    careInstructions = details.description ?: careInstructions
                                    lightNeeds = details.sunlight?.joinToString(", ") ?: lightNeeds
                                    searchQuery = "" // Hide search results
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (result.defaultImage?.thumbnail != null) {
                            AsyncImage(
                                model = result.defaultImage.thumbnail,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(result.commonName)
                    }
                }
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Plant Details", style = MaterialTheme.typography.titleMedium)
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plant name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Indoor/Outdoor)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = lightNeeds,
                onValueChange = { lightNeeds = it },
                label = { Text("Light needs") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = wateringDays,
                onValueChange = { wateringDays = it.filter { ch -> ch.isDigit() } },
                label = { Text("Water every X days") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = careInstructions,
                onValueChange = { careInstructions = it },
                label = { Text("Offline care instructions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }

        item {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            nickname.trim(),
                            species.trim(),
                            location.trim(),
                            lightNeeds.trim(),
                            wateringDays.toIntOrNull() ?: 7,
                            careInstructions.trim(),
                            imageUrl
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plant")
            }
        }
    }
}

// =====================================================
// DETAIL SCREEN
// =====================================================

@Composable
fun PlantDetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    onDelete: () -> Unit
) {
    val plant by viewModel.getPlant(plantId).collectAsState(initial = null)
    val logs by viewModel.getLogs(plantId).collectAsState(initial = emptyList())

    if (plant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Plant not found")
        }
        return
    }

    val currentPlant = plant!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (currentPlant.imageUrl != null) {
                        AsyncImage(
                            model = currentPlant.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = currentPlant.nickname.ifBlank { currentPlant.name },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Plant name: ${currentPlant.name}")
                    Text("Species: ${currentPlant.species.ifBlank { "Unknown" }}")
                    Text("Location: ${currentPlant.location}")
                    Text("Light needs: ${currentPlant.lightNeeds}")
                    Text("Water every: ${currentPlant.wateringFrequencyDays} days")
                    Text("Last watered: ${currentPlant.lastWateredDate}")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Care Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentPlant.careInstructions)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.waterPlant(currentPlant) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Watering")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.deletePlant(currentPlant)
                        onDelete()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }

        item {
            Text(
                "Watering History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (logs.isEmpty()) {
            item {
                Text("No watering logs yet.")
            }
        } else {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Watered on: ${log.wateredOn}")
                        if (log.note.isNotBlank()) {
                            Text("Note: ${log.note}")
                        }
                    }
                }
            }
        }
    }
}
