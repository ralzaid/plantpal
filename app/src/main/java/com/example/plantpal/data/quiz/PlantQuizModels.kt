package com.example.plantpal.quiz

data class BinaryQuizNode(
    val id: String,
    val question: String,
    val leftLabel: String = "No",
    val rightLabel: String = "Yes",
    val leftAction: QuizAction,
    val rightAction: QuizAction
)

sealed class QuizAction {
    data class RecordAndNext(
        val writes: List<QuizWrite>,
        val nextId: String?
    ) : QuizAction()
}

sealed class QuizWrite {
    data class Observation(
        val category: String,
        val key: String,
        val value: String
    ) : QuizWrite()

    data class SoilCondition(
        val soilCondition: String
    ) : QuizWrite()

    data class WateredRecently(
        val watered: Boolean
    ) : QuizWrite()

    data class HealthSymptom(
        val symptom: String
    ) : QuizWrite()

    data class EnvironmentFlag(
        val label: String
    ) : QuizWrite()
}