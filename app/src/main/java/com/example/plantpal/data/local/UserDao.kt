package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("""
        UPDATE users
        SET latitude = :latitude,
            longitude = :longitude
        WHERE id = :userId
    """)
    suspend fun updateUserLocation(
        userId: Int,
        latitude: Double,
        longitude: Double
    )

    @Query("""
        SELECT latitude IS NOT NULL AND longitude IS NOT NULL
        FROM users
        WHERE id = :userId
        LIMIT 1
    """)
    suspend fun hasSavedLocation(userId: Int): Boolean?
}