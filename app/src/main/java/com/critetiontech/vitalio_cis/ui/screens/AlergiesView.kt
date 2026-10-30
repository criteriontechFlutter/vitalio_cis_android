package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter

import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.theme.softTint
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.model.AllergyItem as ApiAllergyItem
import com.critetiontech.ctvitalio.ui.components.AddPillButton
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.viewmodel.AllergyViewModel
import com.critetiontech.ctvitalio.ui.adaptive.rememberResponsiveDimens

// ─── Severity helpers ─────────────────────────────────────────────────────────

enum class AllergySeverity(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color,
    @androidx.annotation.StringRes val labelRes: Int
) {
    MILD("Mild", Color(0xFFEEEEFF), Color(0xFF7B7FD4), R.string.profile_severity_mild),
    MODERATE("Moderate", Color(0xFFFFF3E8), Color(0xFFE8883A), R.string.profile_severity_moderate),
    SEVERE("Severe", Color(0xFFFFEAEA), Color(0xFFE84040), R.string.profile_severity_severe)
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
    val loading by viewModel.loading.collectAsState()
    val addLoading by viewModel.addLoading.collectAsState()
    val medicineAllergies by viewModel.medicineAllergies.collectAsState()
    val foodAllergies by viewModel.foodAllergies.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val addSuccess by viewModel.addSuccess.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchAllergies()
    }

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
                viewModel.addAllergy(substanceName, severity, reaction, details, typeAllergy)
            }
        )
    }

    CommonAppBar(title = stringResource(R.string.profile_menu_allergies)) {
        val colors = LocalMyColorScheme.current
        val allergyDimens = rememberResponsiveDimens()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = allergyDimens.screenPadding)) {

                // Error banner
                if (errorMessage != null) {
                    Snackbar(
                        modifier = Modifier.padding(vertical = 8.dp),
                        action = {
                            HapticTextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.profile_dismiss), color = Color.White)
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
                    val groups = buildAllergyGroups(LocalContext.current, medicineAllergies, foodAllergies)

                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.profile_no_allergies),
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

                AddPillButton(
                    text = stringResource(R.string.profile_add_allergies),
                    loading = addLoading,
                    onClick = { showSheet = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

private fun buildAllergyGroups(
    context: android.content.Context,
    medicineAllergies: List<ApiAllergyItem>,
    foodAllergies: List<ApiAllergyItem>
): List<AllergyGroup> {
    val groups = mutableListOf<AllergyGroup>()
    if (medicineAllergies.isNotEmpty()) {
        groups.add(
            AllergyGroup(
                category = context.getString(R.string.profile_medicine_allergies),
                allergies = medicineAllergies.map { it.toUiAllergy(context) }
            )
        )
    }
    if (foodAllergies.isNotEmpty()) {
        groups.add(
            AllergyGroup(
                category = context.getString(R.string.profile_food_allergies),
                allergies = foodAllergies.map { it.toUiAllergy(context) }
            )
        )
    }
    return groups
}

private fun ApiAllergyItem.toUiAllergy(context: android.content.Context) = Allergy(
    name = substanceName.ifBlank { context.getString(R.string.profile_unknown) },
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
                AnimatedEnter(index = index) {
                    AllergyItem(allergy = allergy)
                    if (index < allergies.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = if (isAppDark) colors.borderGreyLightColor else Color(0xFFF0F1F5),
                            thickness = 1.dp
                        )
                    }
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
        HapticIconButton(
            onClick = {},
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.profile_cd_more_options),
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
            .background(color = softTint(severity.textColor, severity.backgroundColor), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(severity.labelRes), style = AppTextStyles.style12GCN(), color = severity.textColor)
    }
}

// ─── Bottom Add Bar (kept for backward compat) ────────────────────────────────

@Composable
fun AddAllergiesBar(onClick: () -> Unit) {
    Surface(
        tonalElevation = 4.dp,
        color = LocalMyColorScheme.current.dashboardContainerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            HapticTextButton(onClick = onClick) {
                Text(text = stringResource(R.string.profile_add_allergies), style = AppTextStyles.style16GCN())
            }
        }
    }
}
