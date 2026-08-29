package com.example.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.EncounterEntity
import com.example.ai.MultimodalInferenceEngine
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class VerificationHarness {

    private val TAG = "VerificationHarness"

    private fun createPatientA(): PatientEntity {
        return PatientEntity(
            id = 0,
            name = "Test A",
            medicalRecordNumber = "MRN001",
            age = 25,
            gender = "Male",
            bloodGroup = "O+",
            primaryCondition = "Low Risk",
            systolicBp = 110,
            diastolicBp = 70,
            cholesterolMgDl = 145,
            fastingGlucoseMgDl = 82,
            bmi = 21f,
            smoker = false,
            diabetic = false,
            familyHistoryHeartDisease = false,
            familyHistoryStroke = false,
            createdTimestamp = System.currentTimeMillis()
        )
    }

    private fun createEncounterA(patientId: Long): EncounterEntity {
        return EncounterEntity(
            id = 0,
            patientId = patientId,
            encounterDate = "2023-01-01",
            chiefComplaint = "Routine Checkup",
            heartRateBpm = 62,
            respiratoryRate = 14,
            oxygenSatPct = 98,
            nihssScore = 0,
            troponinNgMl = 0.01f,
            ecgSignalPreset = "Normal Sinus",
            ecgFilePath = null,
            eegSignalPreset = "Normal Alpha Spectrum",
            eegFilePath = null,
            mriSliceType = "Axial T2-FLAIR",
            clinicalNotes = "All vitals normal.",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createPatientB(): PatientEntity {
        return PatientEntity(
            id = 0,
            name = "Test B",
            medicalRecordNumber = "MRN002",
            age = 78,
            gender = "Male",
            bloodGroup = "A+",
            primaryCondition = "High Risk",
            systolicBp = 190,
            diastolicBp = 115,
            cholesterolMgDl = 310,
            fastingGlucoseMgDl = 185,
            bmi = 34f,
            smoker = true,
            diabetic = true,
            familyHistoryHeartDisease = true,
            familyHistoryStroke = true,
            createdTimestamp = System.currentTimeMillis()
        )
    }

    private fun createEncounterB(patientId: Long): EncounterEntity {
        return EncounterEntity(
            id = 0,
            patientId = patientId,
            encounterDate = "2023-01-02",
            chiefComplaint = "Acute Symptoms",
            heartRateBpm = 120,
            respiratoryRate = 22,
            oxygenSatPct = 85,
            nihssScore = 12,
            troponinNgMl = 1.2f,
            ecgSignalPreset = "Atrial Fibrillation",
            ecgFilePath = null,
            eegSignalPreset = "Temporal Spike Wave",
            eegFilePath = null,
            mriSliceType = "Diffusion Weighted Imaging (DWI)",
            clinicalNotes = "Patient presents with high BP and arrhythmia.",
            timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun runVerification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MultimodalInferenceEngine.initialize(context)

        // Test A
        val patientA = createPatientA()
        val encounterA = createEncounterA(patientA.id)
        val resultA = MultimodalInferenceEngine.executePrediction(patientA, encounterA)
        Log.d(TAG, "=== Test A Result ===")
        Log.d(TAG, "--- Detailed Result A ---")
Log.d(TAG, "combinedRiskScorePct=" + resultA.combinedRiskScorePct)
Log.d(TAG, "riskSeverityCategory=" + resultA.riskSeverityCategory)
Log.d(TAG, "heartRiskScorePct=" + resultA.heartRiskScorePct)
Log.d(TAG, "heartDiagnosisLabel=" + resultA.heartDiagnosisLabel)
Log.d(TAG, "ecgAvailable=" + resultA.ecgAvailable)
Log.d(TAG, "ecgArrhythmiaDetected=" + resultA.ecgArrhythmiaDetected)
Log.d(TAG, "strokeRiskScorePct=" + resultA.strokeRiskScorePct)
Log.d(TAG, "strokeDiagnosisLabel=" + resultA.strokeDiagnosisLabel)
Log.d(TAG, "eegAvailable=" + resultA.eegAvailable)
Log.d(TAG, "eegSeizureRiskScorePct=" + resultA.eegSeizureRiskScorePct)
Log.d(TAG, "mriAvailable=" + resultA.mriAvailable)
Log.d(TAG, "mriInfarctDetected=" + resultA.mriInfarctDetected)
Log.d(TAG, "mriInfarctVolumeCc=" + resultA.mriInfarctVolumeCc)
Log.d(TAG, "gnnHeartBrainCrosstalkRiskIndex=" + resultA.gnnHeartBrainCrosstalkRiskIndex)
Log.d(TAG, "--- End Result A ---")
// Detailed logging moved after resultB is defined

        // Test B
        val patientB = createPatientB()
        val encounterB = createEncounterB(patientB.id)
        val resultB = MultimodalInferenceEngine.executePrediction(patientB, encounterB)
        Log.d(TAG, "=== Test B Result ===")
        Log.d(TAG, resultB.toString())
    }
}
