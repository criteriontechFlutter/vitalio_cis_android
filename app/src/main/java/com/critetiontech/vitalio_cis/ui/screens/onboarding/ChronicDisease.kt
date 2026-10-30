package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard
import com.critetiontech.ctvitalio.R
@Composable
fun ChronicDiseaseScreen() {

    var showDialog by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedDiseaseList by remember {
        mutableStateOf(listOf<String>())
    }

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val diseaseList = listOf(
        "High Blood Pressure (Hypertension)",
        "Blood Cancer (Leukemia)",
        "Blood Clots (Deep Vein Thrombosis)",
        "Blood Disorders (Anemia)",
        "Thalassemia (Blood Disorder)",
        "Hemophilia (Blood Clotting Disorder)"
    )

    // Values stay English (what gets stored/sent); only the shown label is translated
    val diseaseLabels = chronicDiseaseLabels()
    val filteredList = diseaseList.filter {
        (diseaseLabels[it] ?: it).contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardContainerColor)
            .padding(20.dp)
    ) {

        // 🔙 Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                stringResource(R.string.auth_onb_create_account),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                stringResource(R.string.auth_common_skip),
                color = Color(0xFF3B82F6),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = stringResource(R.string.auth_onb_progress_final_stretch_title),
            subtitle = stringResource(R.string.auth_onb_progress_final_stretch_subtitle)
        )

        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.cronicgif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )

        Text(
            text = stringResource(R.string.auth_onb_chronic_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_onb_chronic_subtitle),
            color = colors.textGreyColor,
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(R.string.auth_onb_chronic_label),
            color = colors.textGreyColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        /// CLICKABLE TEXTFIELD

        OutlinedTextField(
            value = "",
            onValueChange = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDialog = true
                },
            placeholder = {
                Text(
                    text = stringResource(R.string.auth_onb_chronic_hint),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
                disabledContainerColor = colors.dashboardContainerColor,
                disabledTextColor = colors.textDarkColor
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        /// SELECTED CHIP LIST

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            selectedDiseaseList.forEach { disease ->

                SelectedChip(
                    text = diseaseLabels[disease] ?: disease,
                    onRemove = {
                        selectedDiseaseList =
                            selectedDiseaseList - disease
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HapticButton(
            onClick = {
                navController.navigate("familyHistory")
            },
             modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = softTint(colors.textGreyColor, Color(0xFFE5E7EB))
            )
        ) {
            Text(stringResource(R.string.auth_common_next), color = Color.White)
        }
    }

    /// DIALOG

    if (showDialog) {

        Dialog(
            onDismissRequest = {
                showDialog = false
            }
        ) {

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor
                )
            ) {

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
                ) {

                    Text(
                        text = stringResource(R.string.auth_onb_select_disease),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDarkColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    /// SEARCH

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(stringResource(R.string.auth_common_search))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    /// LIST

                    LazyColumn(
                        modifier = Modifier.height(250.dp)
                    ) {

                        items(filteredList) { disease ->

                            Text(
                                text = diseaseLabels[disease] ?: disease,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        if (!selectedDiseaseList.contains(disease)) {

                                            selectedDiseaseList =
                                                selectedDiseaseList + disease
                                        }

                                        showDialog = false
                                    }
                                    .padding(12.dp),
                                fontSize = 14.sp
                            )

                            HorizontalDivider(
                                color = softTint(colors.textGreyColor, Color(0xFFF1F1F1))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Display labels for the chronic-disease values; the English value is what gets stored/sent. */
@Composable
internal fun chronicDiseaseLabels(): Map<String, String> = mapOf(
    "High Blood Pressure (Hypertension)" to stringResource(R.string.auth_onb_disease_hypertension),
    "Blood Cancer (Leukemia)" to stringResource(R.string.auth_onb_disease_leukemia),
    "Blood Clots (Deep Vein Thrombosis)" to stringResource(R.string.auth_onb_disease_dvt),
    "Blood Disorders (Anemia)" to stringResource(R.string.auth_onb_disease_anemia),
    "Thalassemia (Blood Disorder)" to stringResource(R.string.auth_onb_disease_thalassemia),
    "Hemophilia (Blood Clotting Disorder)" to stringResource(R.string.auth_onb_disease_hemophilia),
)

@Composable
fun SelectedChip(
    text: String,
    onRemove: () -> Unit
) {
    val colors = LocalMyColorScheme.current

    Row(
        modifier = Modifier
            .background(
                softTint(Color(0xFF2563EB), Color(0xFFF3F6FF)),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = text,
            color = Color(0xFF2563EB),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .size(16.dp)
                .clickable {
                    onRemove()
                }
        )
    }
}