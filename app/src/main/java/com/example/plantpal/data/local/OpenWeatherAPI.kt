package com.example.plantpal.data.local


import kotlinx.serialization.Serializable
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET

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