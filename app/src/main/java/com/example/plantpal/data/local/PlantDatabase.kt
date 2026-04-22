package com.example.plantpal.data.local

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PlantEntity::class,
        WateringLogEntity::class,
        ConditionLogEntity::class,
        HealthCheckEntity::class,
        EnvironmentLogEntity::class,
        ReminderEntity::class,
        PlantObservationEntity::class,
        DailyWeatherEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao
    abstract fun conditionLogDao(): ConditionLogDao
    abstract fun healthCheckDao(): HealthCheckDao
    abstract fun environmentLogDao(): EnvironmentLogDao
    abstract fun reminderDao(): ReminderDao
    abstract fun plantObservationDao(): PlantObservationDao

    abstract fun dailyWeatherDao(): DailyWeatherDao

    companion object {
        @Volatile
        private var INSTANCE: PlantDatabase? = null

        fun getDatabase(app: Application): PlantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    app,
                    PlantDatabase::class.java,
                    "plantpal_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
