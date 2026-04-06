package com.example.plantpal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val nickname: String = "",
    val species: String = "",
    val location: String = "Indoor",
    val lightNeeds: String = "Bright indirect light",
    val wateringFrequencyDays: Int = 7,
    val careInstructions: String,
    val lastWateredDate: String = "Not watered yet",
    val imageUrl: String? = null
)
