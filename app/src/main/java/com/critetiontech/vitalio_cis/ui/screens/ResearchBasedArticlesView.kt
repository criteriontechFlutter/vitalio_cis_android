package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R

import androidx.compose.material.icons.rounded.Search


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonTextField

@Composable
fun ResearchArticlesScreen() {

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = stringResource(R.string.report_research_articles_title),
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            // 🔹 Greeting
            Text(stringResource(R.string.report_articles_greeting, "Abhay"),
                style = AppTextStyles.style14GCN())

            Text(
                stringResource(R.string.report_explore_trending_articles),
                style = AppTextStyles.style14BCN().copy(fontSize = 22.sp))

            Spacer(Modifier.height(16.dp))
            val search = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
            com.critetiontech.ctvitalio.ui.components.MyTextField(
                value = search.value,
                onValueChange = { search.value = it },
                placeholderText = stringResource(R.string.report_search_articles),
                leadingIcon = {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Search, contentDescription = null)
                },
            )
            // 🔍 Search


            Spacer(Modifier.height(20.dp))

            // 🔹 Specialties
            Text(stringResource(R.string.report_specialties), style = AppTextStyles.style16BCN())

            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(stringResource(R.string.report_specialty_cardiology), Color(0xFF5C6BC0))
                Chip(stringResource(R.string.report_specialty_hematology_oncology), Color(0xFF26A69A))
                Chip(stringResource(R.string.report_specialty_clinical_medicine), Color(0xFFFF7043))
                Chip(stringResource(R.string.report_specialty_endocrinology), Color(0xFFAB47BC))
                Chip(stringResource(R.string.report_specialty_gastroenterology), Color(0xFFFFCA28))
                Chip(stringResource(R.string.report_view_all_specialties), Color(0xFF8D6E63))
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 Featured
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.report_featured), style = AppTextStyles.style16BCN())
                Text(stringResource(R.string.report_view_all), style = AppTextStyles.style16PCN() )
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