package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme

@Composable
fun InteractionCheckerScreen() {

    var search by remember { mutableStateOf("") }

    val colors = LocalMyColorScheme.current

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
                    .padding(16.dp)
            ) {


                Spacer(modifier = Modifier.height(20.dp))

                // Label
                Text(
                    text = "Medicine Name",
                    style = AppTextStyles.style14GCN()
                )

                Spacer(modifier = Modifier.height(8.dp))


                MyTextField(
                    value =search,
                    onValueChange = { search = it },
                    placeholderText = "Search Drugs, Supplements & OTC" ,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
                )
                // Search Field

            }

            // Bottom Button
            Button(
                onClick = { },
                enabled = false,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dashboardContainerColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text("Check Interaction")
            }
        }
    }
}