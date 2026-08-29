package com.example.ai.ingestion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class InputProvenance(val label: String) {
    REAL("REAL"),
    EXTRACTED("EXTRACTED"),
    DEMO_PRESET("DEMO/PRESET"),
    NOT_PROVIDED("NOT PROVIDED"),
    ERROR_FAILED("ERROR/FAILED")
}

enum class DetectedInputType(val displayName: String) {
    RAW_DIAGNOSTIC_IMAGE("Raw Diagnostic Image"),
    CLINICAL_REPORT_PHOTO("Clinical Report Photo"),
    RAW_SIGNAL_DATA("Raw Signal Data (CSV/EDF/DICOM)"),
    UNKNOWN("Unknown Format")
}

data class EcgExtractionResult(
    val isReport: Boolean = false,
    val heartRateBpm: Int? = null,
    val rhythm: String? = null,
    val prIntervalMs: Int? = null,
    val qrsDurationMs: Int? = null,
    val qtcMs: Int? = null,
    val stTFindings: String? = null,
    val machineInterpretation: String? = null,
    val rawText: String = "",
    val provenance: InputProvenance = InputProvenance.NOT_PROVIDED,
    val errorMessage: String? = null
)

data class EegExtractionResult(
    val isReport: Boolean = false,
    val dominantRhythm: String? = null,
    val frequencyHz: Float? = null,
    val slowing: String? = null,
    val spikesSharpWaves: String? = null,
    val seizureFindings: String? = null,
    val impression: String? = null,
    val rawText: String = "",
    val provenance: InputProvenance = InputProvenance.NOT_PROVIDED,
    val errorMessage: String? = null
)

data class RadiologyExtractionResult(
    val isReport: Boolean = false,
    val scanModality: String? = null,
    val bodyRegion: String? = null,
    val acuteInfarctDetected: Boolean? = null,
    val infarctLocation: String? = null,
    val hemorrhageDetected: Boolean? = null,
    val hemorrhageLocation: String? = null,
    val midlineShiftOrMassEffect: String? = null,
    val impression: String? = null,
    val rawText: String = "",
    val provenance: InputProvenance = InputProvenance.NOT_PROVIDED,
    val errorMessage: String? = null
)

object DocumentIngestionEngine {
    private const val TAG = "DocIngestionEngine"
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Performs on-device ML Kit OCR on an image file or URI.
     */
    suspend fun performOcr(context: Context, fileUriOrPath: String): String = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(context, fileUriOrPath)
            ?: throw IllegalArgumentException("Cannot decode image for OCR from $fileUriOrPath")

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text
                    Log.d(TAG, "OCR Completed (${recognizedText.length} chars). Preview: ${recognizedText.take(120).replace('\n', ' ')}")
                    continuation.resume(recognizedText)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR Recognition Failed", e)
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * Classifies whether an image is a text report or raw diagnostic image based on OCR text density.
     */
    fun classifyImageContent(ocrText: String, fileExtension: String): DetectedInputType {
        val textLength = ocrText.trim().length
        val reportKeywords = listOf(
            "patient", "date", "report", "dr.", "doctor", "hospital", "clinic", "interpretation",
            "impression", "findings", "lead", "bpm", "rhythm", "rate", "pr", "qrs", "qt", "qtc",
            "eeg", "ecg", "ekg", "mri", "ct", "scan", "infarct", "hemorrhage", "normal", "abnormal",
            "frequency", "hz", "voltage", "diagnosis", "telemetry", "history"
        )
        val lowerText = ocrText.lowercase()
        val keywordCount = reportKeywords.count { lowerText.contains(it) }

        return if (textLength > 60 || keywordCount >= 2) {
            DetectedInputType.CLINICAL_REPORT_PHOTO
        } else {
            DetectedInputType.RAW_DIAGNOSTIC_IMAGE
        }
    }

    /**
     * Ingests and extracts structured parameters from an ECG input (image, report photo, or CSV).
     */
    suspend fun parseEcgInput(context: Context, filePathOrPreset: String): EcgExtractionResult = withContext(Dispatchers.IO) {
        val file = File(filePathOrPreset.removePrefix("file://").removePrefix("file:"))
        if (!file.exists() || !file.isFile) {
            return@withContext EcgExtractionResult(
                provenance = InputProvenance.DEMO_PRESET,
                rhythm = filePathOrPreset
            )
        }

        val nameLower = file.name.lowercase()
        if (nameLower.endsWith(".csv") || nameLower.endsWith(".edf") || nameLower.endsWith(".hl7") || nameLower.endsWith(".dat")) {
            return@withContext EcgExtractionResult(
                isReport = false,
                provenance = InputProvenance.REAL,
                rhythm = "Raw Waveform Signal (${file.name})"
            )
        }

        // It is an image (JPG, PNG, JPEG, WEBP, etc.) -> Run OCR
        try {
            val text = performOcr(context, file.absolutePath)
            if (text.isBlank()) {
                return@withContext EcgExtractionResult(
                    isReport = false,
                    provenance = InputProvenance.REAL,
                    rawText = "",
                    rhythm = "Waveform Image Photograph (${file.name})"
                )
            }

            return@withContext extractEcgReportFeatures(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ECG image: ${e.message}", e)
            return@withContext EcgExtractionResult(
                provenance = InputProvenance.ERROR_FAILED,
                errorMessage = "ECG Image OCR Failed: ${e.message}"
            )
        }
    }

    /**
     * Regex and token parser for ECG clinical reports.
     */
    fun extractEcgReportFeatures(text: String): EcgExtractionResult {
        val lower = text.lowercase()

        // 1. Heart Rate extraction: e.g. "HR: 84 bpm", "Heart Rate 78", "Vent. Rate: 115 BPM", "Rate: 92"
        val hrRegex = Regex("""(?:hr|heart\s*rate|vent(?:ricular)?\.?\s*rate|rate|pulse)[\s:=]*([0-9]{2,3})\s*(?:bpm)?""", RegexOption.IGNORE_CASE)
        val hrMatch = hrRegex.find(text)
        val hr = hrMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(30, 250)

        // 2. PR interval: e.g. "PR: 160 ms", "PR interval: 210ms", "PR: 140"
        val prRegex = Regex("""(?:pr(?:\s*int(?:erval)?)?)[\s:=]*([0-9]{2,3})\s*(?:ms)?""", RegexOption.IGNORE_CASE)
        val prMatch = prRegex.find(text)
        val pr = prMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(60, 400)

        // 3. QRS duration: e.g. "QRS: 88 ms", "QRS duration 120ms"
        val qrsRegex = Regex("""(?:qrs(?:\s*dur(?:ation)?)?)[\s:=]*([0-9]{2,3})\s*(?:ms)?""", RegexOption.IGNORE_CASE)
        val qrsMatch = qrsRegex.find(text)
        val qrs = qrsMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(40, 250)

        // 4. QT / QTc: e.g. "QTc: 442 ms", "QT/QTc: 380/420"
        val qtcRegex = Regex("""(?:qtc(?:[\s/]*bazett)?)[\s:=]*([0-9]{3})\s*(?:ms)?""", RegexOption.IGNORE_CASE)
        val qtcMatch = qtcRegex.find(text)
        val qtc = qtcMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(250, 650)

        // 5. Rhythm determination from report text
        val rhythm = when {
            lower.contains("atrial fibrillation") || lower.contains("a-fib") || lower.contains("afib") -> "Atrial Fibrillation"
            lower.contains("atrial flutter") -> "Atrial Flutter"
            lower.contains("ventricular tachycardia") || lower.contains("v-tach") || lower.contains("vtach") -> "Ventricular Tachycardia"
            lower.contains("sinus tachycardia") -> "Sinus Tachycardia"
            lower.contains("sinus bradycardia") -> "Sinus Bradycardia"
            lower.contains("sinus rhythm") || lower.contains("normal sinus") -> "Normal Sinus Rhythm"
            lower.contains("pacing") || lower.contains("paced rhythm") -> "Pacemaker Rhythm"
            lower.contains("junctional") -> "Junctional Rhythm"
            else -> if (hr != null) "Sinus Rhythm ($hr bpm)" else "Recorded Cardiac Rhythm"
        }

        // 6. ST-T findings
        val stT = when {
            lower.contains("st elevation") || lower.contains("stemi") || lower.contains("st-segment elevation") -> {
                val leadMatch = Regex("""(?:elevation\s*(?:in)?\s*(?:leads?)?\s*)([ivxl1-6,\s]+)""", RegexOption.IGNORE_CASE).find(text)
                val leads = leadMatch?.groupValues?.get(1)?.trim()
                if (!leads.isNullOrBlank()) "Acute ST Elevation in leads $leads" else "Acute ST Elevation MI Pattern"
            }
            lower.contains("st depression") || lower.contains("st-segment depression") -> "ST-Segment Depression (Subendocardial Ischemia)"
            lower.contains("t wave inversion") || lower.contains("t-wave inversion") || lower.contains("inverted t") -> "T-Wave Inversion / Ischemic Repolarization"
            lower.contains("left bundle branch block") || lower.contains("lbbb") -> "Left Bundle Branch Block (LBBB)"
            lower.contains("right bundle branch block") || lower.contains("rbbb") -> "Right Bundle Branch Block (RBBB)"
            lower.contains("left ventricular hypertrophy") || lower.contains("lvh") -> "Left Ventricular Hypertrophy (LVH)"
            lower.contains("normal ecg") || lower.contains("within normal limits") -> "Normal ST-T Wave Morphology"
            else -> "Nonspecific ST-T Findings"
        }

        // 7. Summary Interpretation
        val interpretationRegex = Regex("""(?:interpretation|impression|conclusion|diagnosis)[\s:=]+([^\n\r]+)""", RegexOption.IGNORE_CASE)
        val interpMatch = interpretationRegex.find(text)
        val interpretation = interpMatch?.groupValues?.get(1)?.trim() ?: "$rhythm, $stT"

        return EcgExtractionResult(
            isReport = true,
            heartRateBpm = hr,
            rhythm = rhythm,
            prIntervalMs = pr,
            qrsDurationMs = qrs,
            qtcMs = qtc,
            stTFindings = stT,
            machineInterpretation = interpretation,
            rawText = text,
            provenance = InputProvenance.EXTRACTED
        )
    }

    /**
     * Ingests and extracts structured parameters from an EEG input (image, report photo, or EDF/CSV).
     */
    suspend fun parseEegInput(context: Context, filePathOrPreset: String): EegExtractionResult = withContext(Dispatchers.IO) {
        val file = File(filePathOrPreset.removePrefix("file://").removePrefix("file:"))
        if (!file.exists() || !file.isFile) {
            return@withContext EegExtractionResult(
                provenance = InputProvenance.DEMO_PRESET,
                dominantRhythm = filePathOrPreset
            )
        }

        val nameLower = file.name.lowercase()
        if (nameLower.endsWith(".edf") || nameLower.endsWith(".csv") || nameLower.endsWith(".dat") || nameLower.endsWith(".bin")) {
            return@withContext EegExtractionResult(
                isReport = false,
                provenance = InputProvenance.REAL,
                dominantRhythm = "Raw Telemetry Signal (${file.name})"
            )
        }

        // It is an image (JPG, PNG, JPEG, WEBP) -> Run OCR
        try {
            val text = performOcr(context, file.absolutePath)
            if (text.isBlank()) {
                return@withContext EegExtractionResult(
                    isReport = false,
                    provenance = InputProvenance.REAL,
                    rawText = "",
                    dominantRhythm = "EEG Tracing Screenshot (${file.name})"
                )
            }

            return@withContext extractEegReportFeatures(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EEG image: ${e.message}", e)
            return@withContext EegExtractionResult(
                provenance = InputProvenance.ERROR_FAILED,
                errorMessage = "EEG Image OCR Failed: ${e.message}"
            )
        }
    }

    /**
     * Regex and token parser for EEG clinical reports.
     */
    fun extractEegReportFeatures(text: String): EegExtractionResult {
        val lower = text.lowercase()

        // 1. Dominant background rhythm & frequency
        val freqRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:hz|cycles/sec)""", RegexOption.IGNORE_CASE)
        val freqMatch = freqRegex.find(text)
        val freq = freqMatch?.groupValues?.get(1)?.toFloatOrNull()

        val rhythm = when {
            lower.contains("alpha") -> "Posterior Dominant Alpha Rhythm (${freq ?: 10.0f} Hz)"
            lower.contains("beta") -> "Fast Beta Activity (${freq ?: 18.0f} Hz)"
            lower.contains("theta") -> "Generalized Theta Rhythm (${freq ?: 6.0f} Hz)"
            lower.contains("delta") -> "Polymorphic Delta Activity (${freq ?: 2.5f} Hz)"
            lower.contains("suppression") || lower.contains("attenuation") -> "Low-Voltage Diffuse Suppression"
            else -> if (freq != null) "Background Rhythm ($freq Hz)" else "Resting Cortical Rhythm"
        }

        // 2. Slowing (focal vs diffuse)
        val slowing = when {
            lower.contains("diffuse slowing") -> "Diffuse Slowing (Encephalopathy / Ischemic Hypoperfusion)"
            lower.contains("focal slowing") || lower.contains("temporal slowing") || lower.contains("frontal slowing") -> {
                if (lower.contains("temporal")) "Focal Left/Right Temporal Slowing" else "Focal Cortical Slowing"
            }
            lower.contains("intermittent slowing") -> "Intermittent Rhythmic Delta Activity (FIRDA/TIRDA)"
            lower.contains("no slowing") || lower.contains("slowing: none") -> "No Pathological Slowing"
            else -> if (lower.contains("slowing")) "Cerebral Slowing Noted" else "Preserved Background Frequencies"
        }

        // 3. Spikes and Sharp Waves
        val spikes = when {
            lower.contains("spike and wave") || lower.contains("spike-and-wave") || lower.contains("spike-wave") -> "Periodic Spike-and-Wave Discharges (3 Hz)"
            lower.contains("sharp wave") || lower.contains("sharp waves") || lower.contains("sharp-wave") -> "Focal Sharp Wave Transients"
            lower.contains("polyspike") -> "Generalized Polyspike-Wave Complexes"
            lower.contains("pleds") || lower.contains("lpd") || lower.contains("lateralized periodic") -> "Lateralized Periodic Discharges (LPDs)"
            lower.contains("epileptiform") && !lower.contains("no epileptiform") -> "Active Focal Epileptiform Discharges"
            lower.contains("no epileptiform") || lower.contains("no spikes") || lower.contains("without epileptiform") -> "No Epileptiform Discharges"
            else -> "No Definite Epileptiform Transients"
        }

        // 4. Seizure findings
        val seizure = when {
            lower.contains("status epilepticus") -> "Status Epilepticus Detected"
            lower.contains("electrographic seizure") || lower.contains("ictal") && !lower.contains("non-ictal") -> "Electrographic Ictal Seizure Activity"
            lower.contains("high seizure risk") || lower.contains("epileptogenic") -> "Elevated Epileptogenic Potential"
            lower.contains("seizure") && lower.contains("negative") -> "No Seizure Activity Recorded"
            else -> "Routine Interictal EEG Pattern"
        }

        // 5. Clinical Impression
        val impressionRegex = Regex("""(?:impression|conclusion|interpretation|summary)[\s:=]+([^\n\r]+)""", RegexOption.IGNORE_CASE)
        val impMatch = impressionRegex.find(text)
        val impression = impMatch?.groupValues?.get(1)?.trim() ?: "$rhythm. $slowing. $spikes."

        return EegExtractionResult(
            isReport = true,
            dominantRhythm = rhythm,
            frequencyHz = freq,
            slowing = slowing,
            spikesSharpWaves = spikes,
            seizureFindings = seizure,
            impression = impression,
            rawText = text,
            provenance = InputProvenance.EXTRACTED
        )
    }

    /**
     * Ingests and extracts structured parameters from an MRI / CT input (image, report photo, or DICOM).
     */
    suspend fun parseRadiologyInput(context: Context, filePathOrPreset: String): RadiologyExtractionResult = withContext(Dispatchers.IO) {
        val file = File(filePathOrPreset.removePrefix("file://").removePrefix("file:"))
        if (!file.exists() || !file.isFile) {
            return@withContext RadiologyExtractionResult(
                provenance = InputProvenance.DEMO_PRESET,
                scanModality = filePathOrPreset
            )
        }

        val nameLower = file.name.lowercase()
        if (nameLower.endsWith(".dcm") || nameLower.endsWith(".dicom") || nameLower.endsWith(".nii") || nameLower.endsWith(".nii.gz")) {
            return@withContext RadiologyExtractionResult(
                isReport = false,
                provenance = InputProvenance.REAL,
                scanModality = "DICOM / NIfTI Volumetric Scan (${file.name})"
            )
        }

        // Check if image is an actual MRI pixel slice or a written report
        try {
            val text = performOcr(context, file.absolutePath)
            val classification = classifyImageContent(text, file.extension)

            if (classification == DetectedInputType.RAW_DIAGNOSTIC_IMAGE || text.isBlank()) {
                // Actual scan slice image (JPG/PNG) -> Direct ONNX inference
                return@withContext RadiologyExtractionResult(
                    isReport = false,
                    provenance = InputProvenance.REAL,
                    scanModality = "Brain MRI Slice Image (${file.name})",
                    rawText = ""
                )
            }

            // It is a photographed / scanned radiology report
            return@withContext extractRadiologyReportFeatures(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process radiology input: ${e.message}", e)
            return@withContext RadiologyExtractionResult(
                provenance = InputProvenance.ERROR_FAILED,
                errorMessage = "Radiology Input Processing Failed: ${e.message}"
            )
        }
    }

    /**
     * Regex and token parser for Radiology / MRI / CT reports.
     */
    fun extractRadiologyReportFeatures(text: String): RadiologyExtractionResult {
        val lower = text.lowercase()

        // 1. Modality
        val modality = when {
            lower.contains("dwi") || lower.contains("diffusion weighted") -> "MRI Brain (Diffusion Weighted DWI)"
            lower.contains("flair") || lower.contains("t2 flair") -> "MRI Brain (Axial T2-FLAIR)"
            lower.contains("mri") || lower.contains("magnetic resonance") -> "Magnetic Resonance Imaging (MRI Brain)"
            lower.contains("ct angiography") || lower.contains("cta") -> "Computed Tomography Angiography (CTA)"
            lower.contains("ct scan") || lower.contains("computed tomography") -> "Non-Contrast Head CT"
            else -> "Diagnostic Neuroimaging"
        }

        // 2. Acute Infarct / Ischemia findings
        val hasInfarct = when {
            lower.contains("acute infarct") || lower.contains("acute ischemic") || lower.contains("restricted diffusion") ||
            lower.contains("infarction") && !lower.contains("no acute infarct") && !lower.contains("without acute infarct") -> true
            lower.contains("no acute infarct") || lower.contains("no acute territorial infarction") ||
            lower.contains("no restricted diffusion") || lower.contains("normal scan") -> false
            else -> null
        }

        val infarctLocation = when {
            lower.contains("mca") || lower.contains("middle cerebral") -> "Middle Cerebral Artery (MCA) Territory"
            lower.contains("pca") || lower.contains("posterior cerebral") -> "Posterior Cerebral Artery (PCA) Territory"
            lower.contains("aca") || lower.contains("anterior cerebral") -> "Anterior Cerebral Artery (ACA) Territory"
            lower.contains("basal ganglia") -> "Basal Ganglia / Internal Capsule"
            lower.contains("lacunar") || lower.contains("small vessel") -> "Small-Vessel Lacunar Infarction"
            lower.contains("cerebellar") || lower.contains("cerebellum") || lower.contains("brainstem") -> "Posterior Fossa / Brainstem"
            hasInfarct == true -> "Focal Acute Cerebral Ischemia"
            else -> "No Acute Territorial Infarct"
        }

        // 3. Hemorrhage / Bleed findings
        val hasHemorrhage = when {
            lower.contains("intracerebral hemorrhage") || lower.contains("ich") || lower.contains("subarachnoid hemorrhage") ||
            lower.contains("sah") || lower.contains("hemorrhagic transformation") || lower.contains("hematoma") &&
            !lower.contains("no hemorrhage") && !lower.contains("without hemorrhage") -> true
            lower.contains("no hemorrhage") || lower.contains("no intracranial hemorrhage") || lower.contains("no acute bleed") -> false
            else -> null
        }

        val hemorrhageLocation = when {
            lower.contains("subarachnoid") -> "Subarachnoid Hemorrhage (SAH)"
            lower.contains("subdural") -> "Subdural Hematoma (SDH)"
            lower.contains("epidural") -> "Epidural Hematoma (EDH)"
            lower.contains("intraventricular") -> "Intraventricular Hemorrhage"
            lower.contains("intracerebral") || lower.contains("intraparenchymal") -> "Acute Intraparenchymal Hemorrhage"
            hasHemorrhage == true -> "Intracranial Hemorrhage Detected"
            else -> "No Acute Intracranial Bleed"
        }

        // 4. Mass effect or midline shift
        val massEffect = when {
            lower.contains("midline shift") -> {
                val mmMatch = Regex("""midline\s*shift\s*(?:of)?\s*([0-9]+(?:\.[0-9]+)?\s*mm)""", RegexOption.IGNORE_CASE).find(text)
                "Positive Midline Shift (${mmMatch?.groupValues?.get(1) ?: "Prominent"})"
            }
            lower.contains("mass effect") -> "Focal Mass Effect with Sulcal Effacement"
            lower.contains("no midline shift") || lower.contains("no mass effect") -> "No Midline Shift or Mass Effect"
            else -> "Stable Ventricular Configuration"
        }

        // 5. Impression
        val impressionRegex = Regex("""(?:impression|conclusion|findings)[\s:=]+([^\n\r]+)""", RegexOption.IGNORE_CASE)
        val impMatch = impressionRegex.find(text)
        val impression = impMatch?.groupValues?.get(1)?.trim() ?: run {
            val status = when {
                hasHemorrhage == true -> "Positive for $hemorrhageLocation"
                hasInfarct == true -> "Positive for $infarctLocation"
                hasInfarct == false && hasHemorrhage == false -> "Unremarkable Scan. No acute intracranial abnormality."
                else -> "Neuroimaging evaluation completed."
            }
            "$modality: $status"
        }

        return RadiologyExtractionResult(
            isReport = true,
            scanModality = modality,
            bodyRegion = "Brain / Neurovascular",
            acuteInfarctDetected = hasInfarct,
            infarctLocation = infarctLocation,
            hemorrhageDetected = hasHemorrhage,
            hemorrhageLocation = hemorrhageLocation,
            midlineShiftOrMassEffect = massEffect,
            impression = impression,
            rawText = text,
            provenance = InputProvenance.EXTRACTED
        )
    }

    private fun loadBitmap(context: Context, fileUriOrPath: String): Bitmap? {
        return try {
            val cleaned = fileUriOrPath.removePrefix("file://").removePrefix("file:")
            val file = File(cleaned)
            if (file.exists() && file.isFile) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                val uri = Uri.parse(fileUriOrPath)
                context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from $fileUriOrPath", e)
            null
        }
    }
}
