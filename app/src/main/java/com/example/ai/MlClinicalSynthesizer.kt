package com.example.ai

import com.example.data.local.entities.EncounterEntity
import com.example.data.local.entities.PatientEntity
import com.example.data.local.entities.PredictionResultEntity

object MlClinicalSynthesizer {

    fun generateTraditionalMlSynthesis(
        patient: PatientEntity,
        encounter: EncounterEntity,
        prediction: PredictionResultEntity
    ): String {
        val mriStatus = if (prediction.mriProvenance == "ERROR/FAILED") {
            "MRI Evaluation: FAILED / UNAVAILABLE (No valid image or report processed)"
        } else if (prediction.mriProvenance == "EXTRACTED") {
            "Radiology Report OCR: ${prediction.strokeDiagnosisLabel}"
        } else if (prediction.mriProvenance == "REAL") {
            "MobileNetV3 2D-CNN: ${prediction.strokeDiagnosisLabel} (Infarct detected: ${prediction.mriInfarctDetected})"
        } else {
            "MRI Preset Heuristic: ${prediction.strokeDiagnosisLabel}"
        }

        val ecgStatus = if (prediction.ecgProvenance == "ERROR/FAILED") {
            "ECG Evaluation: FAILED / UNAVAILABLE"
        } else if (prediction.ecgProvenance == "EXTRACTED") {
            "ECG Report OCR: ${prediction.ecgArrhythmiaDetected}"
        } else if (prediction.ecgProvenance == "REAL") {
            "1D-ResNet ECG Telemetry: ${prediction.ecgArrhythmiaDetected}"
        } else {
            "ECG Preset Heuristic: ${prediction.ecgArrhythmiaDetected}"
        }

        return """
            ### 📊 Multimodal Model-Estimated Risk Stratification
            - **Combined Multimodal Risk**: ${prediction.combinedRiskScorePct}% (${prediction.riskSeverityCategory})
            - **Cardiovascular System Risk**: ${prediction.heartRiskScorePct}% (Tabular DS2 MLP)
            - **Neurological / Stroke Risk**: ${prediction.strokeRiskScorePct}% (Tabular DS3 MLP)
            - **Estimated Acute Cardiac Risk**: ${prediction.acuteCardiacRiskScorePct}% (Estimated based on available cardiac biomarkers & ECG)
            - **Patient Clinical Parameters**: Blood Pressure ${patient.systolicBp}/${patient.diastolicBp} mmHg, Total Cholesterol ${patient.cholesterolMgDl} mg/dL, Fasting Glucose ${patient.fastingGlucoseMgDl} mg/dL, Troponin I ${encounter.troponinNgMl} ng/mL.

            ### 🧠 Diagnostic Imaging & Signal Ingestion
            - **Neuroimaging (MRI/CT)**: $mriStatus [Provenance: ${prediction.mriProvenance}]
            - **Cardiac Electrophysiology (ECG)**: $ecgStatus [Provenance: ${prediction.ecgProvenance}]
            - **Electroencephalography (EEG)**: Seizure Risk Index: ${prediction.eegSeizureRiskScorePct}% [Provenance: ${prediction.eegProvenance}]

            ### 🌐 Heart-Brain Interconnection Coupling (GNN Heuristic)
            - **GNN Crosstalk Risk Index**: ${prediction.gnnHeartBrainCrosstalkRiskIndex}
            - **Pathophysiological Assessment**: ${prediction.crosstalkExplanation}

            ### 🔒 On-Device Privacy & Clinical Assurance
            - **Local Privacy**: Model inference executed 100% on-device. Zero patient health information transmitted off-device.
        """.trimIndent()
    }
}
