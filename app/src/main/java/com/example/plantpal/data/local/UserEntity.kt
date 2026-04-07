package com.example.plantpal.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val experienceLevel: String = "",
    val numberOfPlants: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null

)