package com.example.plantpal.quiz
import com.example.plantpal.data.local.DailyWeatherEntity

data class WeatherSummary14(
    val coldDays: Int = 0,
    val hotDays: Int = 0,
    val highHumidityDays: Int = 0,
    val lowHumidityDays: Int = 0,
    val maxUv: Double = 0.0,
    val windyDays: Int = 0
)

data class PlantDiagnosisResult(
    val diagnosis: String,
    val explanation: String,
    val actionPlan: List<String>,
    val confidence: String
)

object PlantDiagnosisEngine {

    fun buildPlantDiagnosis(
        answers: Map<String, String>,
        weather: WeatherSummary14?
    ): PlantDiagnosisResult {
        val powderyCoating = isYes(answers["powdery_coating"])
        val grayGrowth = isYes(answers["gray_growth"])
        val webbing = isYes(answers["webbing"])
        val cottonyGrowth = isYes(answers["cottony_growth"])
        val blackening = isYes(answers["blackening"])
        val coldExposure = isYes(answers["cold_exposure"])
        val brownScorch = isYes(answers["brown_scorch"])
        val moreDirectLight = isYes(answers["more_direct_light"])
        val soilSoggy = isYes(answers["soil_soggy"])
        val soilDry = isYes(answers["soil_dry"])
        val yellowing = isYes(answers["yellowing"])
        val wilting = isYes(answers["wilting"])
        val wateredRecently = isYes(answers["watered_recently"])
        val fertilizedRecently = isYes(answers["fertilized_recently"])
        val saltBuildup = isYes(answers["salt_buildup"])
        val environmentChanged = isYes(answers["environment_changed"])

        val humidSupport = (weather?.highHumidityDays ?: 0) >= 5
        val lowHumiditySupport = (weather?.lowHumidityDays ?: 0) >= 5
        val hotSupport = (weather?.hotDays ?: 0) >= 4
        val coldSupport = (weather?.coldDays ?: 0) > 0
        val uvSupport = (weather?.maxUv ?: 0.0) >= 6.0
        val windSupport = (weather?.windyDays ?: 0) >= 4

        if (powderyCoating) {
            return PlantDiagnosisResult(
                diagnosis = "Powdery mildew–like fungal issue",
                explanation = buildString {
                    append(
                        "The quiz showed a white powdery coating on the leaves, " +
                                "which strongly points to a powdery mildew–like issue."
                    )
                    if (humidSupport) {
                        append(
                            " The last two weeks also included several humid days, " +
                                    "which can support fungal disease development."
                        )
                    }
                },
                actionPlan = listOf(
                    "Isolate the plant from nearby plants.",
                    "Improve airflow around the plant.",
                    "Remove badly affected leaves.",
                    "Avoid keeping foliage damp for long periods."
                ),
                confidence = confidenceLabel(baseSpecific = true, weatherSupport = humidSupport)
            )
        }

        if (grayGrowth) {
            return PlantDiagnosisResult(
                diagnosis = "Gray mold–like fungal issue",
                explanation = buildString {
                    append(
                        "The quiz showed fuzzy gray growth, which matches a gray mold–like pattern."
                    )
                    if (humidSupport) {
                        append(
                            " Recent humid conditions make fungal growth more plausible."
                        )
                    }
                },
                actionPlan = listOf(
                    "Remove affected leaves or flowers.",
                    "Increase airflow and reduce crowding.",
                    "Do not let moisture linger around the plant."
                ),
                confidence = confidenceLabel(baseSpecific = true, weatherSupport = humidSupport)
            )
        }

        if (webbing) {
            return PlantDiagnosisResult(
                diagnosis = "Spider mite–like pest issue",
                explanation = buildString {
                    append(
                        "The quiz showed webbing or tiny-dot damage, which fits a spider mite–like pattern."
                    )
                    if (hotSupport || lowHumiditySupport) {
                        append(
                            " The recent hot or dry weather pattern may have made this stress pattern more likely."
                        )
                    }
                },
                actionPlan = listOf(
                    "Rinse leaves, especially undersides, with water.",
                    "Inspect nearby plants for similar webbing.",
                    "Repeat cleaning over several days."
                ),
                confidence = confidenceLabel(
                    baseSpecific = true,
                    weatherSupport = hotSupport || lowHumiditySupport
                )
            )
        }

        if (cottonyGrowth) {
            return PlantDiagnosisResult(
                diagnosis = "Mealybug–like pest issue",
                explanation =
                    "The quiz showed white cotton-like growth, which strongly suggests a mealybug-like pest problem.",
                actionPlan = listOf(
                    "Isolate the plant from nearby plants.",
                    "Wipe visible clusters off stems and leaf joints.",
                    "Inspect hidden areas carefully and repeat checks."
                ),
                confidence = "High"
            )
        }

        if (blackening || coldExposure) {
            return PlantDiagnosisResult(
                diagnosis = "Cold injury",
                explanation = buildString {
                    append(
                        "The quiz showed blackening or direct cold exposure, which fits cold injury."
                    )
                    if (coldSupport) {
                        append(
                            " The last two weeks also included cold days, which strengthens that explanation."
                        )
                    }
                },
                actionPlan = listOf(
                    "Move the plant away from drafts or cold windows.",
                    "Prevent further cold exposure.",
                    "Monitor new growth rather than expecting damaged tissue to recover."
                ),
                confidence = confidenceLabel(baseSpecific = true, weatherSupport = coldSupport)
            )
        }

        if (brownScorch && moreDirectLight) {
            return PlantDiagnosisResult(
                diagnosis = "Light / heat stress",
                explanation = buildString {
                    append(
                        "The quiz showed scorched foliage after increased direct light, which suggests light or heat stress."
                    )
                    if (hotSupport || uvSupport || windSupport) {
                        append(
                            " The recent weather pattern may have made that stress more severe."
                        )
                    }
                },
                actionPlan = listOf(
                    "Move the plant to bright indirect light.",
                    "Avoid abrupt increases in direct sun exposure.",
                    "Trim severely damaged leaves only after checking whether symptoms are still spreading."
                ),
                confidence = confidenceLabel(
                    baseSpecific = true,
                    weatherSupport = hotSupport || uvSupport || windSupport
                )
            )
        }

        if (soilSoggy && (yellowing || wilting || wateredRecently)) {
            return PlantDiagnosisResult(
                diagnosis = "Overwatering or root stress",
                explanation = buildString {
                    append(
                        "The quiz showed soggy soil together with yellowing, wilting, or recent watering, " +
                                "which points to excess moisture and possible root stress."
                    )
                    if (humidSupport) {
                        append(
                            " Recent humid weather could have slowed drying and made that stress worse."
                        )
                    }
                },
                actionPlan = listOf(
                    "Pause watering until the potting medium dries appropriately.",
                    "Check that the pot drains well.",
                    "Inspect roots if symptoms continue.",
                    "Increase airflow around the plant."
                ),
                confidence = confidenceLabel(baseSpecific = true, weatherSupport = humidSupport)
            )
        }

        if (soilDry && wilting && !wateredRecently) {
            return PlantDiagnosisResult(
                diagnosis = "Underwatering or dehydration stress",
                explanation = buildString {
                    append(
                        "The quiz showed dry soil, wilting, and no recent watering, " +
                                "which strongly suggests dehydration stress."
                    )
                    if (hotSupport || lowHumiditySupport || windSupport) {
                        append(
                            " The last two weeks were also hot, dry, or windy enough to increase water loss."
                        )
                    }
                },
                actionPlan = listOf(
                    "Water thoroughly until excess drains out.",
                    "Recheck moisture sooner than usual.",
                    "Monitor recovery over the next 24–48 hours."
                ),
                confidence = confidenceLabel(
                    baseSpecific = true,
                    weatherSupport = hotSupport || lowHumiditySupport || windSupport
                )
            )
        }

        if (saltBuildup || (fertilizedRecently && (yellowing || brownScorch))) {
            return PlantDiagnosisResult(
                diagnosis = "Fertilizer or salt stress",
                explanation =
                    "The quiz showed fertilizer-related stress signals such as salt buildup, yellowing, or scorch, which can point to fertilizer or soluble-salt stress.",
                actionPlan = listOf(
                    "Flush the potting medium thoroughly if appropriate.",
                    "Pause fertilizing for now.",
                    "Watch whether new growth improves."
                ),
                confidence = if (saltBuildup) "High" else "Medium"
            )
        }

        return PlantDiagnosisResult(
            diagnosis = "General environmental stress",
            explanation = buildString {
                append(
                    "The quiz did not point to one clear pest or disease pattern. " +
                            "The symptoms suggest general plant stress instead of one highly specific issue."
                )
                if (humidSupport || hotSupport || lowHumiditySupport || coldSupport) {
                    append(
                        " The recent weather pattern may be contributing to that stress."
                    )
                }
                if (environmentChanged) {
                    append(
                        " A recent environmental change may also be part of the problem."
                    )
                }
            },
            actionPlan = listOf(
                "Stabilize watering and avoid sudden care changes.",
                "Inspect roots and the undersides of leaves.",
                "Avoid moving the plant again until symptoms are clearer.",
                "Monitor the plant for a few more days before making another major adjustment."
            ),
            confidence = "Low"
        )
    }

    private fun isYes(value: String?): Boolean {
        return value.equals("yes", ignoreCase = true)
    }

    private fun confidenceLabel(baseSpecific: Boolean, weatherSupport: Boolean): String {
        return when {
            baseSpecific && weatherSupport -> "High"
            baseSpecific -> "Medium"
            else -> "Low"
        }
    }

    fun buildWeatherSummary14(days: List<DailyWeatherEntity>): WeatherSummary14 {
        return WeatherSummary14(
            coldDays = days.count { (it.tempMin ?: Double.MAX_VALUE) < 10.0 },
            hotDays = days.count { (it.tempMax ?: Double.MIN_VALUE) > 30.0 },
            highHumidityDays = days.count { (it.humidityAfternoon ?: 0.0) >= 70.0 },
            lowHumidityDays = days.count { (it.humidityAfternoon ?: 100.0) <= 35.0 },
            maxUv = days.maxOfOrNull { it.uvMax ?: 0.0 } ?: 0.0,
            windyDays = days.count { (it.windMax ?: 0.0) >= 8.0 }
        )
    }
}