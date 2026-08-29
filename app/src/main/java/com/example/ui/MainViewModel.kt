package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MlClinicalSynthesizer
import com.example.data.local.AppDatabase
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.FederatedNodeEntity
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.PredictionResultEntity
import com.example.data.repository.CardioNeuroRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardioNeuroRepository(AppDatabase.getInstance(application))

    val patients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val predictions: StateFlow<List<PredictionResultEntity>> = repository.allPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val federatedNodes: StateFlow<List<FederatedNodeEntity>> = repository.federatedNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPatient = MutableStateFlow<PatientEntity?>(null)
    val selectedPatient: StateFlow<PatientEntity?> = _selectedPatient.asStateFlow()

    private val _selectedPrediction = MutableStateFlow<PredictionResultEntity?>(null)
    val selectedPrediction: StateFlow<PredictionResultEntity?> = _selectedPrediction.asStateFlow()

    private val _selectedEncounter = MutableStateFlow<EncounterEntity?>(null)
    val selectedEncounter: StateFlow<EncounterEntity?> = _selectedEncounter.asStateFlow()

    private val _mlSummary = MutableStateFlow<String>("")
    val mlSummary: StateFlow<String> = _mlSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow<Boolean>(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    // Loading state for prediction detail UI
    private val _isLoadingPrediction = MutableStateFlow<Boolean>(false)
    val isLoadingPrediction: StateFlow<Boolean> = _isLoadingPrediction.asStateFlow()

    // Error state for prediction loading failures
    private val _predictionError = MutableStateFlow<String?>(null)
    val predictionError: StateFlow<String?> = _predictionError.asStateFlow()

    // Theme state (default: false for Day / Light Theme, toggleable to Night / Dark Theme)
    private val _isDarkTheme = MutableStateFlow<Boolean>(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    init {
        com.example.ai.MultimodalInferenceEngine.initialize(application)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun loadPatient(patientId: Long) {
        viewModelScope.launch {
            _selectedPatient.value = repository.getPatientById(patientId)
        }
    }

    fun loadPrediction(predictionId: Long) {
        viewModelScope.launch {
            _isLoadingPrediction.value = true
            _predictionError.value = null
            try {
                val pred = repository.getPredictionById(predictionId)
                _selectedPrediction.value = pred
                if (pred != null) {
                    val patient = repository.getPatientById(pred.patientId)
                    _selectedPatient.value = patient
                    val encounter = repository.getEncounterById(pred.encounterId)
                    _selectedEncounter.value = encounter
                    if (patient != null) {
                        generateMlSummary(patient, pred)
                    }
                }
            } catch (e: Exception) {
                _predictionError.value = e.message ?: "Failed to load prediction details"
            } finally {
                _isLoadingPrediction.value = false
            }
        }
    }

    private fun generateMlSummary(patient: PatientEntity, pred: PredictionResultEntity) {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            val encounter = EncounterEntity(
                patientId = patient.id,
                encounterDate = "Aug 04, 2026",
                chiefComplaint = "Multimodal ML Diagnostic Encounter",
                heartRateBpm = 82,
                respiratoryRate = 18,
                oxygenSatPct = 96,
                nihssScore = 4,
                troponinNgMl = 0.08f,
                ecgSignalPreset = pred.ecgArrhythmiaDetected,
                eegSignalPreset = "Alpha Wave Spectrum",
                mriSliceType = "T2 FLAIR Scan",
                clinicalNotes = "Integrated Multimodal Screening Note."
            )

            val summary = MlClinicalSynthesizer.generateTraditionalMlSynthesis(patient, encounter, pred)
            _mlSummary.value = summary
            _isGeneratingSummary.value = false
        }
    }

    fun addNewPatient(
        name: String,
        mrn: String,
        age: Int,
        gender: String,
        bloodGroup: String,
        primaryCondition: String,
        systolicBp: Int,
        diastolicBp: Int,
        cholesterol: Int,
        glucose: Int,
        bmi: Float,
        smoker: Boolean,
        diabetic: Boolean,
        familyHeart: Boolean,
        familyStroke: Boolean,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newId = repository.addPatient(
                PatientEntity(
                    name = name,
                    medicalRecordNumber = mrn,
                    age = age,
                    gender = gender,
                    bloodGroup = bloodGroup,
                    primaryCondition = primaryCondition,
                    systolicBp = systolicBp,
                    diastolicBp = diastolicBp,
                    cholesterolMgDl = cholesterol,
                    fastingGlucoseMgDl = glucose,
                    bmi = bmi,
                    smoker = smoker,
                    diabetic = diabetic,
                    familyHistoryHeartDisease = familyHeart,
                    familyHistoryStroke = familyStroke
                )
            )
            onSuccess(newId)
        }
    }

    fun runDiagnosticEncounter(
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
        age: Int,
        systolicBp: Int,
        diastolicBp: Int,
        cholesterol: Int,
        glucose: Int,
        bmi: Float,
        smoker: Boolean,
        familyHeart: Boolean,
        familyStroke: Boolean,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val (_, predictionId) = repository.runAndSaveDiagnosticEncounter(
                patientId = patientId,
                chiefComplaint = chiefComplaint,
                heartRate = heartRate,
                respiratoryRate = respiratoryRate,
                oxygenSat = oxygenSat,
                nihssScore = nihssScore,
                troponin = troponin,
                ecgPreset = ecgPreset,
                eegPreset = eegPreset,
                mriType = mriType,
                notes = notes,
                age = age,
                systolicBp = systolicBp,
                diastolicBp = diastolicBp,
                cholesterol = cholesterol,
                glucose = glucose,
                bmi = bmi,
                smoker = smoker,
                familyHeart = familyHeart,
                familyStroke = familyStroke
            )
            onComplete(predictionId)
        }
    }
}
