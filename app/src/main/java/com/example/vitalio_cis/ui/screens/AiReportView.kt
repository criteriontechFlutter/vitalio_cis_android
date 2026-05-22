package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.viewmodel.UploadReportViewModel
import com.google.gson.Gson
import java.time.format.TextStyle

// -----------------------------------------------------
// MAIN SCREEN
// -----------------------------------------------------

@Composable
fun AiReportScreen(

    viewModel: UploadReportViewModel

) {

    // -----------------------------------------------------
    // RESPONSE STRING FROM VIEWMODEL
    // -----------------------------------------------------

    val responseString by
    viewModel.responseString
        .collectAsStateWithLifecycle()

    // -----------------------------------------------------
    // JSON PARSE
    // -----------------------------------------------------

    val reportData = remember(responseString) {

        if (responseString.isNotEmpty()) {

            try {

                Gson().fromJson(

                    responseString,

                    ApiResponse::class.java
                )

            } catch (e: Exception) {

                e.printStackTrace()

                ApiResponse()
            }

        } else {

            ApiResponse()
        }
    }

    // -----------------------------------------------------
    // MAIN RESPONSE
    // -----------------------------------------------------

    val responseData =
        reportData.response
            .firstOrNull()

    // -----------------------------------------------------
    // PATIENT DETAILS
    // -----------------------------------------------------

    val patientDetails =
        responseData?.patient_details

    // -----------------------------------------------------
    // REPORT LIST
    // -----------------------------------------------------

    val report =
        responseData?.report
            ?: emptyList()

    // -----------------------------------------------------
    // SUMMARY
    // -----------------------------------------------------

    val summary =
        responseData?.summary ?: ""

    // -----------------------------------------------------
    // STATUS
    // -----------------------------------------------------

    val status =
        responseData?.status ?: ""

    // -----------------------------------------------------
    // SPECIALTY
    // -----------------------------------------------------

    val recommendedSpecialty =
        responseData?.recommended_specialty
            ?: ""

    // -----------------------------------------------------
    // UI
    // -----------------------------------------------------

    CommonAppBar(
        title = "Upload Report",
        actions = {

            Button(

                onClick = { },

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),

                border = BorderStroke(
                    1.dp,
                    Color(0xFFE5E7EB)
                ),

                shape = RoundedCornerShape(2.dp),

                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),

                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {

                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Upload",

                    color = Color(0xFF2563EB),

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF5F6F8)
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
        ) {

            // -----------------------------------------------------
            // PATIENT NAME
            // -----------------------------------------------------

            LabelText("Patient Name")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(
                patientDetails
                    ?.patient_name ?: ""
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // GENDER + AGE
            // -----------------------------------------------------

            Row {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    LabelText("Gender")

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    EditableInfoField(
                        patientDetails
                            ?.sex ?: ""
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    LabelText("Age")

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    EditableInfoField(
                        patientDetails
                            ?.age ?: ""
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // LAB NAME
            // -----------------------------------------------------

            LabelText("Lab Name")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(
                patientDetails
                    ?.lab_name ?: ""
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // COLLECTION DATE
            // -----------------------------------------------------

            LabelText("Collection Date")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(
                patientDetails
                    ?.collection_date ?: ""
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // REPORTED DATE
            // -----------------------------------------------------

            LabelText("Reported Date")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(
                patientDetails
                    ?.reported_date ?: ""
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // SUMMARY
            // -----------------------------------------------------

            LabelText("Summary")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(summary)

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // STATUS
            // -----------------------------------------------------

            LabelText("Status")

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(status)

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // -----------------------------------------------------
            // SPECIALTY
            // -----------------------------------------------------

            LabelText(
                "Recommended Specialty"
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            EditableInfoField(
                recommendedSpecialty
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            // -----------------------------------------------------
            // REPORT GRID
            // -----------------------------------------------------

            LazyVerticalGrid(

                columns =
                    GridCells.Fixed(2),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    ),

                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max = 5000.dp
                    )
            ) {

                items(report) { item ->

                    ReportCard(item)
                }
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }
    }
}

// -----------------------------------------------------
// REPORT CARD
// -----------------------------------------------------

@Composable
fun ReportCard(
    item: ReportItem
) {

    var value by remember(
        item.result
    ) {

        mutableStateOf(
            item.result
        )
    }

    Column {

        Text(

            text = item.test_name,

            color =
                Color(0xFF7A8799),

            fontSize = 13.sp,

            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        OutlinedTextField(

            value = value,

            onValueChange = {

                value = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine = true,

            shape =
                RoundedCornerShape(
                    10.dp
                ),

            colors =
                OutlinedTextFieldDefaults
                    .colors(

                        focusedBorderColor =
                            Color.Transparent,

                        unfocusedBorderColor =
                            Color.Transparent,

                        focusedContainerColor =
                            Color(
                                0xFFF1F2F4
                            ),

                        unfocusedContainerColor =
                            Color(
                                0xFFF1F2F4
                            ),

                        cursorColor =
                            Color.Black
                    ),

            textStyle =
                LocalTextStyle.current.copy(

                    fontSize = 17.sp,

                    fontWeight =
                        FontWeight.SemiBold
                ),

            suffix = {

                Text(

                    text =
                        item.unit ?: "",
                    style = AppTextStyles.style14BCB().copy(fontSize = 8.sp)
                )
            }
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(

            text =
                item.normal_values
                    ?: "",

            color = Color.Gray,

            fontSize = 11.sp
        )
    }
}

// -----------------------------------------------------
// EDITABLE FIELD
// -----------------------------------------------------

@Composable
fun EditableInfoField(
    value: String
) {

    var text by remember(value) {

        mutableStateOf(value)
    }

    OutlinedTextField(

        value = text,

        onValueChange = {

            text = it
        },

        modifier =
            Modifier.fillMaxWidth(),

        singleLine = true,

        shape =
            RoundedCornerShape(
                10.dp
            ),

        colors =
            OutlinedTextFieldDefaults
                .colors(

                    focusedBorderColor =
                        Color.Transparent,

                    unfocusedBorderColor =
                        Color.Transparent,

                    focusedContainerColor =
                        Color(
                            0xFFF1F2F4
                        ),

                    unfocusedContainerColor =
                        Color(
                            0xFFF1F2F4
                        ),

                    cursorColor =
                        Color.Black
                ),

        textStyle =
            LocalTextStyle.current.copy(

                fontSize = 16.sp,

                fontWeight =
                    FontWeight.Medium,

                color = Color.Black
            )
    )
}

// -----------------------------------------------------
// LABEL
// -----------------------------------------------------

@Composable
fun LabelText(
    text: String
) {

    Text(

        text = text,

        color =
            Color(0xFF7E8AA0),

        fontSize = 12.sp,

        fontWeight =
            FontWeight.Medium
    )
}

// -----------------------------------------------------
// API MODELS
// -----------------------------------------------------

data class ApiResponse(

    val response:
    List<ResponseData> =
        emptyList()
)

data class ResponseData(

    val patient_details:
    PatientDetails =
        PatientDetails(),

    val report:
    List<ReportItem> =
        emptyList(),

    val summary: String = "",

    val status: String = "",

    val recommended_specialty:
    String = ""
)

data class PatientDetails(

    val patient_name: String = "",

    val sex: String = "",

    val age: String = "",

    val lab_name: String = "",

    val collection_date:
    String = "",

    val reported_date:
    String = ""
)

data class ReportItem(

    val test_name: String = "",

    val result: String = "",

    val unit: String? = "",

    val normal_values:
    String? = "",

    val id: String? = "",
)