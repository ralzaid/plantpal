package com.example.plantpal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE plantId = :plantId ORDER BY reminderDate ASC")
    fun getRemindersForPlant(plantId: Int): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insertReminder(reminder: ReminderEntity)
}