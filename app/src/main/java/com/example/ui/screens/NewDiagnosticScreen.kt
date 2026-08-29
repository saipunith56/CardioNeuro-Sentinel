package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDiagnosticScreen(
    patientId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPredictionComplete: (Long) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    val patient by viewModel.selectedPatient.collectAsState()

    var age by remember { mutableStateOf("62") }
    var bmi by remember { mutableStateOf("28.4") }
    var sysBp by remember { mutableStateOf("145") }
    var diaBp by remember { mutableStateOf("92") }
    var heartRate by remember { mutableStateOf("88") }

    var cholesterol by remember { mutableStateOf("215") }
    var fastingGlucose by remember { mutableStateOf("126") }

    var isSmoker by remember { mutableStateOf(true) }
    var familyHeartHistory by remember { mutableStateOf(true) }
    var familyStrokeHistory by remember { mutableStateOf(false) }
    var reportedSymptoms by remember {
        mutableStateOf("Acute palpitations, dizziness, and mild facial numbness")
    }

    var nihssScore by remember { mutableStateOf("6") }
    var troponin by remember { mutableStateOf("0.14") }

    // Uploaded Real File States
    var ecgFilePathState by remember { mutableStateOf<String?>(null) }
    var ecgFileName by remember { mutableStateOf<String?>(null) }

    var eegFilePathState by remember { mutableStateOf<String?>(null) }
    var eegFileName by remember { mutableStateOf<String?>(null) }

    var mriFilePathState by remember { mutableStateOf<String?>(null) }
    var mriFileName by remember { mutableStateOf<String?>(null) }

    // Presets for Demo / Fallback Mode
    var selectedEcgPreset by remember { mutableStateOf("Atrial Fibrillation") }
    var selectedEegPreset by remember { mutableStateOf("Diffuse Slowing Ischemic") }
    var selectedMriPreset by remember { mutableStateOf("Diffusion Weighted Imaging (DWI)") }

    var showAdvancedOptions by remember { mutableStateOf(false) }
    var isRunningInference by remember { mutableStateOf(false) }

    // File pickers for ECG, EEG, and MRI
    val ecgFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val resolvedName = getFileName(context, selectedUri)
                val ext = if (resolvedName.contains(".")) resolvedName.substringAfterLast(".") else "jpg"
                val destinationFile = File(context.filesDir, "imported_ecg_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                ecgFileName = resolvedName
                ecgFilePathState = destinationFile.absolutePath
                Toast.makeText(context, "ECG input selected: $resolvedName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load ECG: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val eegFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val resolvedName = getFileName(context, selectedUri)
                val ext = if (resolvedName.contains(".")) resolvedName.substringAfterLast(".") else "jpg"
                val destinationFile = File(context.filesDir, "imported_eeg_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                eegFileName = resolvedName
                eegFilePathState = destinationFile.absolutePath
                Toast.makeText(context, "EEG input selected: $resolvedName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load EEG: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val mriFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val resolvedName = getFileName(context, selectedUri)
                val ext = if (resolvedName.contains(".")) resolvedName.substringAfterLast(".") else "jpg"
                val destinationFile = File(context.filesDir, "imported_mri_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                mriFileName = resolvedName
                mriFilePathState = destinationFile.absolutePath
                Toast.makeText(context, "MRI/CT input selected: $resolvedName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load MRI/CT: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(patient) {
        patient?.let { p ->
            age = p.age.toString()
            sysBp = p.systolicBp.toString()
            diaBp = p.diastolicBp.toString()
            cholesterol = p.cholesterolMgDl.toString()
            fastingGlucose = p.fastingGlucoseMgDl.toString()
            bmi = p.bmi.toString()
            isSmoker = p.smoker
            familyHeartHistory = p.familyHistoryHeartDisease
            familyStrokeHistory = p.familyHistoryStroke
        }
    }

    val ecgOptions = listOf("Atrial Fibrillation", "ST Elevation MI", "VTach", "Normal Sinus")
    val eegOptions = listOf("Diffuse Slowing Ischemic", "Temporal Spike Wave", "Normal Alpha")
    val mriOptions = listOf("Diffusion Weighted Imaging (DWI)", "Axial T2-FLAIR", "CT Angiography")

    LoadingOverlay(
        isLoading = isRunningInference,
        message = "Running Multimodal AI & Ingestion Pipeline..."
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "New Diagnostic Assessment",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = patient?.name ?: "Multimodal Clinical Intake",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("new_diagnostic_back")
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back to Patient",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Spacing.containerMargin,
                        vertical = Spacing.md
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {

                // 1. PATIENT DEMOGRAPHICS & VITALS
                SectionHeader(
                    title = "Patient Clinical Data",
                    subtitle = "Vitals, anthropometrics, and labs",
                    icon = Icons.Default.Favorite
                )

                ModalityCard(
                    title = "Vitals & Demographics",
                    icon = Icons.Default.Favorite,
                    accentColor = ModalityClinical
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                label = { Text("Age (years)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("age_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedTextField(
                                value = bmi,
                                onValueChange = { bmi = it },
                                label = { Text("BMI (kg/m²)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("bmi_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = "$sysBp / $diaBp",
                                onValueChange = { input ->
                                    val parts = input.split("/")
                                    if (parts.isNotEmpty()) sysBp = parts[0].trim()
                                    if (parts.size > 1) diaBp = parts[1].trim()
                                },
                                label = { Text("Blood Pressure (mmHg)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("bp_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedTextField(
                                value = heartRate,
                                onValueChange = { heartRate = it },
                                label = { Text("Heart Rate (bpm)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("hr_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = cholesterol,
                                onValueChange = { cholesterol = it },
                                label = { Text("Cholesterol (mg/dL)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("cholesterol_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedTextField(
                                value = fastingGlucose,
                                onValueChange = { fastingGlucose = it },
                                label = { Text("Fasting Glucose (mg/dL)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("glucose_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedTextField(
                                value = troponin,
                                onValueChange = { troponin = it },
                                label = { Text("Troponin I (ng/mL)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("troponin_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedTextField(
                                value = nihssScore,
                                onValueChange = { nihssScore = it },
                                label = { Text("NIH Stroke Scale (0-42)", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("nihss_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Smoker", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isSmoker,
                                onCheckedChange = { isSmoker = it },
                                modifier = Modifier.testTag("smoker_switch")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = familyHeartHistory,
                                onCheckedChange = { familyHeartHistory = it },
                                modifier = Modifier.testTag("fam_heart_checkbox")
                            )
                            Text("Family History of Cardiovascular Disease", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = familyStrokeHistory,
                                onCheckedChange = { familyStrokeHistory = it },
                                modifier = Modifier.testTag("fam_stroke_checkbox")
                            )
                            Text("Family History of Cerebrovascular Stroke", style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedTextField(
                            value = reportedSymptoms,
                            onValueChange = { reportedSymptoms = it },
                            label = { Text("Chief Complaint / Reported Symptoms", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth().testTag("symptoms_input"),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // 2. CARDIAC ANALYSIS (ECG Image or Report)
                SectionHeader(
                    title = "Cardiac Telemetry (12-Lead ECG)",
                    subtitle = "Upload ECG image, report photo, or waveform",
                    icon = Icons.Default.Favorite
                )

                ModalityCard(
                    title = "12-Lead ECG Analysis",
                    icon = Icons.Default.Favorite,
                    accentColor = ModalityEcg,
                    status = if (ecgFilePathState != null) "FILE LOADED" else "PRESET"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        UploadCardCompact(
                            title = "ECG Image / Report Photo",
                            buttonText = "Upload ECG Image or Report",
                            fileName = ecgFileName,
                            accentColor = ModalityEcg,
                            onUploadClick = { ecgFilePickerLauncher.launch("*/*") },
                            onClearClick = {
                                ecgFilePathState = null
                                ecgFileName = null
                            }
                        )

                        if (ecgFilePathState == null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No ECG file selected. Fallback preset '$selectedEcgPreset' will be used in Demo/Preset mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 3. NEUROLOGICAL ANALYSIS (EEG & MRI)
                SectionHeader(
                    title = "Neurological & Neuroimaging",
                    subtitle = "Upload EEG and Brain MRI/CT scan or report",
                    icon = Icons.Default.Psychology
                )

                ModalityCard(
                    title = "EEG Electroencephalography",
                    icon = Icons.Default.Psychology,
                    accentColor = ModalityEeg,
                    status = if (eegFilePathState != null) "FILE LOADED" else "PRESET"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        UploadCardCompact(
                            title = "EEG Image / Report Photo",
                            buttonText = "Upload EEG Image or Report",
                            fileName = eegFileName,
                            accentColor = ModalityEeg,
                            onUploadClick = { eegFilePickerLauncher.launch("*/*") },
                            onClearClick = {
                                eegFilePathState = null
                                eegFileName = null
                            }
                        )

                        if (eegFilePathState == null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No EEG file selected. Fallback preset '$selectedEegPreset' will be used in Demo/Preset mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                ModalityCard(
                    title = "Neuroimaging (MRI / CT)",
                    icon = Icons.Default.Science,
                    accentColor = ModalityMri,
                    status = if (mriFilePathState != null) "FILE LOADED" else "PRESET"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        UploadCardCompact(
                            title = "MRI / CT Image or Report",
                            buttonText = "Upload MRI / CT Image or Report",
                            fileName = mriFileName,
                            accentColor = ModalityMri,
                            onUploadClick = { mriFilePickerLauncher.launch("*/*") },
                            onClearClick = {
                                mriFilePathState = null
                                mriFileName = null
                            }
                        )

                        if (mriFilePathState == null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No MRI file selected. Fallback preset '$selectedMriPreset' will be used in Demo/Preset mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 4. ADVANCED / RAW DATA INPUT & PRESETS (COLLAPSIBLE)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedOptions = !showAdvancedOptions },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Advanced / Raw Data & Simulation Presets",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedOptions) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Select baseline presets for unsupplied modalities (Used ONLY when no real file is uploaded):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LabeledDivider(label = "ECG Preset")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    ecgOptions.forEach { option ->
                                        FilterChip(
                                            selected = selectedEcgPreset == option,
                                            onClick = { selectedEcgPreset = option },
                                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                LabeledDivider(label = "EEG Preset")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    eegOptions.forEach { option ->
                                        FilterChip(
                                            selected = selectedEegPreset == option,
                                            onClick = { selectedEegPreset = option },
                                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                LabeledDivider(label = "MRI Preset")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    mriOptions.forEach { option ->
                                        FilterChip(
                                            selected = selectedMriPreset == option,
                                            onClick = { selectedMriPreset = option },
                                            label = { Text(option.take(18), style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 5. GENERATE AI PREDICTION BUTTON
                Button(
                    onClick = {
                        isRunningInference = true
                        viewModel.runDiagnosticEncounter(
                            patientId = patientId,
                            chiefComplaint = reportedSymptoms,
                            heartRate = heartRate.toIntOrNull() ?: 88,
                            respiratoryRate = 18,
                            oxygenSat = 98,
                            nihssScore = nihssScore.toIntOrNull() ?: 6,
                            troponin = troponin.toFloatOrNull() ?: 0.14f,
                            ecgPreset = ecgFilePathState ?: selectedEcgPreset,
                            eegPreset = eegFilePathState ?: selectedEegPreset,
                            mriType = mriFilePathState ?: selectedMriPreset,
                            notes = reportedSymptoms,
                            age = age.toIntOrNull() ?: 62,
                            systolicBp = sysBp.toIntOrNull() ?: 120,
                            diastolicBp = diaBp.toIntOrNull() ?: 80,
                            cholesterol = cholesterol.toIntOrNull() ?: 200,
                            glucose = fastingGlucose.toIntOrNull() ?: 100,
                            bmi = bmi.toFloatOrNull() ?: 25.0f,
                            smoker = isSmoker,
                            familyHeart = familyHeartHistory,
                            familyStroke = familyStrokeHistory,
                            onComplete = { predictionId ->
                                isRunningInference = false
                                onPredictionComplete(predictionId)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("generate_prediction_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isRunningInference
                ) {
                    if (isRunningInference) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Running ML Pipeline & OCR Ingestion...",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    } else {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Generate AI Prediction",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clinical Decision Support System. Requires certified physician verification.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun UploadCardCompact(
    title: String,
    buttonText: String,
    fileName: String?,
    accentColor: Color,
    onUploadClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (fileName != null) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "REAL FILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (fileName != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                fileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear file",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onUploadClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

private fun getFileName(
    context: android.content.Context,
    uri: Uri
): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "imported_file"
}
