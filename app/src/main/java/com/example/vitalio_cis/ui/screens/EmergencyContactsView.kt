package com.example.vitalio_cis.ui.screens


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme


@Composable
fun EmergencyContactsScreen() {

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Emergency Contacts",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            ContactItem("Father", "Vinay Sharma", "+917654892140")
            ContactItem("Brother", "Ayush Sharma", "+917654892140")
            ContactItem("Sister", "Riya Sharma", "+917654892140")
            ContactItem("Friend", "Sumit Bose", "+917654892140")
        }
    }
}

@Composable
fun ContactItem(
    relation: String,
    name: String,
    phone: String
) {
    var expanded by remember { mutableStateOf(false) }

    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {
                Text(relation, style = AppTextStyles.style12GCN())
                Text(name,  style = AppTextStyles.style14BCB())
                Text(phone,  style = AppTextStyles.style14PCN())
            }

            IconButton(onClick = { /* Call */ }) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2962FF))
            }

            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.clickable { expanded = true }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = { expanded = false }
                    )
                }
            }
        }
    }
}