package com.example.vitalio_cis.ui.screens


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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


// ✅ Data Model
data class FAQItem(
    val question: String,
    val answer: String
)

@Composable
fun FAQScreen() {

    var search by remember { mutableStateOf("") }

    val faqList = listOf(
        FAQItem("How often should I work out?", "You should work out at least 3-5 times per week."),
        FAQItem("What’s the best diet for my fitness goals?", "A balanced diet with protein, carbs and fats."),
        FAQItem("How much cardio should I be doing?", "150 minutes of moderate cardio weekly."),
        FAQItem("How To Lose Weight And Tone Up Fast?", "Combine strength + calorie deficit."),
        FAQItem("Should I work out my abs every day?", "No, 2-3 times per week is enough."),
        FAQItem("How much weight should I be lifting when I strength train?", "Start light and increase gradually."),
        FAQItem("How much cardio should I be doing?", "Depends on your goal.")
    )

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title =  "Frequently asked questions",)
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
                placeholderText = "Search Symptoms i.e cold, cough"
            )
            // 🔹 Search Box


            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 FAQ List
            faqList.forEach {
                FAQItemView(it)
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