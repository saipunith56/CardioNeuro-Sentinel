package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val medicalRecordNumber: String,
    val age: Int,
    val gender: String, // Male, Female, Other
    val bloodGroup: String, // A+, O+, B-, AB+, etc.
    val primaryCondition: String, // e.g., "Hypertension & Atrial Fibrillation Risk"
    val systolicBp: Int,
    val diastolicBp: Int,
    val cholesterolMgDl: Int,
    val fastingGlucoseMgDl: Int,
    val bmi: Float,
    val smoker: Boolean,
    val diabetic: Boolean,
    val familyHistoryHeartDisease: Boolean,
    val familyHistoryStroke: Boolean,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val encounterDate: String,
    val chiefComplaint: String,
    val heartRateBpm: Int,
    val respiratoryRate: Int,
    val oxygenSatPct: Int,
    val nihssScore: Int, // National Institutes of Health Stroke Scale (0-42)
    val troponinNgMl: Float, // Cardiac biomarker
    val ecgSignalPreset: String, // "Normal Sinus", "ST Elevation MI", "Atrial Fibrillation", "VTach"
    val ecgFilePath: String? = null, // real ECG file / report image path
    val eegSignalPreset: String, // "Normal Alpha Spectrum", "Temporal Spike Wave", "Diffuse Slowing Ischemic"
    val eegFilePath: String? = null, // real EEG file / report image path
    val mriSliceType: String, // "Axial T2-FLAIR", "Diffusion Weighted Imaging (DWI)", "CT Angiography", or file path
    val clinicalNotes: String,
    val ecgProvenance: String = "DEMO/PRESET",
    val eegProvenance: String = "DEMO/PRESET",
    val mriProvenance: String = "DEMO/PRESET",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prediction_results")
data class PredictionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encounterId: Long,
    val patientId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Overall Multimodal Risk Score (0 - 100)
    val combinedRiskScorePct: Int, // e.g. 78%
    val ecgAvailable: Boolean,
    val eegAvailable: Boolean,
    val mriAvailable: Boolean,
    val riskSeverityCategory: String, // "CRITICAL", "HIGH", "MODERATE", "LOW"
    
    // Cardiovascular Breakdown
    val heartRiskScorePct: Int,
    val heartDiagnosisLabel: String, // "Coronary Artery Disease with AFib"
    val ecgArrhythmiaDetected: String, // "Atrial Fibrillation with ST Depression"
    val acuteCardiacRiskScorePct: Int = 0, // Estimated Acute Cardiac Risk
    
    // Neurological Breakdown
    val strokeRiskScorePct: Int,
    val strokeDiagnosisLabel: String, // "Ischemic Stroke - Cardioembolic Subtype"
    val toastSubtypeClassification: String, // "Large-Artery Atherosclerosis", "Cardioembolism", "Small-Vessel Occlusion"
    val eegSeizureRiskScorePct: Int,
    val mriInfarctDetected: Boolean,
    val mriInfarctVolumeCc: Float,
    
    // Multimodal GNN Fusion
    val gnnHeartBrainCrosstalkRiskIndex: Float, // 0.0 to 1.0 (e.g. 0.84)
    val crosstalkExplanation: String,
    
    // Provenance Tracking
    val ecgProvenance: String = "NOT PROVIDED",
    val eegProvenance: String = "NOT PROVIDED",
    val mriProvenance: String = "NOT PROVIDED",
    val extractedReportDataJson: String = "{}",
    
    // Explainable AI (XAI)
    val shapTopRiskFactorsJson: String, // JSON list of feature weights
    val limeExplanationJson: String, // Local features impact
    val gradCamHeatmapCoordinatesJson: String, // Lesion focal coordinates
    
    // Recommendations
    val clinicalRecommendationsJson: String
)

@Entity(tableName = "federated_nodes")
data class FederatedNodeEntity(
    @PrimaryKey val nodeId: String,
    val hospitalNodeName: String,
    val status: String, // "ONLINE", "TRAINING", "SYNCHRONIZING"
    val localEpochsCompleted: Int,
    val privacyBudgetEpsilon: Float,
    val noiseScaleGamma: Float,
    val lastWeightHash: String,
    val lastSyncTimestamp: Long
)
