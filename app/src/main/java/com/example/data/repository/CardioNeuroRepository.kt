package com.example.data.repository

import com.example.ai.MultimodalInferenceEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.FederatedNodeEntity
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.PredictionResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CardioNeuroRepository(private val db: AppDatabase) {

    val allPatients: Flow<List<PatientEntity>> = db.patientDao().getAllPatients()
    val allEncounters: Flow<List<EncounterEntity>> = db.encounterDao().getAllEncounters()
    val allPredictions: Flow<List<PredictionResultEntity>> = db.predictionDao().getAllPredictions()
    val federatedNodes: Flow<List<FederatedNodeEntity>> = db.federatedNodeDao().getAllNodes()

    suspend fun getPatientById(id: Long): PatientEntity? = withContext(Dispatchers.IO) {
        db.patientDao().getPatientById(id)
    }

    suspend fun getEncounterById(id: Long): EncounterEntity? = withContext(Dispatchers.IO) {
        db.encounterDao().getEncounterById(id)
    }

    suspend fun getPredictionsForPatient(patientId: Long): Flow<List<PredictionResultEntity>> = withContext(Dispatchers.IO) {
        db.predictionDao().getPredictionsForPatient(patientId)
    }

    suspend fun getPredictionById(id: Long): PredictionResultEntity? = withContext(Dispatchers.IO) {
        db.predictionDao().getPredictionById(id)
    }

    suspend fun addPatient(patient: PatientEntity): Long = withContext(Dispatchers.IO) {
        db.patientDao().insertPatient(patient)
    }

    suspend fun runAndSaveDiagnosticEncounter(
        patientId: Long,
        chiefComplaint: String,
        heartRate: Int,
        respiratoryRate: Int,
        oxygenSat: Int,
        nihssScore: Int,
        troponin: Float,
        ecgPreset: String,
        eegPreset: String,
        mriType: String,
        notes: String,
        age: Int? = null,
        systolicBp: Int? = null,
        diastolicBp: Int? = null,
        cholesterol: Int? = null,
        glucose: Int? = null,
        bmi: Float? = null,
        smoker: Boolean? = null,
        familyHeart: Boolean? = null,
        familyStroke: Boolean? = null
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val originalPatient = db.patientDao().getPatientById(patientId)
            ?: throw IllegalArgumentException("Patient with ID $patientId not found")

        val patient = if (age != null || systolicBp != null || diastolicBp != null ||
            cholesterol != null || glucose != null || bmi != null || smoker != null ||
            familyHeart != null || familyStroke != null
        ) {
            val updated = originalPatient.copy(
                age = age ?: originalPatient.age,
                systolicBp = systolicBp ?: originalPatient.systolicBp,
                diastolicBp = diastolicBp ?: originalPatient.diastolicBp,
                cholesterolMgDl = cholesterol ?: originalPatient.cholesterolMgDl,
                fastingGlucoseMgDl = glucose ?: originalPatient.fastingGlucoseMgDl,
                bmi = bmi ?: originalPatient.bmi,
                smoker = smoker ?: originalPatient.smoker,
                familyHistoryHeartDisease = familyHeart ?: originalPatient.familyHistoryHeartDisease,
                familyHistoryStroke = familyStroke ?: originalPatient.familyHistoryStroke
            )
            db.patientDao().insertPatient(updated)
            updated
        } else {
            originalPatient
        }

        // Determine real files vs presets
        val ecgFile = java.io.File(ecgPreset.removePrefix("file://").removePrefix("file:"))
        val isRealEcg = ecgFile.exists() && ecgFile.isFile
        val ecgSignal = if (isRealEcg) "Uploaded ECG File" else ecgPreset
        val ecgPath = if (isRealEcg) ecgFile.absolutePath else null
        val ecgProv = if (isRealEcg) "REAL" else "DEMO/PRESET"

        val eegFile = java.io.File(eegPreset.removePrefix("file://").removePrefix("file:"))
        val isRealEeg = eegFile.exists() && eegFile.isFile
        val eegSignal = if (isRealEeg) "Uploaded EEG File" else eegPreset
        val eegPath = if (isRealEeg) eegFile.absolutePath else null
        val eegProv = if (isRealEeg) "REAL" else "DEMO/PRESET"

        val mriFile = java.io.File(mriType.removePrefix("file://").removePrefix("file:"))
        val isRealMri = mriFile.exists() && mriFile.isFile
        val mriSlice = if (isRealMri) mriFile.absolutePath else mriType
        val mriProv = if (isRealMri) "REAL" else "DEMO/PRESET"

        val encounter = EncounterEntity(
            patientId = patientId,
            encounterDate = "Aug 04, 2026",
            chiefComplaint = chiefComplaint,
            heartRateBpm = heartRate,
            respiratoryRate = respiratoryRate,
            oxygenSatPct = oxygenSat,
            nihssScore = nihssScore,
            troponinNgMl = troponin,
            ecgSignalPreset = ecgSignal,
            ecgFilePath = ecgPath,
            eegSignalPreset = eegSignal,
            eegFilePath = eegPath,
            mriSliceType = mriSlice,
            clinicalNotes = notes,
            ecgProvenance = ecgProv,
            eegProvenance = eegProv,
            mriProvenance = mriProv
        )

        val encounterId = db.encounterDao().insertEncounter(encounter)
        val encounterWithId = encounter.copy(id = encounterId)

        val prediction = MultimodalInferenceEngine.executePrediction(patient, encounterWithId)
        val predictionId = db.predictionDao().insertPrediction(prediction)

        Pair(encounterId, predictionId)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // Seed Federated Nodes
        db.federatedNodeDao().insertOrUpdateNode(
            FederatedNodeEntity(
                nodeId = "NODE_US_EAST_01",
                hospitalNodeName = "Mayo Clinical Sentinel Hub",
                status = "ONLINE",
                localEpochsCompleted = 142,
                privacyBudgetEpsilon = 0.5f,
                noiseScaleGamma = 0.02f,
                lastWeightHash = "0x8f1e...4a2c",
                lastSyncTimestamp = System.currentTimeMillis() - 1200000
            )
        )
        db.federatedNodeDao().insertOrUpdateNode(
            FederatedNodeEntity(
                nodeId = "NODE_EU_WEST_02",
                hospitalNodeName = "Charité Berlin Neuro-Cardio Unit",
                status = "SYNCHRONIZING",
                localEpochsCompleted = 198,
                privacyBudgetEpsilon = 0.4f,
                noiseScaleGamma = 0.015f,
                lastWeightHash = "0x3b7d...9e10",
                lastSyncTimestamp = System.currentTimeMillis() - 450000
            )
        )

        // Check if patients exist
        val existingPatients = db.patientDao().getPatientById(1)
        if (existingPatients == null) {
            // Seed Sample Patient 1 (High Risk Cardioembolic Stroke)
            val p1Id = db.patientDao().insertPatient(
                PatientEntity(
                    name = "Eleanor Vance",
                    medicalRecordNumber = "MRN-884920",
                    age = 68,
                    gender = "Female",
                    bloodGroup = "A+",
                    primaryCondition = "Paroxysmal AFib & Transient Ischemic Attack",
                    systolicBp = 162,
                    diastolicBp = 98,
                    cholesterolMgDl = 238,
                    fastingGlucoseMgDl = 142,
                    bmi = 28.4f,
                    smoker = true,
                    diabetic = true,
                    familyHistoryHeartDisease = true,
                    familyHistoryStroke = true
                )
            )

            runAndSaveDiagnosticEncounter(
                patientId = p1Id,
                chiefComplaint = "Sudden onset left-sided facial weakness and palpitations",
                heartRate = 118,
                respiratoryRate = 20,
                oxygenSat = 96,
                nihssScore = 8,
                troponin = 0.12f,
                ecgPreset = "Atrial Fibrillation",
                eegPreset = "Diffuse Slowing Ischemic",
                mriType = "Diffusion Weighted Imaging (DWI)",
                notes = "Patient reports sudden numbness in left arm and dysarthria lasting 45 mins. ECG shows irregularly irregular rhythm without P waves."
            )

            // Seed Sample Patient 2 (STEMI + Mild Neuro Risk)
            val p2Id = db.patientDao().insertPatient(
                PatientEntity(
                    name = "Marcus Holloway",
                    medicalRecordNumber = "MRN-773104",
                    age = 54,
                    gender = "Male",
                    bloodGroup = "O+",
                    primaryCondition = "Acute Coronary Syndrome & Hypertensive Urgency",
                    systolicBp = 175,
                    diastolicBp = 104,
                    cholesterolMgDl = 264,
                    fastingGlucoseMgDl = 110,
                    bmi = 31.2f,
                    smoker = true,
                    diabetic = false,
                    familyHistoryHeartDisease = true,
                    familyHistoryStroke = false
                )
            )

            runAndSaveDiagnosticEncounter(
                patientId = p2Id,
                chiefComplaint = "Substernal crushing chest pain radiating to left jaw",
                heartRate = 96,
                respiratoryRate = 22,
                oxygenSat = 95,
                nihssScore = 1,
                troponin = 0.85f,
                ecgPreset = "ST Elevation MI",
                eegPreset = "Normal Alpha Spectrum",
                mriType = "Axial T2-FLAIR",
                notes = "Acute ST segment elevation in leads V2-V5. Troponin strongly elevated. Immediate cardiac catheterization activation pending."
            )

            // Seed Sample Patient 3 (Normal Baseline / Low Risk)
            val p3Id = db.patientDao().insertPatient(
                PatientEntity(
                    name = "Sophia Chen",
                    medicalRecordNumber = "MRN-991203",
                    age = 42,
                    gender = "Female",
                    bloodGroup = "B+",
                    primaryCondition = "Routine Routine Cardiovascular Health Screen",
                    systolicBp = 118,
                    diastolicBp = 76,
                    cholesterolMgDl = 175,
                    fastingGlucoseMgDl = 88,
                    bmi = 22.1f,
                    smoker = false,
                    diabetic = false,
                    familyHistoryHeartDisease = false,
                    familyHistoryStroke = false
                )
            )

            runAndSaveDiagnosticEncounter(
                patientId = p3Id,
                chiefComplaint = "Annual wellness check & preventive health screening",
                heartRate = 68,
                respiratoryRate = 14,
                oxygenSat = 99,
                nihssScore = 0,
                troponin = 0.01f,
                ecgPreset = "Normal Sinus",
                eegPreset = "Normal Alpha Spectrum",
                mriType = "Axial T2-FLAIR",
                notes = "All vital signs within optimal medical parameters. No chest pain, focal deficits, or dyspnea."
            )
        }
    }
}
