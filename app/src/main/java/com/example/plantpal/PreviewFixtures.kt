package com.example.plantpal

import com.example.plantpal.ui.screens.UiPlant
import com.example.plantpal.ui.screens.UiUserProfile

val previewProfile = UiUserProfile(
    name = "Rafal",
    email = "rafal@example.com",
    remindersEnabled = true
)

val previewPlants = listOf(
    UiPlant(
        id = 1,
        name = "Monstera",
        nickname = "Mona",
        species = "Monstera deliciosa",
        location = "Living Room",
        lightNeeds = "Bright indirect light",
        wateringFrequencyDays = 7,
        careInstructions = "Rotate weekly and let the top inch dry out before watering.",
        lastWateredDate = "2026-03-25"
    ),
    UiPlant(
        id = 2,
        name = "Snake Plant",
        nickname = "Sage",
        species = "Dracaena trifasciata",
        location = "Bedroom",
        lightNeeds = "Low to bright indirect light",
        wateringFrequencyDays = 14,
        careInstructions = "Avoid overwatering and keep in well-draining soil.",
        lastWateredDate = "2026-04-02"
    )
)
