package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val predictions by viewModel.predictions.collectAsState()

    val totalCount = predictions.size
    val highRiskCount = predictions.count {
        it.riskSeverityCategory == "HIGH" || it.riskSeverityCategory == "CRITICAL"
    }
    val cardioembolicCount = predictions.count { it.toastSubtypeClassification.contains("Cardioembolism") }
    val laaCount = predictions.count { it.toastSubtypeClassification.contains("Large-Artery") }
    val svoCount = predictions.count { it.toastSubtypeClassification.contains("Small-Vessel") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Population Analytics",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analytics_back_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ClinicalHeroBanner(
                    title = "Population Disease Trends",
                    subtitle = "Aggregated analytics across all screening encounters",
                    icon = Icons.Default.BarChart
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Encounters",
                        value = "$totalCount",
                        icon = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "High-Risk Cases",
                        value = "$highRiskCount",
                        trend = if (totalCount > 0) "${(highRiskCount * 100f / totalCount).toInt()}% of cohort" else "0%",
                        trendPositive = false,
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = RiskHigh) },
                        accentColor = RiskHigh,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ModalityCard(
                    title = "Stroke Etiology: TOAST Subtype Distribution",
                    icon = Icons.Default.Insights,
                    accentColor = ModalityEeg
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AnalyticsProgressItem(
                            label = "Cardioembolism (CE)",
                            count = cardioembolicCount,
                            total = totalCount,
                            color = ModalityEcg
                        )
                        AnalyticsProgressItem(
                            label = "Large-Artery Atherosclerosis (LAA)",
                            count = laaCount,
                            total = totalCount,
                            color = ModalityClinical
                        )
                        AnalyticsProgressItem(
                            label = "Small-Vessel Occlusion (SVO)",
                            count = svoCount,
                            total = totalCount,
                            color = ModalityGnn
                        )
                        if (totalCount == 0) {
                            Text(
                                "Run diagnostic encounters to populate population analytics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsProgressItem(label: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) count.toFloat() / total else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "$count (${(pct * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {}
        )
    }
}
