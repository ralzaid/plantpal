package com.example.plantpal.data.workers

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.plantpal.BuildConfig
import com.example.plantpal.data.care.CareNudgeEngine
import com.example.plantpal.data.care.TemplateCareNudgeCopywriter
import com.example.plantpal.data.care.WeatherSnapshot
import com.example.plantpal.data.local.NotificationHelper
import com.example.plantpal.data.local.PlantDatabase
import com.example.plantpal.data.local.WeatherService
import kotlinx.coroutines.flow.first

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

        var weatherSnapshot: WeatherSnapshot? = null

        if (latitude != null && longitude != null) {
            try {
                val weather = WeatherService.api.getCurrentWeather(
                    lat = latitude,
                    lon = longitude,
                    apiKey = BuildConfig.OPENWEATHER_API_KEY
                )

                weatherSnapshot = WeatherSnapshot(
                    temperatureC = weather.main.temp,
                    humidityPercent = weather.main.humidity,
                    windSpeedMetersPerSecond = weather.wind?.speed,
                    rainMillimetersLastHour = weather.rain?.oneHour,
                    condition = weather.weather.firstOrNull()?.main
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (plant in plants) {
            CareNudgeEngine.buildNudges(
                plant = plant,
                weather = weatherSnapshot
            ).forEach { nudge ->
                val notification = TemplateCareNudgeCopywriter.write(nudge)
                NotificationHelper.sendNotification(
                    applicationContext,
                    notification.title,
                    notification.message
                )
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_USER_ID = "user_id"
    }
}
