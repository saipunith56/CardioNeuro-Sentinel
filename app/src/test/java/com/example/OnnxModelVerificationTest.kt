package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.OnnxModelManager
import com.example.ai.MultimodalInferenceEngine
import com.example.ai.wrappers.*
import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.PatientEntity
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnnxModelVerificationTest {

    private lateinit var context: Context
    private lateinit var samplePatient: PatientEntity
    private lateinit var sampleEncounter: EncounterEntity
    private var isOnnxAvailable = false

    @Before
    fun setUp() {
        try {
            // Force initializer check for native libraries
            ai.onnxruntime.OrtEnvironment.getEnvironment()
            isOnnxAvailable = true
        } catch (t: Throwable) {
            isOnnxAvailable = false
        }

        Assume.assumeTrue(
            "ONNX Runtime JNI binary is not available on this host environment. Skipping JNI test cases.",
            isOnnxAvailable
        )

        context = ApplicationProvider.getApplicationContext()
        MultimodalInferenceEngine.initialize(context)

        samplePatient = PatientEntity(
            id = 1L,
            name = "John Doe",
            medicalRecordNumber = "MRN-12345",
            age = 65,
            gender = "Male",
            bloodGroup = "O+",
            primaryCondition = "Hypertension & Atrial Fibrillation Risk",
            systolicBp = 145,
            diastolicBp = 90,
            cholesterolMgDl = 210,
            fastingGlucoseMgDl = 110,
            bmi = 26.5f,
            smoker = true,
            diabetic = true,
            familyHistoryHeartDisease = true,
            familyHistoryStroke = true
        )

        sampleEncounter = EncounterEntity(
            id = 100L,
            patientId = 1L,
            encounterDate = "2026-08-20",
            chiefComplaint = "Acute onset left-sided weakness and mild chest discomfort.",
            heartRateBpm = 75,
            respiratoryRate = 18,
            oxygenSatPct = 98,
            nihssScore = 8,
            troponinNgMl = 0.08f,
            ecgSignalPreset = "ST Elevation MI",
            eegSignalPreset = "Temporal Spike Wave",
            mriSliceType = "Diffusion Weighted Imaging (DWI)",
            clinicalNotes = "Admitted with acute weakness."
        )
    }

    @Test
    fun testOnnxModelManagerSharedEnv() {
        assertNotNull(OnnxModelManager.env)
    }

    @Test
    fun testClinicalDs2HeartModelInference() {
        val prob = ClinicalDs2HeartModel.predict(context, samplePatient, sampleEncounter)
        assertTrue("DS2 Probability must be in range [0, 1]", prob in 0.0f..1.0f)
    }

    @Test
    fun testClinicalDs3StrokeModelInference() {
        val prob = ClinicalDs3StrokeModel.predict(context, samplePatient, sampleEncounter)
        assertTrue("DS3 Stroke Probability must be in range [0, 1]", prob in 0.0f..1.0f)
    }

    @Test
    fun testEegSeizureModelInference() {
        val prob = EegSeizureModel.predict(context, samplePatient, sampleEncounter)
        assertTrue("EEG Seizure Probability must be in range [0, 1]", prob in 0.0f..1.0f)
    }

    @Test
    fun testEcgModelInference() {
        val probabilities = EcgModel.predict(context, samplePatient, sampleEncounter)
        assertEquals("ECG Model outputs probabilities for 5 classes", 5, probabilities.size)
        probabilities.forEachIndexed { index, prob ->
            assertTrue("ECG class $index probability ($prob) must be in range [0, 1]", prob in 0.0f..1.0f)
        }
    }

    @Test
    fun testMriModelInference() {
        val probabilities = MriModel.predict(context, samplePatient, sampleEncounter)
        assertEquals("MRI Model outputs probabilities for 3 classes", 3, probabilities.size)
        
        var sum = 0.0f
        probabilities.forEachIndexed { index, prob ->
            assertTrue("MRI class $index probability ($prob) must be in range [0, 1]", prob in 0.0f..1.0f)
            sum += prob
        }
        assertEquals("MRI class probabilities must sum to 1.0", 1.0f, sum, 1e-4f)
    }

    @Test
    fun testMultimodalInferenceEngineOrchestrator() {
        val result = MultimodalInferenceEngine.executePrediction(samplePatient, sampleEncounter)
        assertNotNull(result)
        assertEquals(1L, result.patientId)
        assertEquals(100L, result.encounterId)
        assertTrue("Combined risk score should be in valid bounds", result.combinedRiskScorePct in 5..99)
        assertTrue("Heart risk score should be in valid bounds", result.heartRiskScorePct in 5..98)
        assertTrue("Stroke risk score should be in valid bounds", result.strokeRiskScorePct in 5..98)
        assertTrue("EEG risk score should be in valid bounds", result.eegSeizureRiskScorePct in 5..98)
        assertTrue("MRI infarct should be detected based on DWI preset", result.mriInfarctDetected)
        assertTrue("MRI volume should be greater than zero", result.mriInfarctVolumeCc > 0.0f)
    }

    @Test
    fun testInvalidInputProducesExplicitState() {
        val invalidPatient = samplePatient.copy(age = -5) // invalid age
        val result = MultimodalInferenceEngine.executePrediction(invalidPatient, sampleEncounter)
        assertNotNull(result)
        assertEquals(0, result.combinedRiskScorePct)
        assertEquals("INVALID/INCOMPATIBLE INPUTS", result.riskSeverityCategory)
        assertTrue(result.heartDiagnosisLabel.contains("Blocked"))
        assertTrue(result.strokeDiagnosisLabel.contains("Blocked"))
    }

    @Test
    fun testRealDicomFileInference() {
        // 1. Create a valid mock DICOM file
        val mockBytes = createMockDicomBytes(128, 128, 16)
        val tempDicomFile = java.io.File.createTempFile("mock_scan", ".dcm")
        tempDicomFile.writeBytes(mockBytes)

        // 2. Configure encounter to point to this file's path
        val realDicomEncounter = sampleEncounter.copy(mriSliceType = tempDicomFile.absolutePath)

        // 3. Execute
        val result = MultimodalInferenceEngine.executePrediction(samplePatient, realDicomEncounter)
        assertNotNull(result)
        assertTrue("MRI diagnosis should contain [Real DICOM Scan] label", result.strokeDiagnosisLabel.contains("[Real DICOM Scan]"))
        assertTrue("MRI infarct volume must be valid (>= 0)", result.mriInfarctVolumeCc >= 0.0f)

        // Cleanup
        tempDicomFile.delete()
    }

    @Test
    fun testFailedDicomParsing() {
        // 1. Point to an invalid file
        val tempInvalidFile = java.io.File.createTempFile("corrupt_scan", ".dcm")
        tempInvalidFile.writeText("Corrupted DICOM Data")

        val invalidDicomEncounter = sampleEncounter.copy(mriSliceType = tempInvalidFile.absolutePath)

        // 2. Execute
        val result = MultimodalInferenceEngine.executePrediction(samplePatient, invalidDicomEncounter)
        assertNotNull(result)
        assertEquals("ERROR/FAILED", result.mriProvenance)
        assertFalse("MRI must not be available when parsing fails", result.mriAvailable)
        assertFalse("Never report infarct when parsing fails", result.mriInfarctDetected)
        assertEquals("MRI volume must be -1.0f on parsing error", -1.0f, result.mriInfarctVolumeCc, 1e-4f)
        assertTrue("Stroke diagnosis must indicate failure", result.strokeDiagnosisLabel.contains("MRI Parsing Failed") || result.strokeDiagnosisLabel.contains("MRI Input Processing Failed"))

        // Cleanup
        tempInvalidFile.delete()
    }

    @Test
    fun testEcgReportOcrParsing() {
        val sampleText = """
            ECG 12-LEAD CLINICAL REPORT
            Heart Rate: 82 BPM
            Rhythm: Atrial Fibrillation
            PR Interval: 160 ms
            QRS Duration: 92 ms
            QTc: 440 ms
            ST-T Changes: ST elevation in leads V2-V4
            Interpretation: Acute anterior ST-elevation myocardial infarction
        """.trimIndent()

        val parsed = com.example.ai.ingestion.DocumentIngestionEngine.extractEcgReportFeatures(sampleText)
        assertTrue(parsed.isReport)
        assertEquals(82, parsed.heartRateBpm)
        assertEquals("Atrial Fibrillation", parsed.rhythm)
        assertEquals(160, parsed.prIntervalMs)
        assertEquals(92, parsed.qrsDurationMs)
        assertEquals(440, parsed.qtcMs)
        assertNotNull(parsed.stTFindings)
    }

    @Test
    fun testEegReportOcrParsing() {
        val sampleText = """
            ROUTINE EEG REPORT
            Dominant Rhythm: Posterior Alpha 10 Hz
            Findings: Diffuse slowing with intermittent polymorphic delta waves.
            Sharp Waves: Temporal spike-and-wave discharges
            Impression: Abnormal EEG consistent with focal epileptogenic activity.
        """.trimIndent()

        val parsed = com.example.ai.ingestion.DocumentIngestionEngine.extractEegReportFeatures(sampleText)
        assertTrue(parsed.isReport)
        assertNotNull(parsed.slowing)
        assertNotNull(parsed.spikesSharpWaves)
        assertNotNull(parsed.dominantRhythm)
    }

    @Test
    fun testRadiologyReportOcrParsing() {
        val sampleText = """
            BRAIN MRI SCAN REPORT
            MODALITY: MRI Brain with DWI and FLAIR
            FINDINGS: Restricted diffusion in the left middle cerebral artery (MCA) territory.
            No acute intracranial hemorrhage or midline shift.
            IMPRESSION: Acute ischemic infarction in the left MCA distribution.
        """.trimIndent()

        val parsed = com.example.ai.ingestion.DocumentIngestionEngine.extractRadiologyReportFeatures(sampleText)
        assertTrue(parsed.isReport)
        assertEquals(true, parsed.acuteInfarctDetected)
        assertEquals(false, parsed.hemorrhageDetected)
        assertNotNull(parsed.impression)
    }

    @Test
    fun testEcgCsvResamplingWith2295Rows() {
        // Create an ECG CSV with 2295 rows (simulating 10s at 229.5Hz or 23s recording)
        val tempCsv = java.io.File.createTempFile("ecg_2295_rows", ".csv")
        tempCsv.bufferedWriter().use { writer ->
            writer.write("I,II,III,aVR,aVL,aVF,V1,V2,V3,V4,V5,V6\n")
            for (i in 0 until 2295) {
                val line = (1..12).joinToString(",") { ((i % 50) * 0.01f).toString() }
                writer.write(line + "\n")
            }
        }

        val ecgEncounter = sampleEncounter.copy(ecgFilePath = tempCsv.absolutePath)
        val result = MultimodalInferenceEngine.executePrediction(samplePatient, ecgEncounter)
        assertNotNull(result)
        assertEquals("REAL", result.ecgProvenance)
        assertTrue(result.ecgAvailable)

        tempCsv.delete()
    }

    @Test
    fun testEstimatedAcuteCardiacRiskCalculation() {
        val acuteEncounter = sampleEncounter.copy(
            troponinNgMl = 0.85f,
            ecgSignalPreset = "ST Elevation MI",
            chiefComplaint = "Crushing substernal chest pain radiating to left arm"
        )
        val result = MultimodalInferenceEngine.executePrediction(samplePatient, acuteEncounter)
        assertNotNull(result)
        assertTrue("Estimated Acute Cardiac Risk must be >= 60% for high troponin and STEMI", result.acuteCardiacRiskScorePct >= 60)
    }

    private fun createMockDicomBytes(rows: Int, cols: Int, bitsAllocated: Int): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        
        // Preamble (128 bytes 0)
        stream.write(ByteArray(128))
        
        // Prefix "DICM"
        stream.write("DICM".toByteArray())
        
        // Group 0002 elements (Transfer Syntax UID)
        // Tag (0002, 0010)
        stream.write(byteArrayOf(0x02, 0x00, 0x10, 0x00))
        stream.write("UI".toByteArray())
        val uid = "1.2.840.10008.1.2.1\u0000"
        stream.write(byteArrayOf((uid.length and 0xFF).toByte(), 0x00))
        stream.write(uid.toByteArray())
        
        // Rows (0028, 0010)
        stream.write(byteArrayOf(0x28, 0x00, 0x10, 0x00))
        stream.write("US".toByteArray())
        stream.write(byteArrayOf(0x02, 0x00))
        stream.write(byteArrayOf((rows and 0xFF).toByte(), ((rows shr 8) and 0xFF).toByte()))
        
        // Columns (0028, 0011)
        stream.write(byteArrayOf(0x28, 0x00, 0x11, 0x00))
        stream.write("US".toByteArray())
        stream.write(byteArrayOf(0x02, 0x00))
        stream.write(byteArrayOf((cols and 0xFF).toByte(), ((cols shr 8) and 0xFF).toByte()))
        
        // Bits Allocated (0028, 0100)
        stream.write(byteArrayOf(0x28, 0x00, 0x00, 0x01))
        stream.write("US".toByteArray())
        stream.write(byteArrayOf(0x02, 0x00))
        stream.write(byteArrayOf((bitsAllocated and 0xFF).toByte(), ((bitsAllocated shr 8) and 0xFF).toByte()))
        
        // Bits Stored (0028, 0101)
        stream.write(byteArrayOf(0x28, 0x00, 0x01, 0x01))
        stream.write("US".toByteArray())
        stream.write(byteArrayOf(0x02, 0x00))
        stream.write(byteArrayOf((bitsAllocated and 0xFF).toByte(), ((bitsAllocated shr 8) and 0xFF).toByte()))
        
        // Pixel Representation (0028, 0103)
        stream.write(byteArrayOf(0x28, 0x00, 0x03, 0x01))
        stream.write("US".toByteArray())
        stream.write(byteArrayOf(0x02, 0x00))
        stream.write(byteArrayOf(0x00, 0x00))
        
        // Pixel Data (7FE0, 0010)
        val pixelByteLen = rows * cols * (bitsAllocated / 8)
        stream.write(byteArrayOf(0xE0.toByte(), 0x7F.toByte(), 0x10, 0x00))
        stream.write("OW".toByteArray())
        stream.write(byteArrayOf(0x00, 0x00)) // Reserved 2 bytes
        stream.write(byteArrayOf(
            (pixelByteLen and 0xFF).toByte(),
            ((pixelByteLen shr 8) and 0xFF).toByte(),
            ((pixelByteLen shr 16) and 0xFF).toByte(),
            ((pixelByteLen shr 24) and 0xFF).toByte()
        ))
        
        val pixelData = ByteArray(pixelByteLen)
        for (i in pixelData.indices) {
            pixelData[i] = (i % 256).toByte()
        }
        stream.write(pixelData)
        
        return stream.toByteArray()
    }
}
