package com.example.vitalio_cis.ui.screens


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme


// ✅ Data Model
data class Member(
    val name: String,
    val id: String
)

// ✅ Main Screen
@Composable
fun SharedAccountScreen() {

    val members = listOf(
        Member("Shamsa Juma", "AD87958"),
        Member("Ayush Dhyan", "AD87958"),
        Member("Surya Kala", "AD87958"),
        Member("Sumit Bose", "AD87958")
    )


    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Shared Account",
        actions = {
            Box(modifier = Modifier
                .background(color = colors.btnWhiteColor)
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .clickable(){
                    navController.navigate(Routes.ADDMEMBER)
                }) {
                Text(
                    text = "Add Member",
                    style = AppTextStyles.style12PCB()
                )
            }
        }
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            // 🔹 List
            members.forEach {
                MemberItem(member = it)
            }
        }
    }
}

// ✅ Item UI
@Composable
fun MemberItem(member: Member) {

    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔹 Profile Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.LightGray, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = AppTextStyles.style14BCN(),
                )
                Text(
                    text = member.id,
                    style = AppTextStyles.style12GCN(),
                )
            }

            // ✅ Export Icon (Clickable)
            IconButton(
                onClick = {
                    println("Export clicked: ${member.name}")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Export",
                    tint = colors.btnDarkColor
                )
            }
        }
    }
}