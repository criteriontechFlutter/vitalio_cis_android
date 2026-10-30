package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticCheckbox
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard

import com.critetiontech.ctvitalio.R
@Composable
fun FamilyHistoryScreen() {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    var showDiseaseDialog by remember {
        mutableStateOf(false)
    }

    var showRelationDialog by remember {
        mutableStateOf(false)
    }

    var selectedDisease by remember {
        mutableStateOf("")
    }

    var selectedRelations by remember {
        mutableStateOf(listOf<String>())
    }

    val diseaseList = listOf(
        "High Blood Pressure (Hypertension)",
        "Blood Cancer (Leukemia)",
        "Blood Clots (Deep Vein Thrombosis)",
        "Blood Disorders (Anemia)",
        "Thalassemia (Blood Disorder)",
        "Hemophilia (Blood Clotting Disorder)"
    )

    val relationList = listOf(
        "Mother",
        "Father",
        "Brother",
        "Sister",
        "Grand Parent"
    )

    // Values stay English (what gets stored/sent); only the shown label is translated
    val diseaseLabels = chronicDiseaseLabels()
    val relationLabels = mapOf(
        "Mother" to stringResource(R.string.auth_onb_relation_mother),
        "Father" to stringResource(R.string.auth_onb_relation_father),
        "Brother" to stringResource(R.string.auth_onb_relation_brother),
        "Sister" to stringResource(R.string.auth_onb_relation_sister),
        "Grand Parent" to stringResource(R.string.auth_onb_relation_grandparent)
    )

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
            title = stringResource(R.string.auth_onb_progress_final_push_title),
            subtitle = stringResource(R.string.auth_onb_progress_final_push_subtitle)
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.familygif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(6.dp))

        /// TITLE

        Text(
            text = stringResource(R.string.auth_onb_family_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_onb_family_subtitle),
            color = colors.textGreyColor,
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.auth_onb_chronic_label),
            color = colors.textGreyColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        /// SELECT DISEASE FIELD

        OutlinedTextField(
            value = diseaseLabels[selectedDisease] ?: selectedDisease,
            onValueChange = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDiseaseDialog = true
                },
            placeholder = {
                Text(stringResource(R.string.auth_onb_chronic_hint))
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
                disabledContainerColor = colors.dashboardContainerColor,
                disabledBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
                disabledTextColor = colors.textDarkColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        /// SELECTED CARD

        if (selectedDisease.isNotEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        softTint(Color(0xFF2563EB), Color(0xFFF4F7FF)),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        showRelationDialog = true
                    }
                    .padding(14.dp)
            ) {

                Text(
                    text = selectedRelations.joinToString(", ") { relationLabels[it] ?: it },
                    color = colors.textGreyColor,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = diseaseLabels[selectedDisease] ?: selectedDisease,
                    color = Color(0xFF2563EB),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        /// NEXT BUTTON

        HapticButton(
            onClick = {
                navController.navigate("createAccount")
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

        Spacer(modifier = Modifier.height(10.dp))
    }

    /// DISEASE DIALOG

    if (showDiseaseDialog) {

        Dialog(
            onDismissRequest = {
                showDiseaseDialog = false
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

                    var search by remember {
                        mutableStateOf("")
                    }

                    val filteredList = diseaseList.filter {
                        (diseaseLabels[it] ?: it).contains(search, ignoreCase = true)
                    }

                    /// SEARCH

                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
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
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
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

                                        selectedDisease = disease

                                        showDiseaseDialog = false

                                        showRelationDialog = true
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

    /// RELATION DIALOG

    if (showRelationDialog) {

        Dialog(
            onDismissRequest = {
                showRelationDialog = false
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
                        text = stringResource(R.string.auth_onb_select_relation),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.auth_onb_select_relation_subtitle),
                        color = colors.textGreyColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    relationList.forEach { relation ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = relationLabels[relation] ?: relation,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )

                            HapticCheckbox(
                                checked = selectedRelations.contains(relation),
                                onCheckedChange = { checked ->

                                    selectedRelations =
                                        if (checked) {
                                            selectedRelations + relation
                                        } else {
                                            selectedRelations - relation
                                        }
                                }
                            )
                        }

                        HorizontalDivider(
                            color = softTint(colors.textGreyColor, Color(0xFFF1F1F1))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HapticButton(
                        onClick = {
                            showRelationDialog = false
                        },
                        enabled = selectedRelations.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1564ED),
                            disabledContainerColor = Color(0xFF1564ED).copy(alpha = 0.35f),
                            disabledContentColor = Color.White
                        )
                    ) {

                        Text(stringResource(R.string.auth_common_submit), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}