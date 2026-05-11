package com.example.vitalio_cis.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
 import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme


@Composable
fun SelectClinicScreen() {

    var search by remember { mutableStateOf("") }

    val clinicList = listOf(
        Clinic(
            "Community Care Center",
            "2nd Floor, Lakshmi Towers, Brigade Road, Bangalore"
        ),
        Clinic(
            "LifeSpring Medical",
            "Main Bazaar Road, Aluva, Kochi - 683101"
        ),
        Clinic(
            "Fatima Hospital",
            "Main Gram Panchayat Road, Kothapet Village"
        )
    )


    val colors = LocalMyColorScheme.current
    val filtered = clinicList.filter {
        it.name.contains(search, true) ||
                it.address.contains(search, true)
    }


    CommonAppBar(
        title = "Select Clinic",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            Spacer(modifier = Modifier.height(12.dp))
            MyTextField(
                value = search,
                onValueChange = { search = it},
                placeholderText = "Search clinic by name or location"
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {

                items(filtered) { clinic ->

                    ClinicItem(
                        name = clinic.name,
                        address = clinic.address
                    )
                }
            }
        }
    }
}


@Composable
fun ClinicItem(
    name: String,
    address: String
) {

    val colors = LocalMyColorScheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.dashboardContainerColor
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.btnGreyColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF2E5BFF)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = name,
                    style = AppTextStyles.style14BCN()
                )

                Text(
                    text = address,
                    style = AppTextStyles.style12GCN()
                )
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
data class Clinic(
    val name: String,
    val address: String
)