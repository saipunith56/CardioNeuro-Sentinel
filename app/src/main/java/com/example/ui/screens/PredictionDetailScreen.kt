package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDetailScreen(
    predictionId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(predictionId) {
        viewModel.loadPrediction(predictionId)
    }

    val prediction by viewModel.selectedPrediction.collectAsState()
    val patient by viewModel.selectedPatient.collectAsState()
    val encounter by viewModel.selectedEncounter.collectAsState()
    val mlSummary by viewModel.mlSummary.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()
    val isLoading by viewModel.isLoadingPrediction.collectAsState()
    val error by viewModel.predictionError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Multimodal Diagnostic Report",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back to list",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    val currentPrediction = prediction
                    val currentEncounter = encounter
                    val currentPatient = patient
                    IconButton(
                        onClick = {
                            if (currentPrediction != null) {
                                exportPdfReport(context, currentPatient, currentPrediction, currentEncounter)
                            } else {
                                Toast.makeText(context, "Report not loaded yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            isLoading -> {
                FullScreenLoading(label = "Loading diagnostic report...", modifier = Modifier.padding(innerPadding))
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorState(
                        title = "Could not load report",
                        message = error ?: "Unknown error occurred",
                        onRetry = { viewModel.loadPrediction(predictionId) }
                    )
                }
            }
            prediction == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No report available",
                        message = "This diagnostic report could not be found."
                    )
                }
            }
            else -> {
                val pred = prediction!!
                val riskColor = getRiskColor(pred.riskSeverityCategory)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ClinicalHeroBanner(
                        title = "Combined Cardiovascular & Neurological Assessment",
                        subtitle = patient?.let { p ->
                            "${p.name} • ${p.age} y/o ${p.gender} • MRN ${p.medicalRecordNumber}"
                        } ?: run {
                            "Encounter #${pred.id}"
                        },
                        icon = Icons.Default.HealthAndSafety
                    )

                    // RISK SUMMARY DASHBOARD (4 SCORES)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(0.38f),
                            contentAlignment = Alignment.Center
                        ) {
                            ChartRing(
                                percentage = pred.combinedRiskScorePct.toFloat() / 100f,
                                label = "COMBINED",
                                color = riskColor,
                                modifier = Modifier.size(125.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.62f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "Cardiovascular Risk",
                                value = "${pred.heartRiskScorePct}%",
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = ModalityEcg) },
                                accentColor = ModalityEcg
                            )
                            StatCard(
                                title = "Stroke / Neuro Risk",
                                value = "${pred.strokeRiskScorePct}%",
                                icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = ModalityEeg) },
                                accentColor = ModalityEeg
                            )
                            StatCard(
                                title = "Estimated Acute Cardiac Risk",
                                value = "${pred.acuteCardiacRiskScorePct}%",
                                icon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                accentColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    LabeledDivider("Modality Findings & Provenance")

                    // 1. CARDIOVASCULAR MODALITY CARD
                    ModalityCard(
                        title = "Cardiovascular System (ECG & Labs)",
                        icon = Icons.Default.Favorite,
                        accentColor = ModalityEcg,
                        status = "Provenance: ${pred.ecgProvenance}"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProvenanceBadge(pred.ecgProvenance)

                            if (pred.ecgProvenance == "ERROR/FAILED") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "ECG Analysis: FAILED",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            pred.heartDiagnosisLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = pred.heartDiagnosisLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            "Rhythm: ${pred.ecgArrhythmiaDetected}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = ModalityEcg
                                        )
                                    }
                                )

                                if (pred.ecgAvailable && pred.ecgProvenance == "REAL") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    EcgWaveformCanvas(
                                        ecgPreset = if (pred.ecgArrhythmiaDetected.contains("ST", ignoreCase = true)) "ST Elevation MI"
                                        else if (pred.ecgArrhythmiaDetected.contains("Atrial", ignoreCase = true)) "Atrial Fibrillation"
                                        else "Normal Sinus",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 2. NEUROLOGICAL & EEG CARD
                    ModalityCard(
                        title = "Neurological & EEG Evaluation",
                        icon = Icons.Default.Psychology,
                        accentColor = ModalityEeg,
                        status = "Provenance: ${pred.eegProvenance}"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProvenanceBadge(pred.eegProvenance)

                            if (pred.eegProvenance == "ERROR/FAILED") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "EEG Analysis: FAILED / UNAVAILABLE",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                val eegHeadline = when {
                                    pred.eegProvenance == "EXTRACTED" -> "Clinical EEG Report Assessment [OCR-EXTRACTED]"
                                    pred.eegProvenance == "REAL" -> "Cortical EEG Telemetry [ON-DEVICE AI INFERENCE]"
                                    else -> "Cortical Background Rhythm [DEMO/PRESET]"
                                }
                                Text(
                                    text = eegHeadline,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                "TOAST: ${pred.toastSubtypeClassification}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                "Seizure Risk: ${pred.eegSeizureRiskScorePct}%",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    )
                                }

                                if (pred.eegAvailable && (pred.eegProvenance == "REAL" || pred.eegProvenance == "DEMO/PRESET")) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    EegSpectrumCanvas(
                                        eegPreset = if (pred.eegSeizureRiskScorePct > 60) "Temporal Spike Wave" else "Diffuse Slowing Ischemic",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 3. NEUROIMAGING (MRI / CT) CARD
                    ModalityCard(
                        title = "Neuroimaging (Brain MRI / CT)",
                        icon = Icons.Default.Science,
                        accentColor = ModalityMri,
                        status = "Provenance: ${pred.mriProvenance}"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProvenanceBadge(pred.mriProvenance)

                            if (pred.mriProvenance == "ERROR/FAILED") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "MRI Analysis: FAILED",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "Reason: ${pred.strokeDiagnosisLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "MRI Prediction: UNAVAILABLE",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "MRI Heatmap: UNAVAILABLE",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = pred.strokeDiagnosisLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (pred.mriInfarctDetected) "Lesion Detected (${pred.mriInfarctVolumeCc} cc estimated volume)" else "No Acute Lesion Detected",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pred.mriInfarctDetected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                    MriScanViewerCanvas(
                                        mriType = if (pred.mriProvenance == "REAL") "Real Scan Slice" else "Diffusion Weighted Imaging (DWI)",
                                        hasInfarct = pred.mriInfarctDetected,
                                        infarctVolumeCc = pred.mriInfarctVolumeCc,
                                        gradCamCoordinatesJson = pred.gradCamHeatmapCoordinatesJson,
                                        isRealDicom = pred.mriProvenance == "REAL",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 4. HEART-BRAIN CROSSTALK GNN
                    SectionHeader(
                        title = "Heart-Brain Axis Crosstalk",
                        subtitle = "Graph Neural Network interconnection model",
                        icon = Icons.Default.Hub
                    )
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        HeartBrainGnnCanvas(
                            crosstalkIndex = pred.gnnHeartBrainCrosstalkRiskIndex,
                            crosstalkExplanation = pred.crosstalkExplanation,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 5. EXPLAINABLE AI (XAI)
                    SectionHeader(
                        title = "Explainable AI (XAI)",
                        subtitle = "SHAP feature weights & LIME explanations",
                        icon = Icons.Default.Lightbulb
                    )
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        ShapLimeBarChart(
                            shapJson = pred.shapTopRiskFactorsJson,
                            limeJson = pred.limeExplanationJson,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 6. SYNTHESIS SUMMARY
                    ModalityCard(
                        title = "ML Clinical Synthesis Summary",
                        icon = Icons.Default.Psychology,
                        accentColor = MaterialTheme.colorScheme.primary,
                        status = if (isGeneratingSummary) "Synthesizing..." else "Complete"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isGeneratingSummary) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "Synthesizing clinical evaluation via local ML pipeline...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = mlSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f
                                )
                            }
                        }
                    }

                    // 7. CLINICAL RECOMMENDATIONS
                    ModalityCard(
                        title = "Clinical Decision Support Recommendations",
                        icon = Icons.Default.Campaign,
                        accentColor = MaterialTheme.colorScheme.secondary
                    ) {
                        val recs = remember(pred.clinicalRecommendationsJson) {
                            val list = mutableListOf<String>()
                            try {
                                val arr = JSONArray(pred.clinicalRecommendationsJson)
                                for (i in 0 until arr.length()) {
                                    list.add(arr.getString(i))
                                }
                            } catch (_: Exception) { }
                            list
                        }

                        if (recs.isEmpty()) {
                            Text(
                                "No specific recommendations generated for this encounter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                recs.forEach { rec ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "•",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = rec,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ModelBadgeRow(
                        label = "Combined Multimodal Fusion",
                        modelName = "CardioNeuro-Sentinel Engine",
                        color = riskColor,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProvenanceBadge(provenance: String) {
    val (bg, fg) = when (provenance) {
        "REAL" -> Pair(Color(0xFF0D9488).copy(alpha = 0.15f), Color(0xFF0D9488))
        "EXTRACTED" -> Pair(Color(0xFF2563EB).copy(alpha = 0.15f), Color(0xFF2563EB))
        "ERROR/FAILED" -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        "DEMO/PRESET" -> Pair(Color(0xFFEAB308).copy(alpha = 0.15f), Color(0xFFCA8A04))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "PROVENANCE: $provenance",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: Paint): Float {
    var currentY = y
    val words = text.split(" ")
    var line = StringBuilder()
    for (word in words) {
        val testLine = if (line.isEmpty()) word else "${line} ${word}"
        val testWidth = paint.measureText(testLine)
        if (testWidth > width) {
            canvas.drawText(line.toString(), x, currentY, paint)
            currentY += paint.textSize * 1.3f
            line = StringBuilder(word)
        } else {
            line = StringBuilder(testLine)
        }
    }
    if (line.isNotEmpty()) {
        canvas.drawText(line.toString(), x, currentY, paint)
        currentY += paint.textSize * 1.3f
    }
    return currentY
}

private fun exportPdfReport(
    context: Context,
    patient: com.example.data.local.entities.PatientEntity?,
    pred: com.example.data.local.entities.PredictionResultEntity,
    encounter: com.example.data.local.entities.EncounterEntity?
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.rgb(2, 132, 199)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val normalPaint = Paint().apply {
            color = android.graphics.Color.rgb(51, 65, 85)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val disclaimerPaint = Paint().apply {
            color = android.graphics.Color.rgb(225, 29, 72)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(226, 232, 240)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // Header
        canvas.drawText("CARDIONEURO CLINICAL AI DIAGNOSTIC REPORT", 40f, 45f, titlePaint)
        canvas.drawText("CardioNeuro-Sentinel On-Device Multimodal Decision Support", 40f, 58f, subtitlePaint)
        canvas.drawLine(40f, 65f, 555f, 65f, linePaint)

        // Patient info
        canvas.drawText("PATIENT CLINICAL INFORMATION", 40f, 82f, headerPaint)
        var nextY = 98f
        canvas.drawText("Name:", 40f, nextY, boldPaint)
        canvas.drawText(patient?.name ?: "N/A", 110f, nextY, normalPaint)
        canvas.drawText("MRN:", 320f, nextY, boldPaint)
        canvas.drawText(patient?.medicalRecordNumber ?: "N/A", 390f, nextY, normalPaint)

        nextY += 13f
        canvas.drawText("Age / Sex:", 40f, nextY, boldPaint)
        canvas.drawText("${patient?.age ?: "N/A"} y/o ${patient?.gender ?: ""}", 110f, nextY, normalPaint)
        canvas.drawText("Report Date:", 320f, nextY, boldPaint)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText(dateFormat.format(Date()), 390f, nextY, normalPaint)

        canvas.drawLine(40f, nextY + 10f, 555f, nextY + 10f, linePaint)

        // Risk Overview (4 metrics)
        nextY += 26f
        canvas.drawText("MODEL-ESTIMATED RISK STRATIFICATION", 40f, nextY, headerPaint)
        nextY += 16f
        canvas.drawText("Combined Multimodal Risk:", 40f, nextY, boldPaint)
        canvas.drawText("${pred.combinedRiskScorePct}% (${pred.riskSeverityCategory})", 210f, nextY, normalPaint)

        nextY += 13f
        canvas.drawText("Cardiovascular System Risk:", 40f, nextY, boldPaint)
        canvas.drawText("${pred.heartRiskScorePct}% Risk (Tabular DS2 MLP)", 210f, nextY, normalPaint)

        nextY += 13f
        canvas.drawText("Neurological / Stroke Risk:", 40f, nextY, boldPaint)
        canvas.drawText("${pred.strokeRiskScorePct}% Risk (Tabular DS3 MLP)", 210f, nextY, normalPaint)

        nextY += 13f
        canvas.drawText("Estimated Acute Cardiac Risk:", 40f, nextY, boldPaint)
        canvas.drawText("${pred.acuteCardiacRiskScorePct}% Risk (Based on available telemetry & biomarkers)", 210f, nextY, normalPaint)

        canvas.drawLine(40f, nextY + 10f, 555f, nextY + 10f, linePaint)

        // Modality Diagnostics
        nextY += 26f
        canvas.drawText("MODALITY EVALUATIONS & PROVENANCE", 40f, nextY, headerPaint)

        nextY += 16f
        canvas.drawText("1. ECG Telemetry [Provenance: ${pred.ecgProvenance}]", 40f, nextY, boldPaint)
        nextY += 12f
        canvas.drawText("Diagnosis:", 55f, nextY, boldPaint)
        nextY = drawWrappedText(canvas, pred.heartDiagnosisLabel, 130f, nextY, 420f, normalPaint)
        canvas.drawText("Arrhythmia:", 55f, nextY, boldPaint)
        nextY = drawWrappedText(canvas, pred.ecgArrhythmiaDetected, 130f, nextY, 420f, normalPaint)

        nextY += 8f
        canvas.drawText("2. EEG Telemetry [Provenance: ${pred.eegProvenance}]", 40f, nextY, boldPaint)
        nextY += 12f
        canvas.drawText("Diagnosis:", 55f, nextY, boldPaint)
        nextY = drawWrappedText(canvas, pred.strokeDiagnosisLabel, 130f, nextY, 420f, normalPaint)
        canvas.drawText("Seizure Risk:", 55f, nextY, boldPaint)
        nextY = drawWrappedText(canvas, "${pred.eegSeizureRiskScorePct}% (TOAST: ${pred.toastSubtypeClassification})", 130f, nextY, 420f, normalPaint)

        nextY += 8f
        canvas.drawText("3. Neuroimaging (MRI/CT) [Provenance: ${pred.mriProvenance}]", 40f, nextY, boldPaint)
        nextY += 12f
        if (pred.mriProvenance == "ERROR/FAILED" || !pred.mriAvailable) {
            canvas.drawText("Infarct Status:", 55f, nextY, boldPaint)
            nextY = drawWrappedText(canvas, "UNAVAILABLE (MRI processing failed / invalid format)", 130f, nextY, 420f, normalPaint)
        } else {
            canvas.drawText("Infarct Status:", 55f, nextY, boldPaint)
            val infTxt = if (pred.mriInfarctDetected) "Detected (${pred.mriInfarctVolumeCc} cc lesion volume)" else "No acute infarct detected"
            nextY = drawWrappedText(canvas, infTxt, 130f, nextY, 420f, normalPaint)
        }

        canvas.drawLine(40f, nextY + 10f, 555f, nextY + 10f, linePaint)

        // Safety disclaimer
        nextY += 24f
        canvas.drawText("CLINICAL SAFETY & PROVENANCE NOTICE", 40f, nextY, headerPaint)
        nextY += 14f
        val disclaimerText = "• This system operates as clinical decision support. All ONNX models and OCR extractions run 100% on-device. Presets and heuristic demos are clearly designated. Final medical decisions require qualified clinician validation."
        nextY = drawWrappedText(canvas, disclaimerText, 40f, nextY, 515f, disclaimerPaint)

        // Signature Line
        nextY += 24f
        canvas.drawText("Clinician Signature: _______________________", 40f, nextY, boldPaint)
        canvas.drawText("Date: _________________", 380f, nextY, boldPaint)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "CardioNeuro_Report_${pred.patientId}_${pred.id}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.cardioneuro.sentinel.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CardioNeuro Clinical Report for ${patient?.name ?: "Patient"}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Report PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting PDF: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}
