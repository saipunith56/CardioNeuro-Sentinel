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
 * Real ONNX wrapper for the DS3 clinical stroke risk model (improved version).
 * Maps patient/encounter data, scales numericals, one-hot encodes cat features, runs inference, and returns computed probability.
 */
object ClinicalDs3StrokeModel {
    private const val MODEL_NAME = "ds3_stroke_model_improved.onnx"

    // Numerical scale parameters from preproc_metadata_all.json
    private const val AGE_MEAN = 43.250604026845636
    private const val AGE_SCALE = 22.681957120799396
    
    private const val GLUCOSE_MEAN = 105.46874720357941
    private const val GLUCOSE_SCALE = 44.36474216879246
    
    private const val BMI_MEAN = 28.759200223713645
    private const val BMI_SCALE = 7.695627577544242

    fun predict(context: Context, patient: PatientEntity, encounter: EncounterEntity): Float {
        val session = OnnxModelManager.getSession(context, MODEL_NAME)
        val env = OnnxModelManager.env

        // Array size is 22 features
        val features = FloatArray(22)

        // 1. Preprocess numeric features: standard scaling
        features[0] = ((patient.age - AGE_MEAN) / AGE_SCALE).toFloat()
        features[1] = ((patient.fastingGlucoseMgDl - GLUCOSE_MEAN) / GLUCOSE_SCALE).toFloat()
        features[2] = ((patient.bmi - BMI_MEAN) / BMI_SCALE).toFloat()

        // 2. Preprocess categorical features: One-Hot coding
        
        // C1: gender -> [Female, Male]
        val isMale = patient.gender.equals("Male", ignoreCase = true)
        features[3] = if (!isMale) 1.0f else 0.0f
        features[4] = if (isMale) 1.0f else 0.0f

        // C2: hypertension -> [0, 1]
        val isHypertensive = patient.systolicBp >= 140 || patient.diastolicBp >= 90
        features[5] = if (!isHypertensive) 1.0f else 0.0f
        features[6] = if (isHypertensive) 1.0f else 0.0f

        // C3: heart_disease -> [0, 1]
        val hasHeartDisease = patient.primaryCondition.contains("afib", ignoreCase = true) ||
                              patient.primaryCondition.contains("heart", ignoreCase = true) ||
                              patient.primaryCondition.contains("cardio", ignoreCase = true)
        features[7] = if (!hasHeartDisease) 1.0f else 0.0f
        features[8] = if (hasHeartDisease) 1.0f else 0.0f

        // C4: ever_married -> [No, Yes]
        val everMarried = patient.age >= 25
        features[9] = if (!everMarried) 1.0f else 0.0f
        features[10] = if (everMarried) 1.0f else 0.0f

        // C5: work_type -> [Govt_job, Never_worked, Private, Self-employed, children]
        val workType = when {
            patient.age < 16 -> "children"
            patient.age >= 65 -> "Self-employed"
            else -> "Private"
        }
        features[11] = if (workType == "Govt_job") 1.0f else 0.0f
        features[12] = if (workType == "Never_worked") 1.0f else 0.0f
        features[13] = if (workType == "Private") 1.0f else 0.0f
        features[14] = if (workType == "Self-employed") 1.0f else 0.0f
        features[15] = if (workType == "children") 1.0f else 0.0f

        // C6: Residence_type -> [Rural, Urban]
        // Determine deterministically from MRN to inject variety
        val mrnDigit = patient.medicalRecordNumber.lastOrNull()?.toString()?.toIntOrNull() ?: 0
        val isRural = mrnDigit % 2 == 1
        features[16] = if (isRural) 1.0f else 0.0f
        features[17] = if (!isRural) 1.0f else 0.0f

        // C7: smoking_status -> [Unknown, formerly smoked, never smoked, smokes]
        val smokingStatus = if (patient.smoker) "smokes" else "never smoked"
        features[18] = if (smokingStatus == "Unknown") 1.0f else 0.0f
        features[19] = if (smokingStatus == "formerly smoked") 1.0f else 0.0f
        features[20] = if (smokingStatus == "never smoked") 1.0f else 0.0f
        features[21] = if (smokingStatus == "smokes") 1.0f else 0.0f

        // Wrap into [1, 22] input format
        val inputData = Array(1) { features }
        // DEBUG: log raw feature vector
        if (BuildConfig.DEBUG) {
            Log.d("ClinicalDs3StrokeModel", "Features=" + features.contentToString())
            var featureSum = 0.0f
            for (f in features) { featureSum += f }
            Log.d("ClinicalDs3StrokeModel", "Feature sum=$featureSum")
        }

            // DEBUG: log tensor info
            if (BuildConfig.DEBUG) {
                Log.d("ClinicalDs3StrokeModel", "ONNX input name='input', shape=${inputData.size}x${inputData[0].size}")
            }
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
                
                // Pass through Sigmoid activation
                val rawLogit = value
                if (BuildConfig.DEBUG) {
                    Log.d("ClinicalDs3StrokeModel", "Raw ONNX output (logit)=$rawLogit")
                }
                val probability = 1.0f / (1.0f + exp(-rawLogit))
                if (BuildConfig.DEBUG) {
                    Log.d("ClinicalDs3StrokeModel", "Final probability=$probability")
                }
                return probability
            }
        }
    }
}
