package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme

@Composable
fun AddActivityScreen() {

    var search by remember { mutableStateOf("") }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }

    val allActivities = listOf(
        "Biking","Aerobics","Archery","Badminton",
        "Baseball","Basketball","Biathlon","Biking",
        "Handbiking","Mountain Biking","Road Biking",
        "Spinning","Stationary Biking","Utility Biking",
        "Boxing","Walking","Running","Jogging",
        "Cycling","Swimming","Hiking","Jump Rope",
        "Stair Climbing","Cricket","Boxing"
    )

    val filteredList = allActivities.filter {
        it.contains(search, ignoreCase = true)
    }

    val colors = LocalMyColorScheme.current
    val recentList = selectedItems.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .padding(16.dp)
    ) {

        /* ---------------- TOP BAR ---------------- */



        /* ---------------- SEARCH ---------------- */

        MyTextField(
            value = search,
            onValueChange = { search = it },
            placeholderText = "Search..."
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {

            /* ---------------- RECENT ---------------- */

            if (recentList.isNotEmpty()) {

                item {
                    Text("Recent",
                        style = AppTextStyles.style16BCB())
                    Spacer(Modifier.height(10.dp))
                }

                item {
                    FlowRow {
                        recentList.forEach { item ->
                            Chip(
                                text = item,
                                selected = true
                            ) {
                                selectedItems =
                                    selectedItems - item
                            }

                            Spacer(Modifier.width(8.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            /* ---------------- ALL ---------------- */

            item {
                Text("All Activities",
                    style = AppTextStyles.style16BCB())
                Spacer(Modifier.height(10.dp))
            }

            item {
                FlowRow {
                    filteredList.forEach { item ->

                        val selected = selectedItems.contains(item)

                        Chip(
                            text = item,
                            selected = selected
                        ) {

                            selectedItems =
                                if (selected)
                                    selectedItems - item
                                else
                                    selectedItems + item
                        }

                        Spacer(Modifier.width(8.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/* ---------------- CHIP ---------------- */

@Composable
fun Chip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current

    Box(
        modifier = Modifier
            .padding(  vertical = 4.dp)
            .background(
                if (selected) colors.primaryBlueColor
                else colors.dashboardContainerColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {

        Text(
            text = text,
            style = AppTextStyles.style14BCN().copy(color = if (selected)colors.textWhiteColor
            else colors.textDarkColor),
        )
    }
}



