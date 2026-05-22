package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.MediaItem
import com.example.vitalio_cis.model.ReportCategory
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.viewmodel.UploadReportViewModel

// ------------------------------------------------------------
// SCREEN
// ------------------------------------------------------------

@Composable
fun LabReportsScreen(
    viewModel: UploadReportViewModel = viewModel()
) {

    val colors = LocalMyColorScheme.current

    // --------------------------------------------------------
    // API DATA
    // --------------------------------------------------------
    val context = LocalContext.current

    // -----------------------------------------
    // API CALL ON SCREEN OPEN
    // -----------------------------------------

    LaunchedEffect(Unit) {

        viewModel.fetchMedia(context)
    }
    val mediaList by viewModel.mediaList.collectAsState()

    val isLoading by viewModel.loading.collectAsState()

    // --------------------------------------------------------
    // CATEGORY LIST
    // --------------------------------------------------------

    val categories = remember(mediaList) {

        mediaList
            .groupBy { it.category }
            .map {

                ReportCategory(
                    label = it.key,
                    count = it.value.size
                )
            }
    }

    // --------------------------------------------------------
    // SELECTED CATEGORY
    // --------------------------------------------------------

    var selectedCategory by remember {

        mutableStateOf("")
    }

    // --------------------------------------------------------
    // DEFAULT FIRST CATEGORY
    // --------------------------------------------------------

    LaunchedEffect(categories) {

        if (
            categories.isNotEmpty()
            &&
            selectedCategory.isEmpty()
        ) {

            selectedCategory = categories.first().label
        }
    }

    // --------------------------------------------------------
    // FILTER LIST
    // --------------------------------------------------------

    val filteredList = remember(
        mediaList,
        selectedCategory
    ) {

        mediaList.filter {

            it.category == selectedCategory
        }
    }

    // --------------------------------------------------------
    // UI
    // --------------------------------------------------------

    CommonAppBar(
        title = "Lab Reports",
        actions = {

            val navController = LocalNavController.current
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF2F80ED),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        navController.navigate(Routes.ADDLABRESULTS)
                    }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "⊕",
                    color = Color(0xFF2F80ED),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Add Report",
                    color = Color(0xFF2F80ED),
                    fontSize = 12.sp
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // CATEGORY TABS
            // ----------------------------------------------------

            if (categories.isNotEmpty()) {

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(categories) { category ->

                        CategoryTab(

                            title = category.label,

                            count = category.count,

                            selected = selectedCategory == category.label

                        ) {

                            selectedCategory = category.label
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----------------------------------------------------
            // TITLE
            // ----------------------------------------------------

            Text(
                text = "Results",
                style = AppTextStyles.style18BCB(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ----------------------------------------------------
            // LOADING
            // ----------------------------------------------------

            if (isLoading) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            // ----------------------------------------------------
            // EMPTY VIEW
            // ----------------------------------------------------

            else if (filteredList.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "No Reports Found",
                        style = AppTextStyles.style14BCB()
                    )
                }
            }

            // ----------------------------------------------------
            // LIST
            // ----------------------------------------------------

            else {

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {

                    items(filteredList) { report ->

                        ReportCard(report)

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// CATEGORY TAB
// ------------------------------------------------------------

@Composable
fun CategoryTab(
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {

    val colors = LocalMyColorScheme.current

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected)
                    colors.dashboardContainerColor
                else
                    Color.Transparent
            )
            .border(
                width = 1.dp,
                color =
                    if (selected)
                        colors.primaryBlueColor
                    else
                        Color.LightGray,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {

                onClick()
            }
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {

        Text(
            text = title,
            style = AppTextStyles.style14BCB()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$count Records",
            style = AppTextStyles.style12GCN()
        )
    }
}

// ------------------------------------------------------------
// REPORT CARD
// ------------------------------------------------------------

@Composable
fun ReportCard(report: MediaItem) {

    val colors = LocalMyColorScheme.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = colors.dashboardContainerColor
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            // ------------------------------------------------
            // LEFT SIDE
            // ------------------------------------------------

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = report.subcategory,
                    style = AppTextStyles.style14BCB()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = report.category,
                    style = AppTextStyles.style12GCN()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = report.remark,
                    style = AppTextStyles.style12GCN(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = report.dateTime,
                    style = AppTextStyles.style12GCN()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ------------------------------------------------
            // IMAGE
            // ------------------------------------------------

            AsyncImage(
                model = report.url.replace("\\", "/"),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        colors.primaryBlueColor.copy(.10f)
                    )
            )
        }
    }
}