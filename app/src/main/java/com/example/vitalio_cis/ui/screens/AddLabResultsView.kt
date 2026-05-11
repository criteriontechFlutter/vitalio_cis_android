package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddLabResultsScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .padding(16.dp)
    ) {

        Text("Test Type")
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Select") },
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
        )

        Spacer(Modifier.height(16.dp))

        Text("Date")
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = "01.05.1998",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.DateRange, null)
            }
        )

        Spacer(Modifier.height(16.dp))

        Text("Test Name")
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Select") }
        )

        Spacer(Modifier.height(16.dp))

        Text("Findings")
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Enter") }
        )

        Spacer(Modifier.height(20.dp))

        Row {
            UploadCard(
                title = "Camera",
                subtitle = "Capture from camera",
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(12.dp))

            UploadCard(
                title = "Gallery",
                subtitle = "upload files from gallery",
                icon = Icons.Default.Check,
                selected = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        FileItem()

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Upload & Save")
        }
    }
}

@Composable
fun UploadCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFE6F4F1) else Color(0xFFF1F3F6))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(icon, contentDescription = null, tint = Color.Black)

        Spacer(Modifier.height(8.dp))

        Text(title, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun FileItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEDEFF3))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            "LFT Report .pdf",
            modifier = Modifier.weight(1f)
        )

        Icon(Icons.Default.Close, contentDescription = null)
    }
}
