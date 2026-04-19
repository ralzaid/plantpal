package com.example.plantpal.data.repos

import com.example.plantpal.data.local.DailyWeatherDao
import com.example.plantpal.data.local.DailyWeatherEntity
import com.example.plantpal.data.local.OpenWeatherHistoryService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeatherHistoryRepository(
    private val dailyWeatherDao: DailyWeatherDao
) {
    suspend fun refreshLast14DaysWeatherForUser(
        userId: Int,
        lat: Double,
        lon: Double,
        apiKey: String
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        val entries = mutableListOf<DailyWeatherEntity>()

        for (i in 0 until 14) {
            val date = formatter.format(calendar.time)

            try {
                val response = OpenWeatherHistoryService.api.getDaySummary(
                    lat = lat,
                    lon = lon,
                    date = date,
                    apiKey = apiKey
                )

                entries.add(
                    DailyWeatherEntity(
                        userId = userId,
                        date = response.date,
                        tempMin = response.temperature?.min,
                        tempMax = response.temperature?.max,
                        humidityAfternoon = response.humidity?.afternoon,
                        windMax = response.wind?.max,
                        uvMax = response.uvIndex?.max
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        if (entries.isNotEmpty()) {
            dailyWeatherDao.upsertAll(entries)
        }
    }
}