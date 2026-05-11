package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme

data class Device(
    val name: String,
    val type: String
)

@Composable
fun ConnectionScreen() {

    val devices = listOf(
        Device("Omron Automatic BP Monitor (HEM-7120)", "BP Machine"),
        Device("Yonker BP Cuff YK-BPW1", "BP Machine"),
        Device("Omron HEM 6161", "BP Machine")
    )

    val colors = LocalMyColorScheme.current
    CommonAppBar(title = "Connection") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {

            /* ---------------- DEVICE LIST ---------------- */

            devices.forEach { device ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.dashboardContainerColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.btnGreyColor)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            device.name,
                            style = AppTextStyles.style14BCB()
                        )

                        Text(
                            device.type,
                            style = AppTextStyles.style12GCN()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            /* ---------------- BOTTOM BUTTON ---------------- */

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(55.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF2F6BFF)
                ),
                border = BorderStroke(1.dp, Color(0xFF2F6BFF))
            ) {
                Text("Add Vital Manually",
                    style = AppTextStyles.style16PCN())
            }
        }
    }
}