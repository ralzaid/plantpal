package com.example.plantpal.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun needsWaterToday(plant: UiPlant): Boolean {
    val lastWatered = parseStoredDate(plant.lastWateredDate) ?: return true
    val daysSince = daysSince(lastWatered)
    return daysSince >= plant.wateringFrequencyDays
}

fun parseStoredDate(value: String): Date? {
    return runCatching {
        synchronized(storageDateFormat) {
            storageDateFormat.parse(value)
        }
    }.getOrNull()
}

fun formatStoredDate(value: String): String {
    val parsedDate = parseStoredDate(value) ?: return value
    return synchronized(displayDateFormat) {
        displayDateFormat.format(parsedDate)
    }
}

private fun daysSince(date: Date): Long {
    return TimeUnit.MILLISECONDS.toDays(Date().time - date.time)
}

val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
