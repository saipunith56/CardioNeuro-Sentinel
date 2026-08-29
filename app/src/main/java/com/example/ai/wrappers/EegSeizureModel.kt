package com.example.ai.wrappers

import android.util.Log
import com.example.BuildConfig

import android.content.Context
import ai.onnxruntime.OnnxTensor
import com.example.ai.OnnxModelManager
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.PatientEntity
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

/**
 * Real ONNX wrapper for the EEG seizure risk model.
 * Synthesizes a 23-channel x 256-sample EEG signal based on encounter presets,
 * normalizes the signal channel-wise using reference dataset stats, executes the ONNX session,
 * and extracts the Sigmoid-filtered seizure risk probability.
 */
object EegSeizureModel {
    private const val MODEL_NAME = "ds5_eeg_seizure_model.onnx"

    // Reference channel-wise mean and std from preproc_metadata_all.json
    private val CHANNEL_MEANS = floatArrayOf(
        0.29829457f, 0.26730454f, 0.29766124f, 0.32641965f, 0.28410074f,
        0.32961127f, 0.2110896f, 0.32765543f, 0.3105496f, 0.11848068f,
        0.1889936f, 0.3011288f, 0.3599409f, 0.15705267f, 0.22075565f,
        0.2527106f, 0.19384961f, 0.15494467f, 0.15040529f, 0.31396577f,
        0.2507688f, 0.16049582f, 0.14065343f
    )

    private val CHANNEL_STDS = floatArrayOf(
        90.2488f, 91.2393f, 84.59465f, 77.516846f, 83.607864f,
        91.66436f, 69.096786f, 74.27618f, 86.86163f, 71.26578f,
        75.313774f, 78.20293f, 81.153336f, 78.81021f, 70.22025f,
        71.30598f, 80.89816f, 60.88616f, 85.003624f, 76.82047f,
        87.6042f, 66.57443f, 64.5862f
    )

    fun predict(context: Context, patient: PatientEntity, encounter: EncounterEntity): Float {
        val session = OnnxModelManager.getSession(context, MODEL_NAME)
        val env = OnnxModelManager.env

        // 1. Synthesize raw 23-channel EEG signal (23 leads x 256 samples at 256Hz)
        val rawEEG = Array(23) { FloatArray(256) }
        val sampleRate = 256.0

        for (c in 0 until 23) {
            for (t in 0 until 256) {
                // Baseline minor high-frequency resting activity (beta-ish noise)
                var signal = sin(2 * PI * 20.0 * t / sampleRate) * 5.0
                
                when (encounter.eegSignalPreset) {
                    "Temporal Spike Wave" -> {
                        // Temporal channels (8 - 15) exhibit epileptic spike-and-wave discharges
                        if (c in 8..15) {
                            // 3 Hz periodic spike wave: saw-tooth spike superimposed on a slow wave
                            val phase = (t / sampleRate) % (1.0 / 3.0) // 3Hz period
                            val spike = if (phase < 0.05) {
                                // Sharp spike rise
                                (0.05 - phase) * 20.0 * 180.0
                            } else {
                                -15.0
                            }
                            val slowWave = sin(2 * PI * 3.0 * t / sampleRate) * 90.0
                            signal += (spike + slowWave)
                        } else {
                            // Occipital normal alpha rhythm
                            if (c >= 16) {
                                signal += sin(2 * PI * 10.0 * t / sampleRate) * 15.0
                            }
                        }
                    }
                    "Diffuse Slowing Ischemic" -> {
                        // General slowing: delta wave (2 Hz) on all channels
                        signal += sin(2 * PI * 2.0 * t / sampleRate) * 60.0
                    }
                    else -> {
                        // Normal resting EEG: Occipital leads (16-22) show alpha rhythm (10 Hz)
                        if (c >= 16) {
                            signal += sin(2 * PI * 10.0 * t / sampleRate) * 25.0
                        }
                    }
                }
                rawEEG[c][t] = signal.toFloat()
            }
        }

        if (BuildConfig.DEBUG) {
            // Log basic stats of the raw EEG tensor
            val rawSum = rawEEG.flatMap { it.asList() }.sum()
            Log.d("EegSeizureModel", "Raw EEG generated: shape=${rawEEG.size}x${rawEEG[0].size}, sum=$rawSum")
        }

        // 2. Normalize signal channel-wise using reference statistics
        val preprocessed = Array(23) { FloatArray(256) }
        for (c in 0 until 23) {
            val mean = CHANNEL_MEANS[c]
            val std = CHANNEL_STDS[c]
            for (t in 0 until 256) {
                preprocessed[c][t] = (rawEEG[c][t] - mean) / std
            }
        }

        if (BuildConfig.DEBUG) {
            // Log a checksum of preprocessed data
            val preSum = preprocessed.flatMap { it.asList() }.sum()
            Log.d("EegSeizureModel", "Preprocessed EEG: shape=${preprocessed.size}x${preprocessed[0].size}, sum=$preSum")
        }

        // Wrap to batch dimension: shape [1, 23, 256]
        val inputData = Array(1) { preprocessed }

        if (BuildConfig.DEBUG) {
            Log.d("EegSeizureModel", "ONNX input name='input', shape=${inputData.size}x${inputData[0].size}x${inputData[0][0].size}")
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
                    Log.d("EegSeizureModel", "Raw ONNX output (logit)=$value")
                }

                // Return prediction probability
                return 1.0f / (1.0f + exp(-value))
            }
        }
    }
}
