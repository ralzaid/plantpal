package com.example.plantpal.data.workers

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.plantpal.BuildConfig
import com.example.plantpal.data.local.NotificationHelper
import com.example.plantpal.data.local.PlantDatabase
import com.example.plantpal.data.local.WeatherService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlantReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = PlantDatabase.getDatabase(applicationContext as Application)

        val userId = inputData.getInt(KEY_USER_ID, -1)
        if (userId == -1) return Result.failure()

        val user = db.userDao().getUserById(userId) ?: return Result.failure()
        val plants = db.plantDao().getPlantsForUser(userId).first()

        val latitude = user.latitude
        val longitude = user.longitude

        var temperature: Double? = null
        var humidity: Int? = null
        var windSpeed: Double? = null
        var rainAmount: Double? = null
        var weatherCondition: String? = null

        if (latitude != null && longitude != null) {
            try {
                val weather = WeatherService.api.getCurrentWeather(
                    lat = latitude,
                    lon = longitude,
                    apiKey = BuildConfig.OPENWEATHER_API_KEY
                )

                temperature = weather.main.temp
                humidity = weather.main.humidity
                windSpeed = weather.wind?.speed ?: 0.0
                rainAmount = weather.rain?.oneHour ?: 0.0
                weatherCondition = weather.weather.firstOrNull()?.main.orEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (plant in plants) {
            if (shouldWater(plant.lastWateredDate, plant.wateringFrequencyDays)) {
                NotificationHelper.sendNotification(
                    applicationContext,
                    "Water ${plant.name} 🌱",
                    "Your plant is due for watering."
                )
            }

            if (plant.plantType.equals("Outdoor", ignoreCase = true)) {
                if (
                    weatherCondition != null &&
                    rainAmount != null &&
                    shouldSendRainWarning(weatherCondition!!, rainAmount!!)
                ) {
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "Rain warning ☔",
                        "Bring ${plant.name} indoors to avoid overwatering."
                    )
                }

                if (windSpeed != null && shouldSendWindWarning(windSpeed!!)) {
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "Wind warning 🌬️",
                        "Strong winds may damage ${plant.name}."
                    )
                }

                if (temperature != null && shouldSendHeatWarning(temperature!!)) {
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "Heat warning ☀️",
                        "${plant.name} may dry out faster in high heat."
                    )
                }

                if (humidity != null && shouldSendHumidityWarning(humidity!!)) {
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "High moisture warning",
                        "Wet conditions may affect ${plant.name}."
                    )
                }
            }
        }

        return Result.success()
    }

    private fun shouldWater(lastWatered: String, frequency: Int): Boolean {
        if (lastWatered.isBlank()) return true

        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val lastDate = formatter.parse(lastWatered) ?: return true
            val diffMillis = Date().time - lastDate.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
            diffDays >= frequency
        } catch (e: Exception) {
            true
        }
    }

    private fun shouldSendRainWarning(condition: String, rainAmount: Double): Boolean {
        return condition.equals("Rain", ignoreCase = true) || rainAmount >= 7.0
    }

    private fun shouldSendWindWarning(windSpeed: Double): Boolean {
        return windSpeed >= 10.0
    }

    private fun shouldSendHeatWarning(temp: Double): Boolean {
        return temp >= 30.0
    }

    private fun shouldSendHumidityWarning(humidity: Int): Boolean {
        return humidity >= 85
    }

    companion object {
        const val KEY_USER_ID = "user_id"
    }
}