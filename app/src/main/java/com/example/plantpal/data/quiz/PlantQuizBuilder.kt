package com.example.plantpal.data.quiz

import com.example.plantpal.quiz.BinaryQuizNode
import com.example.plantpal.quiz.QuizAction
import com.example.plantpal.quiz.QuizWrite

object PlantQuizBuilder {

    fun build(): Map<String, BinaryQuizNode> {
        return listOf(
            BinaryQuizNode(
                id = "appearance_change",
                question = "Have you noticed any recent change in the plant’s appearance?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("screen", "appearance_change", "no")
                    ),
                    nextId = "soil_dry"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("screen", "appearance_change", "yes")
                    ),
                    nextId = "yellowing"
                )
            ),
            BinaryQuizNode(
                id = "yellowing",
                question = "Are the leaves turning yellow or fading?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "yellowing", "no")
                    ),
                    nextId = "wilting"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "yellowing", "yes"),
                        QuizWrite.HealthSymptom("Leaf yellowing")
                    ),
                    nextId = "wilting"
                )
            ),
            BinaryQuizNode(
                id = "wilting",
                question = "Is the plant drooping or wilting?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "wilting", "no")
                    ),
                    nextId = "brown_scorch"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "wilting", "yes"),
                        QuizWrite.HealthSymptom("Wilting or drooping")
                    ),
                    nextId = "brown_scorch"
                )
            ),
            BinaryQuizNode(
                id = "brown_scorch",
                question = "Do you see brown edges or scorched areas on leaves?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "brown_scorch", "no")
                    ),
                    nextId = "blackening"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "brown_scorch", "yes"),
                        QuizWrite.HealthSymptom("Brown edges or scorch")
                    ),
                    nextId = "blackening"
                )
            ),
            BinaryQuizNode(
                id = "blackening",
                question = "Do you see blackened leaves or shoots?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "blackening", "no")
                    ),
                    nextId = "soil_dry"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("symptom", "blackening", "yes"),
                        QuizWrite.HealthSymptom("Leaf or shoot blackening")
                    ),
                    nextId = "soil_dry"
                )
            ),
            BinaryQuizNode(
                id = "soil_dry",
                question = "Does the soil feel dry right now?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "soil_dry", "no")
                    ),
                    nextId = "soil_soggy"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "soil_dry", "yes"),
                        QuizWrite.SoilCondition("dry")
                    ),
                    nextId = "watered_recently"
                )
            ),
            BinaryQuizNode(
                id = "soil_soggy",
                question = "Does the soil feel soggy or waterlogged?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "soil_soggy", "no"),
                        QuizWrite.SoilCondition("moist")
                    ),
                    nextId = "watered_recently"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "soil_soggy", "yes"),
                        QuizWrite.SoilCondition("soggy")
                    ),
                    nextId = "watered_recently"
                )
            ),
            BinaryQuizNode(
                id = "watered_recently",
                question = "Have you watered this plant recently?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "watered_recently", "no"),
                        QuizWrite.WateredRecently(false)
                    ),
                    nextId = "light_changed"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("water", "watered_recently", "yes"),
                        QuizWrite.WateredRecently(true)
                    ),
                    nextId = "light_changed"
                )
            ),
            BinaryQuizNode(
                id = "light_changed",
                question = "Has the plant been getting more direct light than usual?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("light", "more_direct_light", "no")
                    ),
                    nextId = "environment_changed"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("light", "more_direct_light", "yes")
                    ),
                    nextId = "environment_changed"
                )
            ),
            BinaryQuizNode(
                id = "environment_changed",
                question = "Has the plant been moved or kept in a different environment recently?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("environment", "environment_changed", "no")
                    ),
                    nextId = "cold_exposure"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("environment", "environment_changed", "yes"),
                        QuizWrite.EnvironmentFlag("Moved or changed environment")
                    ),
                    nextId = "cold_exposure"
                )
            ),
            BinaryQuizNode(
                id = "cold_exposure",
                question = "Has it been exposed to cold drafts or low temperatures?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("environment", "cold_exposure", "no")
                    ),
                    nextId = "visible_pests"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("environment", "cold_exposure", "yes"),
                        QuizWrite.EnvironmentFlag("Cold exposure")
                    ),
                    nextId = "visible_pests"
                )
            ),
            BinaryQuizNode(
                id = "visible_pests",
                question = "Do you see pests, webbing, mold, or white cotton-like growth?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "visible_pests", "no")
                    ),
                    nextId = "powdery_coating"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "visible_pests", "yes")
                    ),
                    nextId = "webbing"
                )
            ),
            BinaryQuizNode(
                id = "webbing",
                question = "Do you see webbing or tiny dots on the leaves?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "webbing", "no")
                    ),
                    nextId = "cottony_growth"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "webbing", "yes"),
                        QuizWrite.HealthSymptom("Spider mite-like webbing")
                    ),
                    nextId = "cottony_growth"
                )
            ),
            BinaryQuizNode(
                id = "cottony_growth",
                question = "Do you see white cotton-like insects or waxy growth?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "cottony_growth", "no")
                    ),
                    nextId = "powdery_coating"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("pest", "cottony_growth", "yes"),
                        QuizWrite.HealthSymptom("Mealybug-like cottony growth")
                    ),
                    nextId = "powdery_coating"
                )
            ),
            BinaryQuizNode(
                id = "powdery_coating",
                question = "Do you see a white powdery coating on the leaves?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("disease", "powdery_coating", "no")
                    ),
                    nextId = "gray_growth"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("disease", "powdery_coating", "yes"),
                        QuizWrite.HealthSymptom("Powdery mildew-like coating")
                    ),
                    nextId = "gray_growth"
                )
            ),
            BinaryQuizNode(
                id = "gray_growth",
                question = "Do you see fuzzy gray growth on leaves or flowers?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("disease", "gray_growth", "no")
                    ),
                    nextId = "fertilized_recently"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("disease", "gray_growth", "yes"),
                        QuizWrite.HealthSymptom("Gray mold-like growth")
                    ),
                    nextId = "fertilized_recently"
                )
            ),
            BinaryQuizNode(
                id = "fertilized_recently",
                question = "Have you fertilized this plant recently?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("nutrient", "fertilized_recently", "no")
                    ),
                    nextId = "salt_buildup"
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("nutrient", "fertilized_recently", "yes")
                    ),
                    nextId = "salt_buildup"
                )
            ),
            BinaryQuizNode(
                id = "salt_buildup",
                question = "Do you see white crust or salt buildup on the soil or pot?",
                leftAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("nutrient", "salt_buildup", "no")
                    ),
                    nextId = null
                ),
                rightAction = QuizAction.RecordAndNext(
                    writes = listOf(
                        QuizWrite.Observation("nutrient", "salt_buildup", "yes"),
                        QuizWrite.HealthSymptom("Possible salt buildup")
                    ),
                    nextId = null
                )
            )
        ).associateBy { it.id }
    }
}