package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }

    val addLoading by viewModel.addLoading.collectAsState()

    var sysValue by remember { mutableStateOf("") }
    var diaValue by remember { mutableStateOf("") }
    var pulseValue by remember { mutableStateOf("") }
    var singleValue by remember { mutableStateOf("") }

    CommonAppBar(title = "Connection") {

        Box(modifier = Modifier.fillMaxSize()) {

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
                            .padding(horizontal = 16.dp, vertical = 6.dp)
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

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = device.name, style = AppTextStyles.style14BCB())
                            Text(text = device.type, style = AppTextStyles.style12GCN())
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                /* ---------------- BUTTON ---------------- */

                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF2F6BFF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2F6BFF))
                ) {
                    Text(text = "Add Vital Manually", style = AppTextStyles.style16PCN())
                }
            }

            /* ---------------- DIALOG ---------------- */

            if (showDialog) {

                Dialog(onDismissRequest = { if (!addLoading) showDialog = false }) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {

                        Column {

                            /* -------- TITLE ROW -------- */
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(24.dp))

                                Text(
                                    text = "Enter $vitalName Value",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "✕",
                                    modifier = Modifier.clickable(enabled = !addLoading) {
                                        showDialog = false
                                    },
                                    style = AppTextStyles.style16BCB()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            /* -------- BP CASE (SYS + DIA + Pulse) -------- */
                            if (vitalName == "Blood Pressure") {

                                VitalInputRow(label = "SYS", unit = "mmHg", value = sysValue, onValueChange = { sysValue = it })
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                VitalInputRow(label = "DIA", unit = "mmHg", value = diaValue, onValueChange = { diaValue = it })
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                VitalInputRow(label = "Pulse", unit = "BPM", value = pulseValue, onValueChange = { pulseValue = it })

                            } else {

                                /* -------- SINGLE FIELD (OTHER VITALS) -------- */
                                VitalInputRow(label = vitalName, unit = unitName, value = singleValue, onValueChange = { singleValue = it })
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            /* -------- SAVE BUTTON -------- */
                            Button(
                                onClick = {
                                    when (vitalName) {
                                        "Blood Pressure" -> viewModel.addVital(
                                            context = context,
                                            vmValueBPSys = sysValue,
                                            vmValueBPDias = diaValue,
                                            vmValuePulse = pulseValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        "SpO2" -> viewModel.addVital(
                                            context = context,
                                            vmValueSPO2 = singleValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        "Heart Rate" -> viewModel.addVital(
                                            context = context,
                                            vmValueHeartRate = singleValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        "Respiratory Rate", "RespRate" -> viewModel.addVital(
                                            context = context,
                                            vmValueRespiratoryRate = singleValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        "RBS" -> viewModel.addVital(
                                            context = context,
                                            vmValueRbs = singleValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        "Temperature" -> viewModel.addVital(
                                            context = context,
                                            vmValueTemperature = singleValue,
                                            onSuccess = { showDialog = false }
                                        )
                                        else -> viewModel.addVital(
                                            context = context,
                                            onSuccess = { showDialog = false }
                                        )
                                    }
                                },
                                enabled = !addLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (addLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Save Vitals", style = AppTextStyles.style16PCN().copy(color = Color.White))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalInputRow(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = AppTextStyles.style14BCB().copy(fontSize = 28.sp))
            Text(text = unit, style = AppTextStyles.style14GCN())
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 48.sp, color = Color.Black),
            modifier = Modifier.width(130.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (value.isEmpty()) {
                        Text(text = "00", color = Color.LightGray, fontSize = 48.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}
