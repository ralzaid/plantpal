package com.example.plantpal.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: PlantDatabase? = null

    fun getDatabase(context: Context): PlantDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PlantDatabase::class.java,
                "plantpal_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}