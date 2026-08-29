package com.example.ai.wrappers

import android.util.Log
import com.example.BuildConfig

import android.content.Context
import ai.onnxruntime.OnnxTensor
import com.example.ai.OnnxModelManager
import com.example.ai.utils.DicomParser
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.PatientEntity
import java.io.File
import java.io.FileInputStream
import kotlin.math.exp

class MriParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

object MriModel {
    private const val MODEL_NAME = "ds1_mri_stroke_model.onnx"

    // ImageNet normalization constants
    private val MEANS = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STDS = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun predict(context: Context, patient: PatientEntity, encounter: EncounterEntity): FloatArray {
        val session = OnnxModelManager.getSession(context, MODEL_NAME)
        val env = OnnxModelManager.env

        // 1. Determine if we are loading a real image/DICOM file or playing a demo preset
        var filePath = encounter.mriSliceType
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring(7)
        } else if (filePath.startsWith("file:")) {
            filePath = filePath.substring(5)
        }
        val file = File(filePath)
        val isRealFile = file.exists() && file.isFile

        val rawImage: Array<Array<FloatArray>>

        if (isRealFile) {
            val nameLower = file.name.lowercase()
            val isDicom = nameLower.endsWith(".dcm") || nameLower.endsWith(".dicom")

            if (isDicom) {
                try {
                    FileInputStream(file).use { fis ->
                        val parser = DicomParser(fis)
                        parser.parse()

                        val rawPixels = parser.getPixels()
                        val totalPixels = parser.rows * parser.columns

                        // Min-max scale pixels to [0.0, 255.0]
                        var minVal = Int.MAX_VALUE
                        var maxVal = Int.MIN_VALUE
                        for (i in 0 until totalPixels) {
                            val v = rawPixels[i]
                            if (v < minVal) minVal = v
                            if (v > maxVal) maxVal = v
                        }

                        val range = maxVal - minVal
                        val scaledPixels = FloatArray(totalPixels)
                        if (range > 0) {
                            for (i in 0 until totalPixels) {
                                scaledPixels[i] = (rawPixels[i] - minVal).toFloat() * 255.0f / range.toFloat()
                            }
                        } else {
                            for (i in 0 until totalPixels) {
                                scaledPixels[i] = 0.0f
                            }
                        }

                        // Resize to 128x128
                        val resized = resizeBilinear(scaledPixels, parser.columns, parser.rows, 128, 128)

                        // Replicate to 3 channel RGB representation
                        rawImage = Array(3) { Array(128) { FloatArray(128) } }
                        for (y in 0 until 128) {
                            for (x in 0 until 128) {
                                val pixelVal = resized[y * 128 + x]
                                rawImage[0][y][x] = pixelVal
                                rawImage[1][y][x] = pixelVal
                                rawImage[2][y][x] = pixelVal
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw MriParseException("DICOM Parsing Failed: ${e.message}", e)
                }
            } else {
                // Standard Image format: JPG, JPEG, PNG, WEBP, etc.
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        ?: throw MriParseException("Failed to decode image bitmap from ${file.name}")

                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 128, 128, true)
                    rawImage = Array(3) { Array(128) { FloatArray(128) } }

                    for (y in 0 until 128) {
                        for (x in 0 until 128) {
                            val pixel = scaledBitmap.getPixel(x, y)
                            val r = android.graphics.Color.red(pixel).toFloat()
                            val g = android.graphics.Color.green(pixel).toFloat()
                            val b = android.graphics.Color.blue(pixel).toFloat()

                            rawImage[0][y][x] = r
                            rawImage[1][y][x] = g
                            rawImage[2][y][x] = b
                        }
                    }
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    throw MriParseException("MRI Image Decoding Failed: ${e.message}", e)
                }
            }
        } else {
            // Preset / Synthetic generator for demo/evaluation test mode
            rawImage = Array(3) { Array(128) { FloatArray(128) } }

            val generateLesion = encounter.mriSliceType.contains("DWI", ignoreCase = true) || 
                                 (encounter.mriSliceType.contains("FLAIR", ignoreCase = true) && patient.age > 60)

            val lesionTypeIschemic = encounter.mriSliceType.contains("DWI", ignoreCase = true)

            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    val dx = x - 64
                    val dy = y - 64
                    val distFromCenter = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    var pixelVal = 0.0f
                    if (distFromCenter < 55.0f) {
                        pixelVal = 100.0f + (distFromCenter * 0.5f)
                        val v1 = kotlin.math.sqrt(((x - 52) * (x - 52) + (y - 64) * (y - 64)).toDouble())
                        val v2 = kotlin.math.sqrt(((x - 76) * (x - 76) + (y - 64) * (y - 64)).toDouble())
                        if (v1 < 10.0 || v2 < 10.0) {
                            pixelVal = 30.0f
                        }
                        if (generateLesion) {
                            val lx = x - 85
                            val ly = y - 55
                            val distLesion = kotlin.math.sqrt((lx * lx + ly * ly).toDouble()).toFloat()

                            if (distLesion < 14.0f) {
                                if (lesionTypeIschemic) {
                                    pixelVal = 245.0f - (distLesion * 2.0f)
                                } else {
                                    pixelVal = 190.0f - (distLesion * 1.5f)
                                }
                            }
                        }
                    } else {
                        pixelVal = 5.0f
                    }
                    rawImage[0][y][x] = pixelVal
                    rawImage[1][y][x] = pixelVal
                    rawImage[2][y][x] = pixelVal
                }
            }
        }

        // 2. Preprocess: normalize to [0-1] and apply ImageNet mean/std
        if (BuildConfig.DEBUG) {
            var rawSum = 0.0f
            for (c in 0..2) {
                for (y in 0 until 128) {
                    for (x in 0 until 128) {
                        rawSum += rawImage[c][y][x]
                    }
                }
            }
            Log.d("MriModel", "Raw image sum=$rawSum shape=[3,128,128]")
        }
        val preprocessed = Array(3) { Array(128) { FloatArray(128) } }
        for (c in 0 until 3) {
            val mean = MEANS[c]
            val std = STDS[c]
            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    val normalizedVal = (rawImage[c][y][x] / 255.0f)
                    preprocessed[c][y][x] = (normalizedVal - mean) / std
                }
            }
        }

        // DEBUG: log preprocessed sum
        if (BuildConfig.DEBUG) {
            var preSum = 0.0f
            for (c in 0..2) {
                for (y in 0 until 128) {
                    for (x in 0 until 128) {
                        preSum += preprocessed[c][y][x]
                    }
                }
            }
            Log.d("MriModel", "Preprocessed sum=$preSum shape=[3,128,128]")
        }

        // Wrap to batch: shape [1, 3, 128, 128]
        val inputData = Array(1) { preprocessed }
        // DEBUG: log ONNX input shape
        if (BuildConfig.DEBUG) {
            Log.d("MriModel", "ONNX input shape=[1,3,128,128]")
        }

        // 3. Execution
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

                // 4. Softmax calculation
                var maxLogit = Float.NEGATIVE_INFINITY
                for (logit in logits) {
                    if (logit > maxLogit) maxLogit = logit
                }

                var sumExp = 0.0f
                val expLogits = FloatArray(3)
                for (i in 0 until 3) {
                    expLogits[i] = exp(logits[i] - maxLogit)
                    sumExp += expLogits[i]
                }

                val probabilities = FloatArray(3)
        // DEBUG: log logits and probabilities
        if (BuildConfig.DEBUG) {
            Log.d("MriModel", "Logits=" + logits.contentToString())
            Log.d("MriModel", "Probabilities=" + probabilities.contentToString())
        }
                for (i in 0 until 3) {
                    probabilities[i] = expLogits[i] / sumExp
                }
                return probabilities
            }
        }
    }

    private fun resizeBilinear(src: FloatArray, srcW: Int, srcH: Int, dstW: Int, dstH: Int): FloatArray {
        val dst = FloatArray(dstW * dstH)
        val xRatio = (srcW - 1).toFloat() / dstW
        val yRatio = (srcH - 1).toFloat() / dstH

        for (i in 0 until dstH) {
            for (j in 0 until dstW) {
                val x = (j * xRatio).toInt()
                val y = (i * yRatio).toInt()
                val xDiff = (j * xRatio) - x
                val yDiff = (i * yRatio) - y

                val index = y * srcW + x

                val a = src[index]
                val b = if (x + 1 < srcW) src[index + 1] else a
                val c = if (y + 1 < srcH) src[index + srcW] else a
                val d = if (x + 1 < srcW && y + 1 < srcH) src[index + srcW + 1] else c

                val pixel = a * (1 - xDiff) * (1 - yDiff) +
                            b * xDiff * (1 - yDiff) +
                            c * (1 - xDiff) * yDiff +
                            d * xDiff * yDiff

                dst[i * dstW + j] = pixel
            }
        }
        return dst
    }
}
