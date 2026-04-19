package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DailyWeatherEntity>)

    @Query("""
        SELECT * FROM daily_weather_history
        WHERE userId = :userId
          AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    suspend fun getWeatherRange(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<DailyWeatherEntity>
}