package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.Drug
import com.critetiontech.ctvitalio.viewmodel.InteractionCheckerViewModel
import com.critetiontech.ctvitalio.viewmodel.InteractionInstructionItem
import com.critetiontech.ctvitalio.viewmodel.InteractionSeverity
import com.critetiontech.myapplication.utils.LocalNavController

@Composable
fun InteractionCheckerScreen(
    viewModel: InteractionCheckerViewModel = viewModel()
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current

    val searchText by viewModel.searchText.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedDrugs by viewModel.selectedDrugs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCheckingInteraction by viewModel.isCheckingInteraction.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    CommonAppBar(
        title = "Interaction Checker",
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Medicine Name",
                        style = AppTextStyles.style14GCN()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MyTextField(
                        value = searchText,
                        onValueChange = { viewModel.updateSearchText(it) },
                        placeholderText = "Search Drugs, Supplements & OTC",
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (searchText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchText("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedDrugs.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.clickable { viewModel.clearSelectedDrugs() },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Clear", color = colors.primaryBlueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = colors.primaryBlueColor, modifier = Modifier.size(14.dp))
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.03f))
                                    .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                selectedDrugs.forEachIndexed { index, drug ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(drug.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.dashboardContainerColor)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray.copy(alpha = 0.7f))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(drug.activeIngredients, fontSize = 13.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { viewModel.removeDrug(drug) }) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    if (index < selectedDrugs.size - 1) {
                                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                    }
                                }
                            }

                            Text(
                                "Warning: If no interactions are found between two drugs, it does not necessarily mean that no interactions exist. Always consult with a healthcare professional.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp, vertical = 24.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                                .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            itemsIndexed(searchResults) { index, med ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addDrug(med) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    val name = if (med.brandName.isEmpty()) med.medicineName else med.brandName
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.dashboardContainerColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${med.medicineName} • ${med.dosageFormName} ${med.doseStrength.toInt()} ${med.unitName}", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (index < searchResults.size - 1) {
                                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.fetchDrugInteractions { success ->
                            if (success) {
                                navController.navigate(com.critetiontech.ctvitalio.Routes.INTERACTIONINSTRUCTION)
                            }
                        }
                    },
                    enabled = selectedDrugs.isNotEmpty() && !isCheckingInteraction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryBlueColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp)
                ) {
                    if (isCheckingInteraction) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Check Interaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
