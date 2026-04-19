package com.example.plantpal.quiz

import com.example.plantpal.BuildConfig
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantpal.data.repos.PlantRepository
import com.example.plantpal.data.local.ConditionLogEntity
import com.example.plantpal.data.local.EnvironmentLogEntity
import com.example.plantpal.data.local.HealthCheckEntity
import com.example.plantpal.data.local.PlantDatabase
import com.example.plantpal.data.local.PlantObservationEntity
import com.example.plantpal.data.local.PlantEntity
import com.example.plantpal.data.quiz.PlantQuizBuilder
import com.example.plantpal.data.repos.WeatherHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlantQuizUiState(
    val plant: PlantEntity? = null,
    val currentNode: BinaryQuizNode? = null,
    val completed: Boolean = false,
    val answers: Map<String, String> = emptyMap(),
    val diagnosisResult: PlantDiagnosisResult? = null
)

class PlantQuizViewModel(application: Application) : AndroidViewModel(application) {

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

    private val nodes = PlantQuizBuilder.build()

    private val _uiState = MutableStateFlow(PlantQuizUiState())
    val uiState: StateFlow<PlantQuizUiState> = _uiState

    fun startQuiz(plant: PlantEntity) {
        _uiState.value = PlantQuizUiState(
            plant = plant,
            currentNode = nodes["appearance_change"],
            completed = false,
            answers = emptyMap(),
            diagnosisResult = null
        )
    }

    fun answerLeft() = answer(false)

    fun answerRight() = answer(true)

    private fun answer(right: Boolean) {
        val state = _uiState.value
        val plant = state.plant ?: return
        val node = state.currentNode ?: return
        val action = if (right) node.rightAction else node.leftAction

        viewModelScope.launch {
            when (action) {
                is QuizAction.RecordAndNext -> {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val updatedAnswers = state.answers.toMutableMap()

                    action.writes.forEach { write ->
                        when (write) {
                            is QuizWrite.Observation -> {
                                repository.addObservation(
                                    PlantObservationEntity(
                                        plantId = plant.id,
                                        observedOn = today,
                                        category = write.category,
                                        questionKey = write.key,
                                        answerValue = write.value
                                    )
                                )
                                updatedAnswers[write.key] = write.value
                            }

                            is QuizWrite.SoilCondition -> {
                                repository.addConditionLog(
                                    ConditionLogEntity(
                                        plantId = plant.id,
                                        loggedOn = today,
                                        soilCondition = write.soilCondition
                                    )
                                )
                            }

                            is QuizWrite.WateredRecently -> {
                                updatedAnswers["watered_recently"] = if (write.watered) "yes" else "no"

                                if (write.watered) {
                                    repository.waterPlant(plant, today)
                                }
                            }

                            is QuizWrite.HealthSymptom -> {
                                repository.addHealthCheck(
                                    HealthCheckEntity(
                                        plantId = plant.id,
                                        checkedOn = today,
                                        symptom = write.symptom,
                                        recommendation = ""
                                    )
                                )
                            }

                            is QuizWrite.EnvironmentFlag -> {
                                repository.addObservation(
                                    PlantObservationEntity(
                                        plantId = plant.id,
                                        observedOn = today,
                                        category = "environment",
                                        questionKey = "environment_flag",
                                        answerValue = write.label
                                    )
                                )
                            }
                        }
                    }

                    val nextNode = action.nextId?.let { nodes[it] }

                    if (nextNode == null) {
                        val weatherSummary = getWeatherSummaryForLast14Days(plant)
                        val diagnosis = PlantDiagnosisEngine.buildPlantDiagnosis(
                            answers = updatedAnswers,
                            weather = weatherSummary
                        )

                        _uiState.value = state.copy(
                            currentNode = null,
                            completed = true,
                            answers = updatedAnswers,
                            diagnosisResult = diagnosis
                        )
                    } else {
                        _uiState.value = state.copy(
                            currentNode = nextNode,
                            completed = false,
                            answers = updatedAnswers
                        )
                    }
                }
            }
        }
    }

    private val weatherRepo = WeatherHistoryRepository(
        db.dailyWeatherDao()
    )

    private suspend fun getWeatherSummaryForLast14Days(plant: PlantEntity): WeatherSummary14? {
        val user = db.userDao().getUserById(plant.userId) ?: return null
        val lat = user.latitude ?: return null
        val lon = user.longitude ?: return null

        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val end = java.util.Calendar.getInstance()
        val endDate = formatter.format(end.time)

        end.add(java.util.Calendar.DAY_OF_YEAR, -13)
        val startDate = formatter.format(end.time)

        var days = db.dailyWeatherDao().getWeatherRange(
            userId = user.id,
            startDate = startDate,
            endDate = endDate
        )

        if (days.size < 14) {
            weatherRepo.refreshLast14DaysWeatherForUser(
                userId = user.id,
                lat = lat,
                lon = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY
            )

            days = db.dailyWeatherDao().getWeatherRange(
                userId = user.id,
                startDate = startDate,
                endDate = endDate
            )
        }

        if (days.isEmpty()) return null
        return PlantDiagnosisEngine.buildWeatherSummary14(days)
    }
}