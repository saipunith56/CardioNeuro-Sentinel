package com.example.ai.wrappers

import android.util.Log
import com.example.BuildConfig

import android.content.Context
import ai.onnxruntime.OnnxTensor
import com.example.ai.OnnxModelManager
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.PatientEntity
import kotlin.math.exp

/**
 * Real ONNX wrapper for the DS2 clinical heart disease model.
 * Maps patient/encounter data, scales features, runs inference, and returns computed probability.
 */
object ClinicalDs2HeartModel {
    private const val MODEL_NAME = "ds2_heart_model.onnx"
    
    // Scale parameters from preproc_metadata_all.json
    private val MEANS = doubleArrayOf(
        53.76086956521739, 0.782608695652174, 3.25, 132.2888198757764,
        201.37111801242236, 0.15372670807453417, 0.6226708074534162, 137.5310559006211,
        0.37111801242236025, 0.8989130434782606, 1.843167701863354, 0.2080745341614907,
        5.591614906832298
    )
    
    private val SCALES = doubleArrayOf(
        9.49559998734323, 0.41247099915239727, 0.9297949416620702, 18.04541714601706,
        107.02480050714773, 0.36068657765309364, 0.8069082607841539, 25.17474268133459,
        0.4831039570092933, 1.0759433526113842, 0.5274454107460589, 0.6003596391397517,
        1.386265101792797
    )

    fun predict(context: Context, patient: PatientEntity, encounter: EncounterEntity): Float {
        val session = OnnxModelManager.getSession(context, MODEL_NAME)
        val env = OnnxModelManager.env
        
        // 1. Raw feature preparation
        val rawFeatures = DoubleArray(13)
        rawFeatures[0] = patient.age.toDouble()
        rawFeatures[1] = if (patient.gender.equals("Male", ignoreCase = true)) 1.0 else 0.0
        
        // cp: chest pain type
        rawFeatures[2] = when {
            encounter.chiefComplaint.contains("typical", ignoreCase = true) -> 1.0
            encounter.chiefComplaint.contains("atypical", ignoreCase = true) -> 2.0
            encounter.chiefComplaint.contains("non-anginal", ignoreCase = true) -> 3.0
            else -> 4.0 // asymptomatic / fallback
        }
        
        rawFeatures[3] = patient.systolicBp.toDouble()
        rawFeatures[4] = patient.cholesterolMgDl.toDouble()
        rawFeatures[5] = if (patient.fastingGlucoseMgDl > 120) 1.0 else 0.0
        
        // restecg
        rawFeatures[6] = when (encounter.ecgSignalPreset) {
            "Atrial Fibrillation" -> 1.0
            "ST Elevation MI" -> 2.0
            else -> 0.0
        }
        
        rawFeatures[7] = encounter.heartRateBpm.toDouble()
        
        // exang: exercise induced angina
        rawFeatures[8] = if (patient.primaryCondition.contains("angina", ignoreCase = true)) 1.0 else 0.0
        
        // oldpeak: ST depression
        rawFeatures[9] = when (encounter.ecgSignalPreset) {
            "ST Elevation MI" -> 2.5
            "VTach" -> 1.5
            else -> 0.0
        }
        
        rawFeatures[10] = if (encounter.ecgSignalPreset == "ST Elevation MI") 1.0 else 2.0 // slope
        rawFeatures[11] = 0.0 // ca: number of major vessels
        rawFeatures[12] = 6.0 // thal: default thalassemia

        if (BuildConfig.DEBUG) {
            Log.d("ClinicalDs2HeartModel", "Raw features: ${rawFeatures.contentToString()}")
        }

        // 2. Standardization
        val preprocessed = FloatArray(13)
        for (i in 0..12) {
            preprocessed[i] = ((rawFeatures[i] - MEANS[i]) / SCALES[i]).toFloat()
        }

        if (BuildConfig.DEBUG) {
            Log.d("ClinicalDs2HeartModel", "Preprocessed features: ${preprocessed.contentToString()}")
        }

        // Wrap into [1, 13] 2D array representation
        val inputData = Array(1) { preprocessed }

        if (BuildConfig.DEBUG) {
            Log.d("ClinicalDs2HeartModel", "ONNX input name='input', shape=${inputData.size}x${inputData[0].size}")
        }

        // 3. Execution
        val inputTensor = OnnxTensor.createTensor(env, inputData)
        inputTensor.use {
            val inputs = mapOf("input" to inputTensor)
            session.run(inputs).use { results ->
                val output = results[0]
                val value = when (val valObj = output.value) {
                    is FloatArray -> valObj[0]
                    is Array<*> -> {
                        val firstRow = valObj[0]
                        if (firstRow is FloatArray) {
                            firstRow[0]
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            val raw2D = valObj as Array<FloatArray>
                            raw2D[0][0]
                        }
                    }
                    else -> throw IllegalStateException("Unexpected output tensor value type: ${valObj?.javaClass?.name}")
                }
                
                if (BuildConfig.DEBUG) {
                    Log.d("ClinicalDs2HeartModel", "Raw ONNX output (logit)=$value")
                }

                // TabularNN outputs raw logit. Pass through Sigmoid.
                val prob = 1.0f / (1.0f + exp(-value))
                if (BuildConfig.DEBUG) {
                    Log.d("ClinicalDs2HeartModel", "Final probability=$prob")
                }
                return prob
            }
        }
    }
}
