package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.viewmodel.AddMedicineReminderViewModel
import com.example.vitalio_cis.viewmodel.DoctorDetailsViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
@Composable
fun AddMedicineReminderScreen(

    viewModel: AddMedicineReminderViewModel = viewModel()
     ) {
    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Add New Medicine Reminder",
    ) {

        var showSheet by remember { mutableStateOf(false) }
        if (showSheet) {
            AddAllergyBottomSheet(
                onDismiss = { showSheet = false },   // ✅ important
                onSubmit = { showSheet = false }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            Spacer(Modifier.height(16.dp))

            Text("Medicine Name", style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(6.dp))


            MyTextField(
                value = viewModel.searchText,
                onValueChange = { viewModel.onSearchChange(it) },
                trailingIcon = {
                    Icon(Icons.Default.Search, null)
                },
                placeholderText = "Search medicine"
            )


            Spacer(Modifier.height(16.dp))

            Text("Frequency", style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(6.dp))

            FrequencyDropdown()


            Spacer(Modifier.height(16.dp))

            Text("Time Slot", style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(6.dp))


            MyTextField(
                value = viewModel.searchText,
                onValueChange = { viewModel.onSearchChange(it) },
                placeholderText = "HH:MM"
            )


            Spacer(Modifier.height(16.dp))

            Text("Instructions", style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(6.dp))

            MyTextField(
                value = viewModel.searchText,
                onValueChange = { viewModel.onSearchChange(it) },
                placeholderText = "i.e. Take this before food"
            )


            Spacer(Modifier.height(16.dp))

            Row {
                Column(modifier = Modifier.weight(1f)) {

                    Text("Start Date", style = AppTextStyles.style14BCN())
                    Spacer(Modifier.height(6.dp))

                    MyTextField(
                        value = viewModel.searchText,
                        onValueChange = { viewModel.onSearchChange(it) },
                        placeholderText = " "
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text("End Date", style = AppTextStyles.style14BCN())
                        Spacer(Modifier.height(6.dp))
                        MyTextField(
                            value = viewModel.searchText,
                            onValueChange = { viewModel.onSearchChange(it) },
                            placeholderText = "DD/MM/YYYY"
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {showSheet = true},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Medicine")
                    }
                }
            }


        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAllergyBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val colors = LocalMyColorScheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // Title
            Text(
                text = "Add Allergy",
                style = AppTextStyles.style16BCN(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Substance Type
            Text("Substance Type", style = AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Select Substance") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Substance Name
            Text("Substance Name", style = AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search substance name...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reaction
            Text("Reaction/ Allergy", style = AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Enter Reaction/ Allergy") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "How severe was the reaction?",
                style = AppTextStyles.style12GCN()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeverityButton("Mild")
                SeverityButton("Moderate")
                SeverityButton("Severe")
            }

            Spacer(modifier = Modifier.height(20.dp))

            CommonButton(
                text = "Submit",
                modifier = Modifier.fillMaxWidth(),
                onClick = onSubmit
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SeverityButton(text: String) {
    Box(
        modifier = Modifier
            .background(
                Color(0xFFF3F4F6),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, style = AppTextStyles.style12GCN())
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyDropdown() {

    val items = listOf(
        "Every day",
        "Every x day",
        "Every week",
        "Every month",
        "As Needed"
    )

    val colors = LocalMyColorScheme.current
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("") }

    Column {

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            TextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("How often?") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.dashboardBackgroundColor,
                    unfocusedContainerColor = colors.dashboardBackgroundColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            selected = item
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}