package com.example.plantpal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantUiUtilsTest {

    @Test
    fun parseStoredDate_parsesKnownDate() {
        assertNotNull(parseStoredDate("2026-03-25"))
    }

    @Test
    fun formatStoredDate_returnsOriginalValueWhenUnparseable() {
        assertEquals("unknown", formatStoredDate("unknown"))
    }

    @Test
    fun needsWaterToday_returnsTrueForOldWateringDate() {
        val plant = UiPlant(
            id = 1,
            name = "Fern",
            careInstructions = "Keep soil moist",
            wateringFrequencyDays = 7,
            lastWateredDate = "2025-01-01"
        )

        assertTrue(needsWaterToday(plant))
    }
}
