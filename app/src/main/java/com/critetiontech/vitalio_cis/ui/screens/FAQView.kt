package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme


// ✅ Data Model
data class FAQItem(
    val question: String,
    val answer: String
)

@Composable
fun FAQScreen() {

    var search by remember { mutableStateOf("") }

    val faqList = listOf(
        FAQItem(stringResource(R.string.profile_faq_q_workout_frequency), stringResource(R.string.profile_faq_a_workout_frequency)),
        FAQItem(stringResource(R.string.profile_faq_q_best_diet), stringResource(R.string.profile_faq_a_best_diet)),
        FAQItem(stringResource(R.string.profile_faq_q_cardio_amount), stringResource(R.string.profile_faq_a_cardio_weekly)),
        FAQItem(stringResource(R.string.profile_faq_q_lose_weight), stringResource(R.string.profile_faq_a_lose_weight)),
        FAQItem(stringResource(R.string.profile_faq_q_abs_daily), stringResource(R.string.profile_faq_a_abs_daily)),
        FAQItem(stringResource(R.string.profile_faq_q_lifting_weight), stringResource(R.string.profile_faq_a_lifting_weight)),
        FAQItem(stringResource(R.string.profile_faq_q_cardio_amount), stringResource(R.string.profile_faq_a_cardio_goal))
    )

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title =  stringResource(R.string.profile_faq_title),)
     {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {

            MyTextField(
                value = search,
                onValueChange = {search = it },
                placeholderText = stringResource(R.string.profile_faq_search_hint)
            )
            // 🔹 Search Box


            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 FAQ List
            faqList.forEachIndexed { i, faq ->
                AnimatedEnter(index = i) {
                    FAQItemView(faq)
            
                }
            }
        }
    }
}

// ✅ Single FAQ Item
@Composable
fun FAQItemView(item: FAQItem) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.question,
                    modifier = Modifier.weight(1f),
                    style = AppTextStyles.style14BCN()
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF2563EB)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.answer,
                    style = AppTextStyles.style12GCN()
                )
            }
        }
    }
}