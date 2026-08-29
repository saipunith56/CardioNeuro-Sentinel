package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.EncounterDao
import com.example.data.local.dao.FederatedNodeDao
import com.example.data.local.dao.PatientDao
import com.example.data.local.dao.PredictionDao
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.FederatedNodeEntity
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.PredictionResultEntity

@Database(
    entities = [
        PatientEntity::class,
        EncounterEntity::class,
        PredictionResultEntity::class,
        FederatedNodeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun encounterDao(): EncounterDao
    abstract fun predictionDao(): PredictionDao
    abstract fun federatedNodeDao(): FederatedNodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardioneuro_sentinel.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
