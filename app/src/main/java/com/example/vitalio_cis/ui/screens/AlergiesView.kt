package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton

// ─── Data Models ─────────────────────────────────────────────────────────────

enum class AllergySeverity(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    MILD(
        label = "Mild",
        backgroundColor = Color(0xFFEEEEFF),
        textColor = Color(0xFF7B7FD4)
    ),
    MODERATE(
        label = "Moderate",
        backgroundColor = Color(0xFFFFF3E8),
        textColor = Color(0xFFE8883A)
    ),
    SEVERE(
        label = "Severe",
        backgroundColor = Color(0xFFFFEAEA),
        textColor = Color(0xFFE84040)
    )
}

data class Allergy(
    val name: String,
    val symptoms: String,
    val severity: AllergySeverity
)

data class AllergyGroup(
    val category: String,
    val allergies: List<Allergy>
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleAllergyGroups = listOf(
    AllergyGroup(
        category = "Drug Allergies",
        allergies = listOf(
            Allergy("Rifampin", "Itching, Rash", AllergySeverity.MODERATE),
            Allergy("Penicillin", "Skin Rash, Breathing Difficulty", AllergySeverity.SEVERE)
        )
    ),
    AllergyGroup(
        category = "Food Allergies",
        allergies = listOf(
            Allergy("Lemon", "Itching, Rash", AllergySeverity.MILD)
        )
    )
)

// ─── Main Screen ──────────────────────────────────────────────────────────────




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllergiesScreen(
    allergyGroups: List<AllergyGroup> = sampleAllergyGroups,
    onBackClick: () -> Unit = {},
    onAddAllergies: () -> Unit = {},
    onMoreOptions: (Allergy) -> Unit = {}
) {

    CommonAppBar(
        title = "Allergies",
    )
    {
        val colors = LocalMyColorScheme.current

        Column (
            modifier = Modifier
            .padding(horizontal = 16.dp)
        ){
            LazyColumn(
                modifier = Modifier
                    .weight(1f)   // fix
                    .fillMaxWidth() ,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                allergyGroups.forEach { group ->
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = group.category,
                            style = AppTextStyles.style14GCB(),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    item {
                        AllergyGroupCard(
                            allergies = group.allergies,
                            onMoreOptions = onMoreOptions
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            CommonButton(
                text = "+ Add Allergies",

                containerColor = colors.dashboardContainerColor,
                textStyle = AppTextStyles.style14GCN(),
                onClick = {}
            )
        }
    }
    }

// ─── Group Card (white card wrapping all items in a category) ─────────────────

@Composable
fun AllergyGroupCard(
    allergies: List<Allergy>,
    onMoreOptions: (Allergy) -> Unit
) {


    val colors = LocalMyColorScheme.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            allergies.forEachIndexed { index, allergy ->
                AllergyItem(
                    allergy = allergy,
                    onMoreOptions = { onMoreOptions(allergy) }
                )
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
fun AllergyItem(
    allergy: Allergy,
    onMoreOptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name + Symptoms
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = allergy.name,

                style = AppTextStyles.style16BCN(),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = allergy.symptoms,
                style = AppTextStyles.style12GCN()
            )
        }

        // Severity Badge
        SeverityBadge(severity = allergy.severity)

        Spacer(modifier = Modifier.width(8.dp))

        // Three-dot menu
        IconButton(
            onClick = onMoreOptions,
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
            .background(
                color = severity.backgroundColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = severity.label,
            style = AppTextStyles.style12GCN()
        )
    }
}

// ─── Bottom Add Bar ───────────────────────────────────────────────────────────

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
                Text(
                    text = "+ Add Allergies",
                    style = AppTextStyles.style16GCN()
                )
            }
        }
    }
}

