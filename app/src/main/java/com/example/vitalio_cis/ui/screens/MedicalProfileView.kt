package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme

@Composable
fun MedicalProfileScreen() {

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Medical Profile",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            /// 🔹 CARD 1
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor // 👈 background color
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileRow("Blood Group", "AB+")
                    ProfileRow("Weight", "56 kg")
                    ProfileRow("Height", "170 cm")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            /// 🔹 CARD 2
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor // 👈 background color
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    ProfileRow("Medical Conditions", "Hypothyroidism")

                    ProfileRowWithTag("Allergies", "Rifampin", "Moderate")
                    ProfileRowWithTag("", "Lemon", "Moderate")

                    ProfileRow(
                        "Chronic Disease",
                        "High Blood Pressure (Hypertension), Thalassemia"
                    )

                    ProfileRowWithTag(
                        "Family's Health History",
                        "Blood Cancer (Leukemia)",
                        "Mother"
                    )

                    ProfileRowWithTag(
                        "",
                        "High Blood Pressure (Hypertension)",
                        "Mother"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            /// 🔹 BUTTON
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit Medical Profile")
            }
        }
    }
}

/// 🔹 SIMPLE ROW
@Composable
fun ProfileRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            title,
            modifier = Modifier.weight(0.3f),
            style = AppTextStyles.style14GCN()
        )

        Text(
            value,
            modifier = Modifier.weight(0.7f),
            style = AppTextStyles.style14BCN()
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

/// 🔹 ROW WITH TAG (Moderate / Mother)
@Composable
fun ProfileRowWithTag(title: String, value: String, tag: String) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (title.isNotEmpty()) {
            Text(
                title,
                modifier = Modifier.weight(0.3f),
                style = AppTextStyles.style14GCN()
            )
        } else {
            Spacer(modifier = Modifier.weight(0.3f)) // ✅ FIX
        }

        Row(modifier = Modifier.weight(0.7f)) {

            Text(
                value,
                style = AppTextStyles.style14BCN(),
                modifier = Modifier.weight(1f) // 👈 text wrap properly
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFFFF9800), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    tag,
                    style = AppTextStyles.style12WCN()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}