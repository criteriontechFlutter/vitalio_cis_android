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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vitalio_cis.viewmodel.OTPViewModel
import com.example.vitalio_cis.viewmodel.VitalDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen( viewModel: VitalDetailViewModel = viewModel()) {

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.getPatientDetailsByMobileNo(context)
    }

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
        ) {
            val vitals by viewModel.vitalList.collectAsState()
            if (vitals.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else {

                val bpSys = vitals.find { it.vitalName.equals("BP_Sys", true) }
                val bpDia = vitals.find { it.vitalName.equals("BP_Dias", true) }

                // ✅ BP Combined Card
                if (bpSys != null && bpDia != null) {

                    VitalCard(
                        title = "Blood Pressure",
                        value = "${bpSys.vitalValue.toInt()}/${bpDia.vitalValue.toInt()}",
                        unit = "mmHg",
                        color = Color.Red,
                        time = formatTimeAgo(bpSys.vitalDateTime)
                    )
                }

                // ✅ Other vitals (BP skip)
                vitals
                    .filter {
                        it.vitalName != "BP_Sys" && it.vitalName != "BP_Dias"
                    }
                    .forEach { vital ->

                        VitalCard(
                            title = vital.vitalName,
                            value = formatValue(vital.vitalValue),
                            unit = vital.unit.replace("/", ""), // clean unit
                            color = getVitalColor(vital.vitalName),
                            time = formatTimeAgo(vital.vitalDateTime)
                        )
                    }
            }
        }
    }
}

fun formatValue(value: Double): String {
    return if (value % 1 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}
fun getVitalColor(name: String): Color {
    return when (name.lowercase()) {
        "pulse" -> Color(0xFFFFB300)
        "temperature" -> Color.Red
        "bp_sys" -> Color.Red
        "weight" -> Color(0xFF2962FF)
        "height" -> Color(0xFF2962FF)
        else -> Color.Gray
    }
}

@Composable
fun VitalCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    time: String
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
                    time.toString(),
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
//                MiniGraph(color)
            }
        }
    }
}


fun formatTimeAgo(dateTime: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateTime)

        val diff = Date().time - (date?.time ?: 0)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        }

    } catch (e: Exception) {
        ""
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


