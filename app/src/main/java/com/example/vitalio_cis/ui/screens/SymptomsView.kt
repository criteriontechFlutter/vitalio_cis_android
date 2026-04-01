package com.example.vitalio_cis.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vitalio_cis.viewmodel.SymptomTrackerViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun SymptomsView(viewModel: SymptomTrackerViewModel = viewModel()) {

    val context = LocalContext.current

    // 🔹 Initial API Call
    LaunchedEffect(Unit) {
        viewModel.getProblemsWithIcon(context)
    }

    // 🔥 STATES
    var searchText by remember { mutableStateOf("") }
    var selectedSymptoms by remember { mutableStateOf(listOf<Int>()) }

    val defaultList = viewModel.symptomIconsList.value
    val searchList = viewModel.searchedsymptomList.value
    val isLoading = viewModel.loading.value

    // 🔥 SEARCH API CALL
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty()) {
            delay(400)
            viewModel.getAllProblems(context, searchText)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(16.dp)
    ) {

        // 🔹 HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Symptom Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔍 SEARCH FIELD
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search Symptoms i.e cold, cough") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.clickable {
                            searchText = ""
                        }
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
        )

        // 🔄 LOADING
        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }

        // 🔥 SEARCH DROPDOWN LIST
        if (searchText.isNotEmpty() && searchList.isNotEmpty()) {

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
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
                                .clickable {

                                    selectedSymptoms =
                                        if (selectedSymptoms.contains(item.problemId))
                                            selectedSymptoms - item.problemId
                                        else
                                            selectedSymptoms + item.problemId

                                    searchText = "" // close dropdown
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

        // ✅ SELECTED CHIPS
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

        // 🔥 GRID SHOW ONLY WHEN NO SEARCH

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
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
    }
}

// 🔥 GRID ITEM
@Composable
fun SymptomItem(
    title: String,
    iconUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFE4EEFF) else Color.White)
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

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}

// 🔥 CHIP
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