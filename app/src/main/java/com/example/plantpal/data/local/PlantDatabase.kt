package com.example.plantpal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import android.app.Application
import androidx.room.Room


@Database(
    entities = [PlantEntity::class, WateringLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao
 companion object {
    @Volatile
    private var INSTANCE: PlantDatabase? = null

    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE plants ADD COLUMN imageUrl TEXT")
        }
    }

    fun getDatabase(app: Application): PlantDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                app,
                PlantDatabase::class.java,
                "plantpal_db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
}