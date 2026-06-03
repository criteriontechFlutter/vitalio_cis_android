package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.vitalio_cis.model.AllergyItem as ApiAllergyItem
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.CommonButton
import com.critetiontech.vitalio_cis.viewmodel.AllergyViewModel

// ─── Severity helpers ─────────────────────────────────────────────────────────

enum class AllergySeverity(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    MILD("Mild", Color(0xFFEEEEFF), Color(0xFF7B7FD4)),
    MODERATE("Moderate", Color(0xFFFFF3E8), Color(0xFFE8883A)),
    SEVERE("Severe", Color(0xFFFFEAEA), Color(0xFFE84040))
}

private fun String.toAllergySeverity(): AllergySeverity = when (lowercase()) {
    "severe" -> AllergySeverity.SEVERE
    "moderate" -> AllergySeverity.MODERATE
    else -> AllergySeverity.MILD
}

// ─── UI models (kept for composable reuse) ────────────────────────────────────

data class Allergy(
    val name: String,
    val symptoms: String,
    val severity: AllergySeverity
)

data class AllergyGroup(
    val category: String,
    val allergies: List<Allergy>
)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllergiesScreen(
    viewModel: AllergyViewModel = viewModel()
) {
    val context = LocalContext.current

    val loading by viewModel.loading.collectAsState()
    val addLoading by viewModel.addLoading.collectAsState()
    val medicineAllergies by viewModel.medicineAllergies.collectAsState()
    val foodAllergies by viewModel.foodAllergies.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val addSuccess by viewModel.addSuccess.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchAllergies(context)
    }

    // Dismiss sheet and refresh on successful add
    LaunchedEffect(addSuccess) {
        if (addSuccess) {
            showSheet = false
            viewModel.resetAddSuccess()
        }
    }

    if (showSheet) {
        AddAllergyBottomSheet(
            onDismiss = { showSheet = false },
            isLoading = addLoading,
            onSubmit = { substanceName, severity, reaction, details, typeAllergy ->
                viewModel.addAllergy(context, substanceName, severity, reaction, details, typeAllergy)
            }
        )
    }

    CommonAppBar(title = "Allergies") {
        val colors = LocalMyColorScheme.current

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // Error banner
                if (errorMessage != null) {
                    Snackbar(
                        modifier = Modifier.padding(vertical = 8.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss", color = Color.White)
                            }
                        }
                    ) {
                        Text(errorMessage ?: "")
                    }
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val groups = buildAllergyGroups(medicineAllergies, foodAllergies)

                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No allergies recorded yet.",
                                style = AppTextStyles.style14GCB()
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            groups.forEach { group ->
                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = group.category,
                                        style = AppTextStyles.style14GCB(),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                item {
                                    AllergyGroupCard(allergies = group.allergies)
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }

                CommonButton(
                    text = if (addLoading) "Saving..." else "+ Add Allergies",
                    containerColor = colors.dashboardContainerColor,
                    textStyle = AppTextStyles.style14GCN(),
                    onClick = { if (!addLoading) showSheet = true }
                )
            }
        }
    }
}

private fun buildAllergyGroups(
    medicineAllergies: List<ApiAllergyItem>,
    foodAllergies: List<ApiAllergyItem>
): List<AllergyGroup> {
    val groups = mutableListOf<AllergyGroup>()
    if (medicineAllergies.isNotEmpty()) {
        groups.add(
            AllergyGroup(
                category = "Medicine Allergies",
                allergies = medicineAllergies.map { it.toUiAllergy() }
            )
        )
    }
    if (foodAllergies.isNotEmpty()) {
        groups.add(
            AllergyGroup(
                category = "Food Allergies",
                allergies = foodAllergies.map { it.toUiAllergy() }
            )
        )
    }
    return groups
}

private fun ApiAllergyItem.toUiAllergy() = Allergy(
    name = substanceName.ifBlank { "Unknown" },
    symptoms = reaction.ifBlank { details.take(60) },
    severity = allergy.toAllergySeverity()
)

// ─── Group Card ───────────────────────────────────────────────────────────────

@Composable
fun AllergyGroupCard(allergies: List<Allergy>) {
    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            allergies.forEachIndexed { index, allergy ->
                AllergyItem(allergy = allergy)
                if (index < allergies.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFF0F1F5),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

// ─── Single Allergy Row ───────────────────────────────────────────────────────

@Composable
fun AllergyItem(allergy: Allergy) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = allergy.name, style = AppTextStyles.style16BCN())
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = allergy.symptoms, style = AppTextStyles.style12GCN())
        }
        SeverityBadge(severity = allergy.severity)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {},
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color(0xFFB0B5C0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Severity Badge ───────────────────────────────────────────────────────────

@Composable
fun SeverityBadge(severity: AllergySeverity) {
    Box(
        modifier = Modifier
            .background(color = severity.backgroundColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = severity.label, style = AppTextStyles.style12GCN(), color = severity.textColor)
    }
}

// ─── Bottom Add Bar (kept for backward compat) ────────────────────────────────

@Composable
fun AddAllergiesBar(onClick: () -> Unit) {
    Surface(
        tonalElevation = 4.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            TextButton(onClick = onClick) {
                Text(text = "+ Add Allergies", style = AppTextStyles.style16GCN())
            }
        }
    }
}
