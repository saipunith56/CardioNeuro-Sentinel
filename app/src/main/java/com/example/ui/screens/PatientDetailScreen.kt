package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalInformation

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.ui.MainViewModel
import com.example.data.local.entities.PredictionResultEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// Removed duplicate dp import
import androidx.compose.ui.graphics.Color
// import removed: MonitorHeart icon not available
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToNewDiagnostic: (Long) -> Unit,
    onNavigateToPredictionDetail: (Long) -> Unit
) {
    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    val patient by viewModel.selectedPatient.collectAsState()
    val allPredictions by viewModel.predictions.collectAsState()
    // Explicitly type the filtered predictions list to aid type inference
    val patientPredictions: List<PredictionResultEntity> = allPredictions.filter { it.patientId == patientId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        patient?.name ?: "Patient Profile",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("patient_detail_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
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
        patient?.let { p ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${p.gender}, ${p.age} years old",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "MRN ${p.medicalRecordNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onNavigateToNewDiagnostic(p.id) },
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.testTag("start_ai_multimodal_screening"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "New AI Scan",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (p.primaryCondition.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            p.primaryCondition,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Sick,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                            }

                            LabeledDivider("Baseline Vitals")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VitalStatItem(
                                    label = "Blood Pressure",
                                    value = "${p.systolicBp}/${p.diastolicBp} mmHg",
                                    accentColor = ModalityEcg,
                                    modifier = Modifier.weight(1f)
                                )
                                VitalStatItem(
                                    label = "Cholesterol",
                                    value = "${p.cholesterolMgDl} mg/dL",
                                    accentColor = ModalityClinical,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VitalStatItem(
                                    label = "Fasting Glucose",
                                    value = "${p.fastingGlucoseMgDl} mg/dL",
                                    accentColor = ModalityMri,
                                    modifier = Modifier.weight(1f)
                                )
                                VitalStatItem(
                                    label = "BMI",
                                    value = "%.1f".format(p.bmi),
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VitalStatItem(
                                    label = "Blood Group",
                                    value = p.bloodGroup,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                VitalStatItem(
                                    label = "Risk Factors",
                                    value = buildString {
                                        var count = 0
                                        if (p.smoker) append("Smoker ").also { count++ }
                                        if (p.diabetic) append("Diabetic ").also { count++ }
                                        if (count == 0) append("None")
                                    }.trim(),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Diagnostic History",
                        subtitle = "${patientPredictions.size} AI screening encounter${if (patientPredictions.size != 1) "s" else ""}",
                        icon = Icons.Default.MedicalInformation,
                        
                    )
                }

                if (patientPredictions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = "No screening history",
                                message = "Run this patient through the AI diagnostic pipeline to see reports here."
                            )
                        }
                    }
                } else {
                    items(patientPredictions) { pred ->
                        PredictionCardItem(
                            prediction = pred,
                            patientName = p.name,
                            onClick = { onNavigateToPredictionDetail(pred.id) }
                        )
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Patient not found",
                    message = "This patient record could not be loaded."
                )
            }
        }
    }
}
