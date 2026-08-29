package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederatedPrivacyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToModelEvaluation: () -> Unit
) {
    val nodes by viewModel.federatedNodes.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    var epsilonValue by remember { mutableStateOf(0.5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Privacy",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("privacy_back_button")
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ClinicalHeroBanner(
                    title = "System Settings & Privacy",
                    subtitle = "Customize appearance theme and inspect on-device differential privacy protocols.",
                    icon = Icons.Default.GppGood
                )
            }

            // Theme Switcher Card (Day / Night Mode Toggle)
            item {
                ModalityCard(
                    title = "App Appearance & Theme",
                    icon = Icons.Default.Settings,
                    accentColor = if (isDarkTheme) ModalityEeg else ModalityClinical,
                    status = if (isDarkTheme) "Dark / Night" else "Light / Day"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDarkTheme) "Night Theme (Midnight Slate)" else "Day Theme (Clean Medical White)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isDarkTheme) "High-contrast dark clinical background for low-light environments." else "Crisp, bright white clinical interface with maximum daytime visibility.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) }
                            )
                        }
                    }
                }
            }

            item {
                ModalityCard(
                    title = "Model Performance Audit",
                    icon = Icons.Default.Insights,
                    accentColor = MaterialTheme.colorScheme.primary,
                    status = "5 ONNX Models"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Inspect validation metrics, dataset references, confusion matrices, and reproducibility parity for the five integrated clinical ONNX models.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
                        )
                        Button(
                            onClick = onNavigateToModelEvaluation,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_navigate_model_evaluation")
                        ) {
                            Icon(
                                Icons.Default.Dataset,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Open Model Performance Panel",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                ModalityCard(
                    title = "Differential Privacy Budget",
                    icon = Icons.Default.Lock,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    status = "ε = ${"%.2f".format(epsilonValue)}"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "ε ${"%.2f".format(epsilonValue)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Gaussian noise scale γ = 0.02",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = epsilonValue,
                            onValueChange = { epsilonValue = it },
                            valueRange = 0.1f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("epsilon_privacy_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "0.1 (Strict)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "2.0 (Relaxed)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Health records remain strictly on-device. Only encrypted gradient updates are shared across consortium nodes using traditional FedAvg.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.35f
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Federated Node Consortium",
                    subtitle = "${nodes.size} active training participant${if (nodes.size != 1) "s" else ""}",
                    icon = Icons.Default.CloudSync
                )
            }

            if (nodes.isEmpty()) {
                item {
                    EmptyState(
                        title = "No active consortium nodes",
                        message = "Federated node configuration will appear here when the cluster is provisioned."
                    )
                }
            } else {
                items(nodes) { node ->
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = ModalityClinical.copy(alpha = 0.12f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.LocationCity,
                                                contentDescription = null,
                                                tint = ModalityClinical,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = node.hospitalNodeName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = RiskLow.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        node.status,
                                        color = RiskLow,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VitalStatItem(
                                    label = "Local Epochs",
                                    value = "${node.localEpochsCompleted}",
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                SpecRow(
                                    label = "Last Weight Hash",
                                    value = node.lastWeightHash
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
