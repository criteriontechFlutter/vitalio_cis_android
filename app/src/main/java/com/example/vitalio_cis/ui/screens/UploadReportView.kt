package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel

// ---------------- SAMPLE STATIC DATA ----------------

data class ReportCategory(
    val label: String,
    val recordCount: Int,
    val isSelected: Boolean = false
)

data class LabReport(
    val title: String,
    val type: String,
    val description: String,
    val date: String
)

val sampleCategories = listOf(
    ReportCategory("Radiology", 1, true),
    ReportCategory("Imaging", 2),
    ReportCategory("Lab", 4)
)

val sampleReports = listOf(
    LabReport(
        "Abdomen X Ray",
        "Findings",
        "Dummy text ever since the 1500s when printer took a galley and scrambled it.",
        "02 Aug 2024"
    ),
    LabReport(
        "Blood Test",
        "Lab",
        "CBC report with all parameters normal.",
        "05 Aug 2024"
    )
)


// ---------------- SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabReportsScreen(
    categories: List<ReportCategory> = sampleCategories,
    reports: List<LabReport> = sampleReports,
    onBackClick: () -> Unit = {}
) {


    val colors = LocalMyColorScheme.current
    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull())
    }



    val navController = LocalNavController.current

        CommonAppBar(
            title = "Lab Reports",
            actions = {
                Row( modifier = Modifier
                    .clickable(){
                        navController.navigate(Routes.ADDLABRESULTS)

                    }) {
                    Text("Add Report")
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .verticalScroll(rememberScrollState())
            )
            {

                CategoryTabRow(categories, selectedCategory) {
                    selectedCategory = it
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Results",
                    style = AppTextStyles.style18BCB(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(12.dp))

                reports.forEach {
                    ReportCard(it)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
}


// ---------------- ADD BUTTON ----------------

@Composable
fun AddReportButton() {

    val colors = LocalMyColorScheme.current

    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, colors.primaryBlueColor, RoundedCornerShape(20.dp))
            .clickable {}
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = colors.primaryBlueColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            "Add Report",
            style = AppTextStyles.style12BCN()
        )
    }
}


// ---------------- CATEGORY ROW ----------------

@Composable
fun CategoryTabRow(
    categories: List<ReportCategory>,
    selected: ReportCategory?,
    onSelect: (ReportCategory) -> Unit
) {

    LazyRow(
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        items(categories) { item ->

            CategoryTab(
                category = item,
                selected = item == selected,
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
fun CategoryTab(
    category: ReportCategory,
    selected: Boolean,
    onClick: () -> Unit
) {

    val colors = LocalMyColorScheme.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected)
                    colors.dashboardContainerColor
                else
                    Color.Transparent
            )
            .border(
                1.dp,
                if (selected) colors.primaryBlueColor else Color.LightGray,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {

        Column {

            Text(
                category.label,
                style = AppTextStyles.style12BCN()
            )

            Text(
                "${category.recordCount} Records",
                style = AppTextStyles.style12GCN()
            )
        }
    }
}


// ---------------- REPORT CARD ----------------

@Composable
fun ReportCard(report: LabReport) {

    val colors = LocalMyColorScheme.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.dashboardContainerColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    report.title,
                    style = AppTextStyles.style14BCB()
                )

                Text(
                    report.type,
                    style = AppTextStyles.style12GCN()
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    report.description,
                    style = AppTextStyles.style12GCN(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    report.date,
                    style = AppTextStyles.style12GCN()
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryBlueColor.copy(.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🩻", fontSize = 30.sp)
            }
        }
    }
}


// ---------------- PREVIEW ----------------

@Preview(showBackground = true)
@Composable
fun PreviewLabReports() {
    LabReportsScreen()
}