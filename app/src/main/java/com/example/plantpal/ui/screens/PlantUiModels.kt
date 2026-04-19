package com.example.plantpal.ui.screens

data class UiUserProfile(
    val name: String = "",
    val email: String = "",
    val remindersEnabled: Boolean = true
)

data class UiPlant(
    val id: Int,
    val name: String,
    val nickname: String = "",
    val species: String = "",
    val location: String = "Indoor",
    val lightNeeds: String = "Bright indirect light",
    val wateringFrequencyDays: Int = 7,
    val careInstructions: String = "",
    val lastWateredDate: String = "Not watered yet",
    val imageUrl: String? = null
)
