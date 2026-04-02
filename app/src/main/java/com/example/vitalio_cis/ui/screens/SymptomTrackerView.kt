package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.viewmodel.SymptomTrackerViewModel
import com.example.vitalio_cis.viewmodel.VitalDetailViewModel
data class SymptomQuestion(
    val id: Int,
    val prefix: String,
    val highlight: String = "",
    val suffix: String = "?"
)

@Composable
fun SymptomTrackerScreen(viewModel: SymptomTrackerViewModel = viewModel()) {

    val context = LocalContext.current

    // 🔹 API Call
    LaunchedEffect(Unit) {
        viewModel.getSymptoms(context)
    }

    // 🔹 StateFlow Bind
    val symptomList by viewModel.symptomTrackerList.collectAsState()
    val selectedList by viewModel.selectedSymptoms.collectAsState()

    // ✅ FIXED: SAFE MAPPING
    val questions = symptomList.map { item ->

        val id = item.detailId.toInt()
        val name = item.details ?: ""

        SymptomQuestion(
            id = id,
            prefix = "Do you still have ",
            highlight = name,
            suffix = "?"
        )
    }

    // ✅ FIXED: SAFE EMPTY CHECK
    if (questions.isEmpty() || symptomList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...")
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }

    // ✅ FIXED: SAFE INDEX
    val safeIndex = currentIndex.coerceIn(0, questions.lastIndex)

    val currentQuestion = questions[safeIndex]
    val currentSymptom = symptomList[safeIndex]

    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8))
            .padding(16.dp)
    ) {

        // 🔹 Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Symptom Tracker", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔹 Image
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.symptom_tacker),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 23.dp),
                contentScale = ContentScale.FillWidth
            )
        }

        // 🔹 Question Text
        Text(
            text = buildAnnotatedString {
                append(currentQuestion.prefix)
                withStyle(
                    SpanStyle(
                        color = Color(0xFF2F6BFF),
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(currentQuestion.highlight)
                }
                append(currentQuestion.suffix)
            },
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // ✅ YES BUTTON FIXED
            Button(
                onClick = {
                    viewModel.addSymptom(currentSymptom)

                    if (currentIndex < questions.lastIndex) {
                        currentIndex++
                    } else {
                        viewModel.insertSymptoms(context, navController)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2E7F0),
                    contentColor = Color.Black
                )
            ) {
                Text("Yes")
            }

            // ❌ NO BUTTON FIXED
            Button(
                onClick = {
                    viewModel.removeSymptom(currentSymptom)

                    if (currentIndex < questions.lastIndex) {
                        currentIndex++
                    } else {
                        viewModel.insertSymptoms(context, navController)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE4EEFF),
                    contentColor = Color.White
                )
            ) {
                Text("No")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(questions.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .width(if (index == safeIndex) 25.dp else 6.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == safeIndex) Color.Blue
                            else Color.LightGray
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Back
        if (safeIndex > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentIndex-- },
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back to previous question")
            }
        }
    }
}
