package com.example.plantpal.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "daily_weather_history",
    primaryKeys = ["userId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class DailyWeatherEntity(
    val userId: Int,
    val date: String,
    val tempMin: Double?,
    val tempMax: Double?,
    val humidityAfternoon: Double?,
    val windMax: Double?,
    val uvMax: Double?
)