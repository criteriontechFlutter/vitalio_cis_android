package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.utils.CommonTextField

@Composable
fun ResearchArticlesScreen() {

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Research Based Articles",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            // 🔹 Greeting
            Text("Hi, Abhay!",
                style = AppTextStyles.style14GCN())

            Text(
                "Explore Today’s\nTrending Articles",
                style = AppTextStyles.style14BCN().copy(fontSize = 22.sp))

            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value =  " ",
                onValueChange = {     },
                hint = "Search..."
            )
            // 🔍 Search


            Spacer(Modifier.height(20.dp))

            // 🔹 Specialties
            Text("Specialties", style = AppTextStyles.style16BCN())

            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Cardiology", Color(0xFF5C6BC0))
                Chip("Hematology/Oncology", Color(0xFF26A69A))
                Chip("Clinical Medicine", Color(0xFFFF7043))
                Chip("Endocrinology", Color(0xFFAB47BC))
                Chip("Gastroenterology", Color(0xFFFFCA28))
                Chip("View All Specialties", Color(0xFF8D6E63))
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 Featured
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Featured", style = AppTextStyles.style16BCN())
                Text("View All", style = AppTextStyles.style16PCN() )
            }

            Spacer(Modifier.height(12.dp))

            ResearchArticleCard(
                title = "Carbohydrate antigen 125 (CA125) following acute myocardial infarction: effects of empagliflozin...",
                author = "Ahmed M. Hassan, and Others",
                date = "23 December 2025"
            )

            Spacer(Modifier.height(10.dp))

            ResearchArticleCard(
                title = "Trial of High-Dose Oral Rifampin in Adults with Tuberculous Meningitis.",
                author = "D.B. Meya, and Others",
                date = "17 December 2025"
            )

            Spacer(Modifier.height(10.dp))

            ResearchArticleCard(
                title = "Explainable multimodal AI for skin lesion risk prediction via 3D imaging.",
                author = "Various Authors",
                date = "10 December 2025"
            )
        }
    }
}

/* ================= CHIP ================= */

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable { }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

/* ================= ARTICLE CARD ================= */

@Composable
fun ResearchArticleCard(
    title: String,
    author: String,
    date: String
) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {

        Text(
            text = title, style = AppTextStyles.style14BCN())

        Spacer(Modifier.height(6.dp))

        Text(
            text = author, style = AppTextStyles.style12GCN())


        Text(
            text = date, style = AppTextStyles.style12GCN())
    }
}