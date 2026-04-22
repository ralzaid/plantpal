package com.example.plantpal

import com.example.plantpal.data.care.CareNudgeEngine
import com.example.plantpal.data.care.CareNudgePromptBuilder
import com.example.plantpal.data.care.CareNudgeType
import com.example.plantpal.data.care.TemplateCareNudgeCopywriter
import com.example.plantpal.data.care.WeatherSnapshot
import com.example.plantpal.data.local.PlantEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class CareNudgeEngineTest {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val now = formatter.parse("2026-04-22")!!

    @Test
    fun buildNudges_wateredTodayInHotDryWeatherSuggestsSoilCheckOnly() {
        val nudges = CareNudgeEngine.buildNudges(
            plant = plant(
                plantType = "Outdoor",
                wateringFrequencyDays = 2,
                lastWateredDate = "2026-04-22"
            ),
            weather = WeatherSnapshot(
                temperatureC = 34.0,
                humidityPercent = 25,
                windSpeedMetersPerSecond = 1.0,
                rainMillimetersLastHour = 0.0,
                condition = "Clear"
            ),
            now = now
        )

        assertTrue(nudges.any { it.type == CareNudgeType.HeatDrynessCheck })
        assertFalse(nudges.any { it.type == CareNudgeType.RegularWateringDue })

        val notification = TemplateCareNudgeCopywriter.write(nudges.first())
        assertEquals("Heat warning ☀️", notification.title)
        assertTrue(notification.message.contains("do not automatically water again", ignoreCase = true))
    }

    @Test
    fun buildNudges_dueOutdoorPlantInRainKeepsWateringAndRainWarnings() {
        val nudges = CareNudgeEngine.buildNudges(
            plant = plant(
                plantType = "Outdoor",
                wateringFrequencyDays = 3,
                lastWateredDate = "2026-04-18"
            ),
            weather = WeatherSnapshot(
                temperatureC = 18.0,
                humidityPercent = 70,
                windSpeedMetersPerSecond = 2.0,
                rainMillimetersLastHour = 3.0,
                condition = "Rain"
            ),
            now = now
        )

        assertTrue(nudges.any { it.type == CareNudgeType.RegularWateringDue })
        assertTrue(nudges.any { it.type == CareNudgeType.RainDelayWatering })

        val rainNotification = TemplateCareNudgeCopywriter.write(
            nudges.first { it.type == CareNudgeType.RainDelayWatering }
        )

        assertEquals("Rain warning ☔", rainNotification.title)
        assertEquals("Bring Basil indoors to avoid overwatering.", rainNotification.message)
    }

    @Test
    fun buildNudges_unknownLastWateredFallsBackToRegularWateringCheck() {
        val nudges = CareNudgeEngine.buildNudges(
            plant = plant(lastWateredDate = ""),
            weather = null,
            now = now
        )

        assertEquals(CareNudgeType.RegularWateringDue, nudges.first().type)

        val notification = TemplateCareNudgeCopywriter.write(nudges.first())
        assertEquals("Water Basil 🌱", notification.title)
        assertEquals("Your plant is due for watering.", notification.message)
    }

    @Test
    fun copyPromptKeepsLlmBoundToTheEngineDecision() {
        val nudge = CareNudgeEngine.buildNudges(
            plant = plant(lastWateredDate = ""),
            weather = null,
            now = now
        ).first()

        val prompt = CareNudgePromptBuilder.buildCopyPrompt(nudge)

        assertTrue(prompt.contains("Required action"))
        assertTrue(prompt.contains("Do not diagnose disease"))
    }

    private fun plant(
        plantType: String = "Indoor",
        wateringFrequencyDays: Int = 7,
        lastWateredDate: String = "2026-04-15",
        careInstructions: String = "Let the top inch of soil dry before watering."
    ): PlantEntity {
        return PlantEntity(
            id = 1,
            userId = 1,
            name = "Basil",
            species = "Ocimum basilicum",
            plantType = plantType,
            careInstructions = careInstructions,
            wateringFrequencyDays = wateringFrequencyDays,
            lastWateredDate = lastWateredDate
        )
    }
}
