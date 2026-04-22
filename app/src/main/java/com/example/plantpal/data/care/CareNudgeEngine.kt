package com.example.plantpal.data.care

import com.example.plantpal.data.local.PlantEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val temperatureC: Double?,
    val humidityPercent: Int?,
    val windSpeedMetersPerSecond: Double?,
    val rainMillimetersLastHour: Double?,
    val condition: String?
)

enum class CareNudgeType {
    RegularWateringDue,
    HeatDrynessCheck,
    RainDelayWatering,
    WindProtection,
    HighHumidityAirflow,
    ColdProtection
}

enum class CareNudgeUrgency {
    Low,
    Medium,
    High
}

data class CareNudge(
    val type: CareNudgeType,
    val urgency: CareNudgeUrgency,
    val plantName: String,
    val action: String,
    val reason: String,
    val evidence: List<String>
)

data class CareNudgeNotification(
    val title: String,
    val message: String
)

interface CareNudgeCopywriter {
    fun write(nudge: CareNudge): CareNudgeNotification
}

object TemplateCareNudgeCopywriter : CareNudgeCopywriter {
    override fun write(nudge: CareNudge): CareNudgeNotification {
        return when (nudge.type) {
            CareNudgeType.RegularWateringDue -> CareNudgeNotification(
                title = "Water ${nudge.plantName} 🌱",
                message = "Your plant is due for watering."
            )

            CareNudgeType.HeatDrynessCheck -> CareNudgeNotification(
                title = "Heat warning ☀️",
                message = if (nudge.action.contains("already watered", ignoreCase = true)) {
                    nudge.action
                } else {
                    "${nudge.plantName} may dry out faster in high heat."
                }
            )

            CareNudgeType.RainDelayWatering -> CareNudgeNotification(
                title = "Rain warning ☔",
                message = "Bring ${nudge.plantName} indoors to avoid overwatering."
            )

            CareNudgeType.WindProtection -> CareNudgeNotification(
                title = "Wind warning 🌬️",
                message = "Strong winds may damage ${nudge.plantName}."
            )

            CareNudgeType.HighHumidityAirflow -> CareNudgeNotification(
                title = "High moisture warning",
                message = "Wet conditions may affect ${nudge.plantName}."
            )

            CareNudgeType.ColdProtection -> CareNudgeNotification(
                title = "Protect ${nudge.plantName} from cold",
                message = nudge.action
            )
        }
    }
}

object CareNudgePromptBuilder {
    fun buildCopyPrompt(nudge: CareNudge): String {
        val evidenceText = nudge.evidence.joinToString(separator = "; ")
        return """
            Write one concise, friendly plant-care notification.
            Do not diagnose disease. Do not tell the user to water unless the action says to water.
            Keep the advice conditional when soil checking is mentioned.

            Plant: ${nudge.plantName}
            Nudge type: ${nudge.type}
            Urgency: ${nudge.urgency}
            Required action: ${nudge.action}
            Reason: ${nudge.reason}
            Evidence: $evidenceText
        """.trimIndent()
    }
}

object CareNudgeEngine {
    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun buildNudges(
        plant: PlantEntity,
        weather: WeatherSnapshot?,
        now: Date = Date()
    ): List<CareNudge> {
        val daysSinceWatered = daysSinceWatered(plant.lastWateredDate, now)
        val wateredToday = daysSinceWatered == 0L
        val overdueForWater = daysSinceWatered == null ||
            daysSinceWatered >= plant.wateringFrequencyDays.coerceAtLeast(1)

        val isOutdoor = plant.plantType.equals("Outdoor", ignoreCase = true)
        val highWaterNeed = plant.wateringFrequencyDays <= 3 ||
            plant.careInstructions.contains("moist", ignoreCase = true) ||
            plant.careInstructions.contains("frequent", ignoreCase = true)

        val hot = (weather?.temperatureC ?: Double.MIN_VALUE) >= 30.0
        val veryHot = (weather?.temperatureC ?: Double.MIN_VALUE) >= 34.0
        val lowHumidity = (weather?.humidityPercent ?: Int.MAX_VALUE) <= 35
        val highHumidity = (weather?.humidityPercent ?: Int.MIN_VALUE) >= 85
        val windy = (weather?.windSpeedMetersPerSecond ?: 0.0) >= 10.0
        val cold = (weather?.temperatureC ?: Double.MAX_VALUE) <= 10.0
        val rainy = isRainy(weather)

        val nudges = mutableListOf<CareNudge>()

        if (overdueForWater) {
            nudges += CareNudge(
                type = CareNudgeType.RegularWateringDue,
                urgency = if (hot && lowHumidity) CareNudgeUrgency.High else CareNudgeUrgency.Medium,
                plantName = plant.name,
                action = if (hot && lowHumidity) {
                    "Check the soil today and water if the top layer feels dry. Evening watering is safest in high heat."
                } else {
                    "Check the soil today and water if it feels dry."
                },
                reason = "The saved watering interval says this plant is due.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered,
                    extra = "watering is due"
                )
            )
        }

        if (isOutdoor && rainy) {
            nudges += CareNudge(
                type = CareNudgeType.RainDelayWatering,
                urgency = CareNudgeUrgency.Medium,
                plantName = plant.name,
                action = "Bring it indoors or keep it sheltered from excess rain.",
                reason = "Rain can overwater outdoor container plants.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered
                )
            )
        }

        if (wateredToday && hot && lowHumidity && (isOutdoor || highWaterNeed)) {
            nudges += CareNudge(
                type = CareNudgeType.HeatDrynessCheck,
                urgency = if (veryHot) CareNudgeUrgency.High else CareNudgeUrgency.Medium,
                plantName = plant.name,
                action = "You already watered today, so do not automatically water again. Check the soil this evening and water only if it feels dry.",
                reason = "High heat and low humidity can dry soil faster than the normal schedule.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered,
                    extra = "watered today"
                )
            )
        } else if (isOutdoor && hot) {
            nudges += CareNudge(
                type = CareNudgeType.HeatDrynessCheck,
                urgency = if (veryHot) CareNudgeUrgency.High else CareNudgeUrgency.Medium,
                plantName = plant.name,
                action = "Hot weather can make outdoor plants thirsty. Check the soil later and water only if it feels dry.",
                reason = "Outdoor heat can accelerate drying.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered
                )
            )
        }

        if (isOutdoor && windy) {
            nudges += CareNudge(
                type = CareNudgeType.WindProtection,
                urgency = CareNudgeUrgency.High,
                plantName = plant.name,
                action = "Move it to a sheltered spot or bring it indoors until the wind settles.",
                reason = "Strong wind can damage foliage and dry soil faster.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered
                )
            )
        }

        if (isOutdoor && cold) {
            nudges += CareNudge(
                type = CareNudgeType.ColdProtection,
                urgency = CareNudgeUrgency.High,
                plantName = plant.name,
                action = "Bring it indoors or move it away from cold exposure if possible.",
                reason = "Low outdoor temperatures can stress many common houseplants.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered
                )
            )
        }

        if (isOutdoor && highHumidity) {
            nudges += CareNudge(
                type = CareNudgeType.HighHumidityAirflow,
                urgency = CareNudgeUrgency.Medium,
                plantName = plant.name,
                action = "Avoid extra watering for now and give the plant gentle airflow.",
                reason = "Very humid conditions slow evaporation and can support fungal issues.",
                evidence = evidence(
                    plant = plant,
                    weather = weather,
                    daysSinceWatered = daysSinceWatered
                )
            )
        }

        return nudges
            .distinctBy { it.type }
            .sortedWith(compareByDescending<CareNudge> { it.urgency.ordinal }.thenBy { it.type.ordinal })
    }

    fun daysSinceWatered(lastWateredDate: String, now: Date = Date()): Long? {
        if (lastWateredDate.isBlank()) return null

        return try {
            val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
            val wateredOn = formatter.parse(lastWateredDate) ?: return null
            val diffMillis = now.time - wateredOn.time
            TimeUnit.MILLISECONDS.toDays(diffMillis).coerceAtLeast(0)
        } catch (e: Exception) {
            null
        }
    }

    private fun isRainy(weather: WeatherSnapshot?): Boolean {
        if (weather == null) return false

        val condition = weather.condition.orEmpty()
        return condition.equals("Rain", ignoreCase = true) ||
            condition.equals("Drizzle", ignoreCase = true) ||
            condition.equals("Thunderstorm", ignoreCase = true) ||
            (weather.rainMillimetersLastHour ?: 0.0) >= 7.0
    }

    private fun evidence(
        plant: PlantEntity,
        weather: WeatherSnapshot?,
        daysSinceWatered: Long?,
        extra: String? = null
    ): List<String> {
        val values = mutableListOf<String>()

        values += "plant type: ${plant.plantType}"
        values += if (daysSinceWatered == null) {
            "last watered: unknown"
        } else {
            "last watered: $daysSinceWatered day(s) ago"
        }

        weather?.temperatureC?.let { values += "temperature: ${it.toInt()}C" }
        weather?.humidityPercent?.let { values += "humidity: $it%" }
        weather?.windSpeedMetersPerSecond?.let { values += "wind: ${it.toInt()} m/s" }
        weather?.rainMillimetersLastHour?.let { values += "rain: $it mm in the last hour" }
        weather?.condition?.takeIf { it.isNotBlank() }?.let { values += "condition: $it" }
        extra?.let { values += it }

        return values
    }
}
