package com.example.plantpal.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class DailySummaryResponse(
    @SerialName("date") val date: String,
    @SerialName("temperature") val temperature: DailyTemperature? = null,
    @SerialName("humidity") val humidity: DailyHumidity? = null,
    @SerialName("wind") val wind: DailyWind? = null,
    @SerialName("uv_index") val uvIndex: DailyUv? = null
)

@Serializable
data class DailyTemperature(
    @SerialName("min") val min: Double? = null,
    @SerialName("max") val max: Double? = null,
    @SerialName("afternoon") val afternoon: Double? = null
)

@Serializable
data class DailyHumidity(
    @SerialName("afternoon") val afternoon: Double? = null
)

@Serializable
data class DailyWind(
    @SerialName("max") val max: Double? = null
)

@Serializable
data class DailyUv(
    @SerialName("max") val max: Double? = null
)

interface OpenWeatherHistoryApi {
    @GET("day_summary")
    suspend fun getDaySummary(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("date") date: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): DailySummaryResponse
}

object OpenWeatherHistoryService {
    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/3.0/onecall/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: OpenWeatherHistoryApi = retrofit.create(OpenWeatherHistoryApi::class.java)
}