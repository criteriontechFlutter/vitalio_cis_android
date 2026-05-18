package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.viewmodel.SymptomTrackerViewModel
import kotlinx.coroutines.delay

@Composable
fun SymptomsView(viewModel: SymptomTrackerViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    LaunchedEffect(Unit) {
        viewModel.getProblemsWithIcon(context)
    }

    var searchText by remember { mutableStateOf("") }
    var selectedSymptoms by remember { mutableStateOf(listOf<Int>()) }

    val defaultList by viewModel.symptomIconsList.collectAsState()
    val searchList by viewModel.searchedsymptomList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val addLoading by viewModel.addLoading.collectAsState()

    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            viewModel.clearSearchResults()
        } else {
            delay(300)
            viewModel.getAllProblems(context, searchText)
        }
    }

    CommonAppBar(title = "Symptom Tracker") {
        val themeViewModel: ThemeViewModel = viewModel()
        val colors by themeViewModel.colorScheme.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            MyTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholderText = "Search Symptoms i.e cold, cough"
            )

            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            // Search results dropdown
            if (searchText.isNotEmpty() && searchList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
                        items(searchList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.dashboardContainerColor)
                                    .clickable {
                                        selectedSymptoms =
                                            if (selectedSymptoms.contains(item.problemId))
                                                selectedSymptoms - item.problemId
                                            else
                                                selectedSymptoms + item.problemId
                                        searchText = ""
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.displayIcon,
                                    contentDescription = item.problemName,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = item.problemName)
                            }
                        }
                    }
                }
            }

            // Selected symptom chips
            if (selectedSymptoms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow {
                    items(selectedSymptoms) { id ->
                        val item = (defaultList + searchList).find { it.problemId == id }
                        item?.let {
                            SelectedChip(it.problemName) {
                                selectedSymptoms = selectedSymptoms - id
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(defaultList.size) { index ->
                    val item = defaultList[index]
                    SymptomItem(
                        title = item.problemName,
                        iconUrl = item.displayIcon,
                        isSelected = selectedSymptoms.contains(item.problemId),
                        onClick = {
                            selectedSymptoms =
                                if (selectedSymptoms.contains(item.problemId))
                                    selectedSymptoms - item.problemId
                                else
                                    selectedSymptoms + item.problemId
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CommonButton(
                text = "Save and Update symptoms",
                isLoading = addLoading,
                enabled = selectedSymptoms.isNotEmpty(),
                onClick = {
                    viewModel.saveProblems(
                        context,
                        selectedSymptoms,
                        defaultList + searchList,
                        navController
                    )
                }
            )
        }
    }
}

@Composable
fun SymptomItem(
    title: String,
    iconUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) colors.primaryBlueColor else colors.dashboardContainerColor)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = iconUrl,
            contentDescription = title,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = AppTextStyles.style12BCB())
    }
}

@Composable
fun SelectedChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE4EEFF))
            .clickable { onRemove() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text)
        Spacer(modifier = Modifier.width(4.dp))
        Text("✕")
    }
}
