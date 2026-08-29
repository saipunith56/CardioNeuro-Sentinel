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
import kotlin.math.sqrt
import kotlin.math.PI


/**
 * Real ONNX wrapper for the ECG multi-label diagnosis model.
 * Parsons a 12-lead x 1000-sample raw ECG CSV file,
 * applies a digital bandpass filter (0.5Hz - 40Hz) and temporal lead-wise z-score normalization,
 * executes the 1D ResNet model, and returns probabilities for: [NORM, MI, STTC, CD, HYP].
 */
object EcgModel {
    private const val MODEL_NAME = "ds4_ecg_model.onnx"

    // Coefficients calculation from SciPy butter(4, [0.5/50.0, 40.0/50.0], btype='band')
    private val filterB = doubleArrayOf(
        0.41433450842934577, 0.0, -1.657338033717383, 0.0,
        2.4860070505760747, 0.0, -1.657338033717383, 0.0,
        0.41433450842934577
    )
    private val filterA = doubleArrayOf(
        1.0, -1.5531413924368365, -1.2001834773273905, 1.8641081944818034,
        1.382401905539973, -1.1805056337537856, -0.750590630607623,
        0.26621212842669784, 0.17170549898199286
    )

    fun predict(context: Context, patient: PatientEntity, encounter: EncounterEntity): FloatArray {
        val session = OnnxModelManager.getSession(context, MODEL_NAME)
        val env = OnnxModelManager.env

        // Use real ECG file path if provided; otherwise fallback to preset handling
        val filePath = encounter.ecgFilePath
        val file = if (!filePath.isNullOrBlank()) java.io.File(filePath) else null
        val isRealFile = file?.exists() == true && file.isFile
        if (BuildConfig.DEBUG) {
            Log.d("EcgModel", "ECG filePath=${encounter.ecgFilePath}, isRealFile=$isRealFile")
        }

        // 1. Ingest Raw Waveforms
        if (!isRealFile) {
            throw IllegalArgumentException("ECG file not provided or does not exist.")
        }
        val rawECG = parseEcgCsv(file!!)


        // 2. Preprocess: Bandpass filter (0.5Hz - 40Hz) matching SciPy lfilter
        val filteredECG = Array(12) { FloatArray(1000) }
        for (c in 0 until 12) {
            filteredECG[c] = lfilter(filterB, filterA, rawECG[c])
        }

        // 3. Preprocess: Z-score normalization matching python
        val preprocessed = Array(12) { FloatArray(1000) }
        for (c in 0 until 12) {
            var sum = 0.0f
            for (t in 0 until 1000) sum += filteredECG[c][t]
            val mean = sum / 1000f

            var varSum = 0.0f
            for (t in 0 until 1000) {
                val diff = filteredECG[c][t] - mean
                varSum += diff * diff
            }
            val std = sqrt(varSum / 1000f) + 1e-8f

            for (t in 0 until 1000) {
                preprocessed[c][t] = (filteredECG[c][t] - mean) / std
            }
        }

        // Wrap for ONNX: shape [1, 12, 1000]
        val inputData = Array(1) { preprocessed }

        // 4. ONNX Execution
        val inputTensor = OnnxTensor.createTensor(env, inputData)
        inputTensor.use {
            val inputs = mapOf("input" to inputTensor)
            session.run(inputs).use { results ->
                val output = results[0]
                val logits = when (val valObj = output.value) {
                    is FloatArray -> valObj
                    is Array<*> -> {
                        val firstRow = valObj[0]
                        if (firstRow is FloatArray) {
                            firstRow
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            val raw2D = valObj as Array<FloatArray>
                            raw2D[0]
                        }
                    }
                    else -> throw IllegalStateException("Unexpected output tensor value type: ${valObj?.javaClass?.name}")
                }
                // DEBUG: log logits
                if (BuildConfig.DEBUG) {
                    Log.d("EcgModel", "Logits=" + logits.contentToString())
                }
                // Return 5 Sigmoid probabilities
                val probabilities = FloatArray(5)
                for (i in 0 until 5) {
                    probabilities[i] = 1.0f / (1.0f + exp(-logits[i]))
                }
                // DEBUG: log probabilities
                if (BuildConfig.DEBUG) {
                    Log.d("EcgModel", "Probabilities=" + probabilities.contentToString())
                }
                return probabilities
            }
        }
    }

    private fun parseEcgCsv(file: java.io.File): Array<FloatArray> {
        val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Empty CSV file")
        }

        // Header detection: check first line
        val firstLine = lines.first()
        val firstRowParts = firstLine.split(",")
        val hasHeader = firstRowParts.any { part ->
            part.trim().any { it.isLetter() }
        }

        val dataLines = if (hasHeader) lines.drop(1) else lines
        val numSamples = dataLines.size
        if (numSamples < 10) {
            throw IllegalArgumentException("Insufficient ECG sample count: expected at least 10 samples, found $numSamples")
        }

        // Read all rows
        val rawData = Array(12) { FloatArray(numSamples) }
        for (t in 0 until numSamples) {
            val line = dataLines[t]
            val cols = line.split(",")
            if (cols.size < 12) {
                throw IllegalArgumentException("Invalid lead count: expected 12 columns, but found ${cols.size} at row ${t + 1}")
            }
            for (c in 0 until 12) {
                val valueStr = cols[c].trim()
                val value = valueStr.toFloatOrNull()
                    ?: throw IllegalArgumentException("Malformed numeric value '$valueStr' at column ${c + 1}, row ${t + 1}")
                rawData[c][t] = value
            }
        }

        // If exactly 1000 samples, return directly
        if (numSamples == 1000) {
            return rawData
        }

        // Resample along temporal dimension to exactly 1000 samples
        val resampled = Array(12) { FloatArray(1000) }
        for (c in 0 until 12) {
            for (i in 0 until 1000) {
                val srcPos = i.toFloat() * (numSamples - 1).toFloat() / 999.0f
                val idx0 = srcPos.toInt().coerceIn(0, numSamples - 1)
                val idx1 = (idx0 + 1).coerceIn(0, numSamples - 1)
                val frac = srcPos - idx0
                resampled[c][i] = rawData[c][idx0] * (1.0f - frac) + rawData[c][idx1] * frac
            }
        }
        return resampled
    }

    private fun synthesizeECG(ecgSignalPreset: String): Array<FloatArray> {
        val rawECG = Array(12) { FloatArray(1000) }
        val sampleRate = 100.0

        for (c in 0 until 12) {
            for (t in 0 until 1000) {
                val time = t / sampleRate
                
                // Base heart rate frequency
                var hrFreq = 72.0 / 60.0
                
                when (ecgSignalPreset) {
                    "Atrial Fibrillation" -> {
                        if (t % 25 == 0) {
                            hrFreq = (70.0 + (t % 40) - 20) / 60.0
                        }
                    }
                    "VTach" -> {
                        hrFreq = 165.0 / 60.0
                    }
                }

                // Periodic heartbeat generation (sine approximation of P-QRS-T)
                val phase = (time * hrFreq) % 1.0
                var valWave = 0.0

                if (ecgSignalPreset == "VTach") {
                    valWave = sin(2 * PI * phase) * 2.0
                } else {
                    if (phase in 0.05..0.15) {
                        valWave += sin(2 * PI * (phase - 0.05) / 0.1) * 0.12
                    }
                    if (phase in 0.20..0.26) {
                        val qrsPhase = (phase - 0.20) / 0.06
                        valWave += when {
                            qrsPhase < 0.25 -> -qrsPhase * 1.6
                            qrsPhase < 0.75 -> -0.4 + (qrsPhase - 0.25) * 4.8
                            else -> 2.0 - (qrsPhase - 0.75) * 8.0
                        }
                    }
                    if (phase in 0.26..0.50) {
                        val tPhase = (phase - 0.26) / 0.24
                        val stOffset = if (ecgSignalPreset == "ST Elevation MI" && (c == 1 || c == 8)) 0.45 else 0.0
                        valWave += stOffset + sin(PI * tPhase) * 0.3
                    }
                }
                
                val baselineWander = sin(2 * PI * 0.2 * time) * 0.15
                val highFreqNoise = sin(2 * PI * 50.0 * time) * 0.05
                valWave += baselineWander + highFreqNoise

                rawECG[c][t] = valWave.toFloat()
            }
        }
        return rawECG
    }

    private fun lfilter(b: DoubleArray, a: DoubleArray, x: FloatArray): FloatArray {
        val n = Math.max(b.size, a.size)
        val bPadded = DoubleArray(n)
        val aPadded = DoubleArray(n)
        System.arraycopy(b, 0, bPadded, 0, b.size)
        System.arraycopy(a, 0, aPadded, 0, a.size)

        val z = DoubleArray(n)
        val y = FloatArray(x.size)

        for (i in x.indices) {
            val xi = x[i].toDouble()
            val yi = bPadded[0] * xi + z[0]
            y[i] = yi.toFloat()
            for (j in 0 until n - 1) {
                z[j] = bPadded[j + 1] * xi + z[j + 1] - aPadded[j + 1] * yi
            }
        }
        return y
    }
}
