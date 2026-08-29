package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.PatientEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit,
    onNavigateToNewDiagnostic: (Long) -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredPatients = patients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.medicalRecordNumber.contains(searchQuery, ignoreCase = true) ||
        it.primaryCondition.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Patient Directory",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.testTag("add_patient_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register new patient")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Patients",
                        value = "${patients.size}",
                        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active Cases",
                        value = "${patients.size}",
                        icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ModalityClinical) },
                        accentColor = ModalityClinical,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_search_input"),
                    placeholder = {
                        Text(
                            "Search name, MRN, condition...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredPatients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (searchQuery.isNotBlank()) "No matching patients" else "No patients registered",
                        message = if (searchQuery.isNotBlank()) "Try a different search term." else "Tap + below to register your first patient."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "All Patients",
                            subtitle = "${filteredPatients.size} record${if (filteredPatients.size != 1) "s" else ""}",
                            icon = Icons.Default.StackedLineChart
                        )
                    }
                    items(filteredPatients) { patient ->
                        PatientRowCard(
                            patient = patient,
                            onClick = { onNavigateToPatientDetail(patient.id) },
                            onLaunchDiagnostic = { onNavigateToNewDiagnostic(patient.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPatientDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mrn, age, gender, bloodGroup, condition, sysBp, diaBp, chol, gluc, bmi, smoker, diabetic, famHeart, famStroke ->
                viewModel.addNewPatient(
                    name = name,
                    mrn = mrn,
                    age = age,
                    gender = gender,
                    bloodGroup = bloodGroup,
                    primaryCondition = condition,
                    systolicBp = sysBp,
                    diastolicBp = diaBp,
                    cholesterol = chol,
                    glucose = gluc,
                    bmi = bmi,
                    smoker = smoker,
                    diabetic = diabetic,
                    familyHeart = famHeart,
                    familyStroke = famStroke
                ) { newId ->
                    showAddDialog = false
                    onNavigateToPatientDetail(newId)
                }
            }
        )
    }
}

@Composable
private fun PatientRowCard(
    patient: PatientEntity,
    onClick: () -> Unit,
    onLaunchDiagnostic: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("patient_item_${patient.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Patient Info + Google Lens Scanner Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = patient.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${patient.gender}, ${patient.age} y/o  •  ${patient.medicalRecordNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sleek Google Lens / Scanner Action Button
                FilledTonalIconButton(
                    onClick = onLaunchDiagnostic,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ModalityClinical.copy(alpha = 0.12f),
                        contentColor = ModalityClinical
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("launch_ai_diagnostic_button_${patient.id}")
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "AI Scanner (Google Lens)",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Vitals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VitalStatItem(
                    label = "Blood Pressure",
                    value = "${patient.systolicBp}/${patient.diastolicBp}",
                    accentColor = ModalityEcg,
                    modifier = Modifier.weight(1.1f)
                )
                VitalStatItem(
                    label = "Cholesterol",
                    value = "${patient.cholesterolMgDl} mg/dL",
                    accentColor = ModalityClinical,
                    modifier = Modifier.weight(1.1f)
                )
                VitalStatItem(
                    label = "Blood Group",
                    value = patient.bloodGroup,
                    accentColor = ModalityMri,
                    modifier = Modifier.weight(0.8f)
                )
            }

            // Bottom Condition Chip & Direct Action Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (patient.primaryCondition.isNotBlank()) {
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                patient.primaryCondition,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLaunchDiagnostic)
                ) {
                    Text(
                        text = "New Scan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ModalityClinical
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ModalityClinical,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, String, Int, Int, Int, Int, Float, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mrn by remember { mutableStateOf("MRN-${(100000..999999).random()}") }
    var age by remember { mutableStateOf("62") }
    var gender by remember { mutableStateOf("Male") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var condition by remember { mutableStateOf("Hypertension & Atherosclerosis") }
    var sysBp by remember { mutableStateOf("154") }
    var diaBp by remember { mutableStateOf("92") }
    var chol by remember { mutableStateOf("225") }
    var gluc by remember { mutableStateOf("115") }
    var smoker by remember { mutableStateOf(true) }
    var diabetic by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val bloodOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Register New Patient",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    var genderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender", style = MaterialTheme.typography.bodySmall) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        gender = option
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sysBp,
                        onValueChange = { sysBp = it },
                        label = { Text("Systolic BP", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = diaBp,
                        onValueChange = { diaBp = it },
                        label = { Text("Diastolic BP", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = chol,
                    onValueChange = { chol = it },
                    label = { Text("Cholesterol (mg/dL)", style = MaterialTheme.typography.bodySmall) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                var bgExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bgExpanded,
                    onExpandedChange = { bgExpanded = !bgExpanded }
                ) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood Group", style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = bgExpanded,
                        onDismissRequest = { bgExpanded = false }
                    ) {
                        bloodOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    bloodGroup = option
                                    bgExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(checked = smoker, onCheckedChange = { smoker = it })
                    Text("Current Smoker", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(checked = diabetic, onCheckedChange = { diabetic = it })
                    Text("Diabetic", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name, mrn, age.toIntOrNull() ?: 60, gender, bloodGroup, condition,
                            sysBp.toIntOrNull() ?: 140, diaBp.toIntOrNull() ?: 90,
                            chol.toIntOrNull() ?: 200, gluc.toIntOrNull() ?: 100,
                            26.5f, smoker, diabetic, true, false
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Register Patient", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
