package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.viewmodel.VitalDetailViewModel

data class Device(
    val name: String,
    val type: String
)

@Composable
fun ConnectionScreen(
    vitalName: String,
    unitName: String,
    viewModel: VitalDetailViewModel = viewModel()
) {

    val devices = listOf(
        Device("Omron Automatic BP Monitor (HEM-7120)", "BP Machine"),
        Device("Yonker BP Cuff YK-BPW1", "BP Machine"),
        Device("Omron HEM 6161", "BP Machine")
    )

    val colors = LocalMyColorScheme.current

    var showDialog by remember {
        mutableStateOf(false)
    }

    val navController = LocalNavController.current
    var vitalValue by remember {
        mutableStateOf("")
    }
    var sysValue by remember { mutableStateOf("") }
    var diaValue by remember { mutableStateOf("") }
    var singleValue by remember { mutableStateOf("") }



    val context = LocalContext.current



    CommonAppBar(title = "Connection") {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
            ) {

                /* ---------------- DEVICE LIST ---------------- */

                devices.forEach { device ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.dashboardContainerColor)
                            .padding(12.dp),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.btnGreyColor)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = device.name,
                                style = AppTextStyles.style14BCB()
                            )

                            Text(
                                text = device.type,
                                style = AppTextStyles.style12GCN()
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                /* ---------------- BUTTON ---------------- */

                Button(
                    onClick = {
                        showDialog = true
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(55.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF2F6BFF)
                    ),

                    border = BorderStroke(
                        1.dp,
                        Color(0xFF2F6BFF)
                    )
                ) {

                    Text(
                        text = "Add Vital Manually",
                        style = AppTextStyles.style16PCN()
                    )
                }
            }

            /* ---------------- DIALOG ---------------- */

            if (showDialog) {

                Dialog(onDismissRequest = { showDialog = false }) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {

                        Column {

                            /* -------- TITLE -------- */
                            Box {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text =  "",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "Enter $vitalName Value",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text("✕",modifier = Modifier.clickable(){
                                        showDialog = false
                                    })


                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            /* -------- BP CASE (SYS + DIA) -------- */
                            if (vitalName == "Blood Pressure") {

                                /* SYS */
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Column {
                                        Text("SYS", style = AppTextStyles.style14BCB().copy(fontSize = 30.sp))
                                        Text("mmHg",
                                                style = AppTextStyles.style14GCN())
                                    }

                                    BasicTextField(
                                        value = sysValue,
                                        onValueChange = { sysValue = it },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 50.sp,
                                            color = Color.Black
                                        ),
                                        modifier = Modifier.width(120.dp),
                                        decorationBox = { innerTextField ->

                                            Column {

                                                Box(
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                ) {

                                                    if (sysValue.isEmpty()) {

                                                        Text(
                                                            text = "00",
                                                            color = Color.Gray,
                                                            fontSize = 50.sp
                                                        )
                                                    }

                                                    innerTextField()
                                                }
                                            }}
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                /* DIA */
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Column {
                                        Text("DIA", style = AppTextStyles.style14BCB().copy(fontSize = 30.sp))
                                        Text("mmHg",
                                            style = AppTextStyles.style14GCN())
                                    }

                                    BasicTextField(
                                        value = diaValue,
                                        onValueChange = { diaValue = it },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 50.sp,
                                            color = Color.Black
                                        ),
                                        modifier = Modifier.width(120.dp),
                                        decorationBox = { innerTextField ->

                                            Column {

                                                Box(
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                ) {

                                                    if (diaValue.isEmpty()) {

                                                        Text(
                                                            text = "00",
                                                            color = Color.Gray,
                                                            fontSize = 50.sp
                                                        )
                                                    }

                                                    innerTextField()
                                                }
                                            }}
                                    )
                                }
                            }

                            /* -------- SINGLE FIELD (OTHER VITALS) -------- */
                            else {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Column {

                                        Text(
                                            text = vitalName,
                                            style = AppTextStyles.style14BCB().copy(fontSize = 30.sp))


                                        Text(
                                            text = unitName,
                                            style = AppTextStyles.style14GCN()
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    BasicTextField(
                                        value = singleValue,
                                        onValueChange = { singleValue = it },
                                        singleLine = true,

                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 50.sp,
                                            color = Color.Black
                                        ),

                                        modifier = Modifier.width(120.dp),

                                        decorationBox = { innerTextField ->

                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {

                                                Box(
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                ) {

                                                    if (singleValue.isEmpty()) {

                                                        Text(
                                                            text = "00",
                                                            color = Color.Gray,
                                                            fontSize = 50.sp
                                                        )
                                                    }

                                                    innerTextField()
                                                }

                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            /* -------- SAVE BUTTON -------- */
                            Button(
                                onClick = {

                                    if (vitalName == "BP") {

                                        println("SYS: $sysValue")
                                        println("DIA: $diaValue")

                                    } else {

                                        println("Value: $singleValue")
                                    }

                                    when (vitalName) {

                                        "Blood Pressure" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueBPSys = sysValue,

                                                vmValueBPDias = diaValue
                                            )
                                        }

                                        "SpO2" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueSPO2 = singleValue
                                            )
                                        }

                                        "Heart Rate" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueHeartRate = singleValue
                                            )
                                        }

                                        "Respiratory Rate" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueRespiratoryRate = singleValue
                                            )
                                        }

                                        "RBS" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueRbs = singleValue
                                            )
                                        }


                                        "RespRate" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueRespiratoryRate = singleValue
                                            )
                                        }
                                        "Temperature" -> {

                                            viewModel.addVital(

                                                context = context,

                                                vmValueTemperature = singleValue
                                            )
                                        }
                                    }
                                    showDialog = false

                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Vitals")
                            }
                        }
                    }
                }
            }
        }
    }
}