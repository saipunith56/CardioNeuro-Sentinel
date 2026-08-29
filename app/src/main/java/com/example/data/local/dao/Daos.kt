package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.FederatedNodeEntity
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.PredictionResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY createdTimestamp DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatientById(id: Long)
}

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getEncountersForPatient(patientId: Long): Flow<List<EncounterEntity>>

    @Query("SELECT * FROM encounters ORDER BY timestamp DESC")
    fun getAllEncounters(): Flow<List<EncounterEntity>>

    @Query("SELECT * FROM encounters WHERE id = :id")
    suspend fun getEncounterById(id: Long): EncounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(encounter: EncounterEntity): Long
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM prediction_results ORDER BY timestamp DESC")
    fun getAllPredictions(): Flow<List<PredictionResultEntity>>

    @Query("SELECT * FROM prediction_results WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getPredictionsForPatient(patientId: Long): Flow<List<PredictionResultEntity>>

    @Query("SELECT * FROM prediction_results WHERE encounterId = :encounterId LIMIT 1")
    suspend fun getPredictionForEncounter(encounterId: Long): PredictionResultEntity?

    @Query("SELECT * FROM prediction_results WHERE id = :id")
    suspend fun getPredictionById(id: Long): PredictionResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionResultEntity): Long
}

@Dao
interface FederatedNodeDao {
    @Query("SELECT * FROM federated_nodes")
    fun getAllNodes(): Flow<List<FederatedNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: FederatedNodeEntity)
}
