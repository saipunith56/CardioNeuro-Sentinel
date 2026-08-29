package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.PredictionResultEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPatientList: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit,
    onNavigateToPredictionDetail: (Long) -> Unit,
    onNavigateToFederatedPrivacy: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    val predictions by viewModel.predictions.collectAsState()

    val criticalCount = predictions.count {
        it.riskSeverityCategory == "CRITICAL" || it.riskSeverityCategory == "HIGH"
    }
    val moderateCount = predictions.count { it.riskSeverityCategory == "MODERATE" }
    val lowCount = predictions.count { it.riskSeverityCategory == "LOW" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "CardioNeuro Sentinel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Multimodal Clinical AI Decision Support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToFederatedPrivacy,
                        modifier = Modifier.testTag("federated_privacy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Federated Privacy & Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.testTag("analytics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Population Analytics",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.containerMargin,
                end = Spacing.containerMargin,
                top = Spacing.md,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                ClinicalHeroBanner(
                    title = "Independent Clinical AI Assessment",
                    subtitle = "XGBoost • Random Forest • 2D CNN • 1D-CNN • GNN • Federated DP",
                    icon = Icons.Default.Psychology,
                    primaryAction = "Patient Directory (${patients.size})" to onNavigateToPatientList,
                    brushColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("hero_banner_card")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatCard(
                        title = "Critical / High Risk",
                        value = "$criticalCount Cases",
                        modifier = Modifier.weight(1f),
                        accentColor = RiskCritical,
                        icon = {
                            Surface(
                                color = RiskCritical,
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                    )
                    StatCard(
                        title = "Moderate Risk",
                        value = "$moderateCount Cases",
                        modifier = Modifier.weight(1f),
                        accentColor = RiskModerate,
                        icon = {
                            Surface(
                                color = RiskModerate,
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatCard(
                        title = "Low Risk",
                        value = "$lowCount Cases",
                        modifier = Modifier.weight(1f),
                        accentColor = RiskLow,
                        icon = {
                            Surface(
                                color = RiskLow,
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                    )
                    StatCard(
                        title = "Federated Node",
                        value = "ε = 0.5 (Active)",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        icon = {
                            Surface(
                                color = RiskLow,
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Model Architecture Inventory",
                    subtitle = "Verified ONNX inference pipeline",
                    icon = Icons.Default.Dns
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        ModelBadgeRow(
                            "Tabular Vitals & Biomarkers",
                            "XGBoost + Random Forest",
                            ModalityClinical
                        )
                        ModelBadgeRow(
                            "CT/MRI Lesion Segmentation",
                            "2D Convolutional Neural Net",
                            ModalityMri
                        )
                        ModelBadgeRow(
                            "ECG & EEG Telemetry",
                            "1D-CNN Spectral Classifier",
                            ModalityEcg
                        )
                        ModelBadgeRow(
                            "Heart-Brain Axis Crosstalk",
                            "Graph Neural Network (GNN)",
                            ModalityGnn
                        )
                        ModelBadgeRow(
                            "Privacy & Weight Sync",
                            "Federated DP (FedAvg)",
                            MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Recent AI Diagnostic Reports",
                    subtitle = "${predictions.size} total assessments",
                    icon = Icons.Default.Assessment,
                    action = {
                        TextButton(onClick = onNavigateToAnalytics) {
                            Text(
                                "View Analytics",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            if (predictions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Diagnostic Reports Yet",
                        message = "Navigate to Patient Directory and start a new AI multimodal screening to generate predictions.",
                        icon = Icons.Default.Description,
                        actionLabel = "Go to Patients",
                        onAction = onNavigateToPatientList,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                items(predictions) { pred ->
                    PredictionCardItem(
                        prediction = pred,
                        patientName = patients.find { it.id == pred.patientId }?.name
                            ?: "Patient #${pred.patientId}",
                        onClick = { onNavigateToPredictionDetail(pred.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
fun PredictionCardItem(
    prediction: PredictionResultEntity,
    patientName: String,
    onClick: () -> Unit
) {
    val severityColor = getRiskColor(prediction.riskSeverityCategory)
    val riskLevel = getRiskLevelFromCategory(prediction.riskSeverityCategory)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prediction_card_${prediction.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = patientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = prediction.heartDiagnosisLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                RiskBadge(riskLevel = riskLevel, compact = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ModalityClinical.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Combined Risk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${prediction.combinedRiskScorePct}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor
                        )
                    }
                }

                Surface(
                    color = ModalityMri.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "TOAST Subtype",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = prediction.toastSubtypeClassification,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModalityMri
                        )
                    }
                }

                Surface(
                    color = ModalityGnn.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "GNN Crosstalk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = prediction.gnnHeartBrainCrosstalkRiskIndex.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ModalityGnn
                        )
                    }
                }
            }
        }
    }
}
