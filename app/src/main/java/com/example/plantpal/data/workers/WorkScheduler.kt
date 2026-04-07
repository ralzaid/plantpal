package com.example.plantpal.data.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

fun scheduleReminderWorker(
    context: Context,
    userId: Int
) {
    val inputData = workDataOf(
        PlantReminderWorker.KEY_USER_ID to userId
    )

    val request =
        PeriodicWorkRequestBuilder<PlantReminderWorker>(24, TimeUnit.HOURS)
            .setInputData(inputData)
            .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "plant_reminders_user_$userId",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}