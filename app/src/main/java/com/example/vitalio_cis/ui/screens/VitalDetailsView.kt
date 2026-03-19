package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen() {

    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text("Vitals") },

                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },

                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        )
        {

            VitalCard(
                title = "Blood Pressure",
                value = "88/54",
                unit = "mm/Hg",
                color = Color.Red
            )

            VitalCard(
                title = "Heart Rate",
                value = "60",
                unit = "BPM",
                color = Color(0xFFFFB300)
            )

            VitalCard(
                title = "Blood Oxygen (spo2)",
                value = "97%",
                unit = "",
                color = Color(0xFF2962FF)
            )

            VitalCard(
                title = "Body Temperature",
                value = "102.9",
                unit = "°F",
                color = Color.Red
            )

            VitalCard(
                title = "Respiratory Rate",
                value = "17",
                unit = "min",
                color = Color(0xFFFFB300)
            )

            VitalCard(
                title = "RBS",
                value = "77",
                unit = "mg/dL",
                color = Color(0xFF2962FF)
            )
        }
    }
}


@Composable
fun VitalCard(
    title: String,
    value: String,
    unit: String,
    color: Color
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6FA))
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                    )


                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Last 5 hours",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {

                    Text(
                        value,
                        color = color,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        unit,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "1hr ago",
                    fontSize = 11.sp,
                    color = color
                )
            }

            Column   {
                Text(
                    "Add Vital",
                    color = Color(0xFF2962FF),
                    fontSize = 12.sp
                )
                MiniGraph(color)
            }
        }
    }
}


@Composable
fun MiniGraph(color: Color) {

    Canvas(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp)
    ) {

        val path = Path()

        path.moveTo(0f, size.height / 2)

        for (i in 1..10) {

            val x = size.width / 10 * i
            val y = (size.height / 2) + (-20..20).random()

            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )
    }
}


