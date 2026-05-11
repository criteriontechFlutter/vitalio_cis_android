package com.example.vitalio_cis.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LocationScreen() {

    var street by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // 🔹 Street Address
        Label("Street Address")
        CustomTextField(
            value = street,
            placeholder = "Street Address",
            onValueChange = { street = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Zip + City
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Column(modifier = Modifier.weight(1f)) {
                Label("Zip Code")
                CustomTextField(
                    value = zip,
                    placeholder = "Enter Zip Code",
                    onValueChange = { zip = it }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Label("City")
                CustomDropdown(
                    value = city,
                    placeholder = "Select City",
                    onSelect = { city = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 State + Country
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Column(modifier = Modifier.weight(1f)) {
                Label("State")
                CustomDropdown(
                    value = state,
                    placeholder = "Select State",
                    onSelect = { state = it }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Label("Country")
                CustomDropdown(
                    value = country,
                    placeholder = "Select Country",
                    onSelect = { country = it }
                )
            }
        }
    }
}

@Composable
fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedBorderColor = Color(0xFF1E5CCB)
        )
    )
}

@Composable
fun CustomDropdown(
    value: String,
    placeholder: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf("Option 1", "Option 2", "Option 3")

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF1E5CCB)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}