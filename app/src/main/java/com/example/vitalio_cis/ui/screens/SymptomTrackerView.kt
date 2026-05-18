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
import androidx.compose.material3.CircularProgressIndicator
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
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
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
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current

    LaunchedEffect(Unit) {
        viewModel.getSymptoms(context)
    }

    val symptomList by viewModel.symptomTrackerList.collectAsState()
    val hasFetched by viewModel.hasFetched.collectAsState()
    val addLoading by viewModel.addLoading.collectAsState()

    // Show spinner while API is in flight
    if (!hasFetched) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2F6BFF))
        }
        return
    }

    // Fetch complete but no symptoms found — go to the add-symptoms screen
    if (symptomList.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.SYMPTOMS) {
                popUpTo(Routes.SYMPTOMSTRACKER) { inclusive = true }
            }
        }
        return
    }

    val questions = symptomList.map { item ->
        SymptomQuestion(
            id = item.detailId,
            prefix = "Do you still have ",
            highlight = item.details,
            suffix = "?"
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, questions.lastIndex)
    val currentQuestion = questions[safeIndex]
    val currentSymptom = symptomList[safeIndex]

    CommonAppBar(
        title = "Symptom Tracker",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {

            // 🔹 Top Bar


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

                style = AppTextStyles.style18BCB()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                CommonButton(
                    text = "Yes",
                    textStyle = AppTextStyles.style14BCB(),
                    modifier = Modifier.weight(1f),
                    containerColor = colors.btnGreyColor,
                    isLoading = addLoading && currentIndex == questions.lastIndex,
                    enabled = !addLoading,
                    onClick = {
                        viewModel.addSymptom(currentSymptom)
                        if (currentIndex < questions.lastIndex) {
                            currentIndex++
                        } else {
                            viewModel.insertSymptoms(context, navController)
                        }
                    }
                )

                CommonButton(
                    text = "No",
                    modifier = Modifier.weight(1f),
                    isLoading = addLoading && currentIndex == questions.lastIndex,
                    enabled = !addLoading,
                    onClick = {
                        viewModel.removeSymptom(currentSymptom)
                        if (currentIndex < questions.lastIndex) {
                            currentIndex++
                        } else {
                            viewModel.insertSymptoms(context, navController)
                        }
                    }
                )
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
}
