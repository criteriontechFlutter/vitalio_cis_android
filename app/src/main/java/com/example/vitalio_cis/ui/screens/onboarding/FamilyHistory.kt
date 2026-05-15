package com.example.vitalio_cis.ui.screens.onboarding
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard


@Composable
fun FamilyHistoryScreen() {

    val navController = LocalNavController.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                "Create Account",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "Skip",
                color = Color(0xFF3B82F6),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = "Final Push Ahead",
            subtitle = "So close!  Only a few steps remain!"
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
            text = "Your Family’s Health\nHistory",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Are there any hereditary conditions in your\nfamily?",
            color = Color.Gray,
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Chronic Disease",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        /// SELECT DISEASE FIELD

        OutlinedTextField(
            value = selectedDisease,
            onValueChange = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDiseaseDialog = true
                },
            placeholder = {
                Text("Select Chronic Diseases")
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
                disabledContainerColor = Color.White,
                disabledBorderColor = Color(0xFFE5E7EB),
                disabledTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        /// SELECTED CARD

        if (selectedDisease.isNotEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF4F7FF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        showRelationDialog = true
                    }
                    .padding(14.dp)
            ) {

                Text(
                    text = selectedRelations.joinToString(", "),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = selectedDisease,
                    color = Color(0xFF2563EB),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        /// NEXT BUTTON

        Button(
            onClick = {
                navController.navigate("createAccount")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {
            Text("Next", color = Color.White)
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    var search by remember {
                        mutableStateOf("")
                    }

                    val filteredList = diseaseList.filter {
                        it.contains(search, ignoreCase = true)
                    }

                    /// SEARCH

                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search")
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
                                text = disease,
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
                                color = Color(0xFFF1F1F1)
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Select Relation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Who in your family has been diagnosed with this disease?",
                        color = Color.Gray,
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
                                text = relation,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp
                            )

                            Checkbox(
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
                            color = Color(0xFFF1F1F1)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showRelationDialog = false
                        },
                        enabled = selectedRelations.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Text("Submit")
                    }
                }
            }
        }
    }
}