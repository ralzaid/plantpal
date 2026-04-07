package com.example.plantpal.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class PerenualSearchResponse(
    val data: List<PerenualSearchResult> = emptyList()
)

@Serializable
data class PerenualSearchResult(
    val id: Int,
    @SerialName("common_name") val commonName: String? = null,
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
    @SerialName("common_name") val commonName: String? = null,
    val description: String? = null,
    val watering: String? = null,
    val sunlight: List<String>? = null,
    @SerialName("default_image") val defaultImage: PerenualImage? = null
)

interface PerenualApi {
    @GET("species-list")
    suspend fun searchPlants(
        @Query("key") apiKey: String,
        @Query("q") query: String
    ): PerenualSearchResponse

    @GET("species/details/{id}")
    suspend fun getPlantDetails(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): PerenualDetails
}

object PerenualService {
    private val json = Json { ignoreUnknownKeys = true }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://perenual.com/api/v2/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: PerenualApi = retrofit.create(PerenualApi::class.java)
}