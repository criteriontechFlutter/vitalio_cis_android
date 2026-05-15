package com.example.vitalio_cis.ui.screens.onboarding
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard

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
    val diseaseList = listOf(
        "High Blood Pressure (Hypertension)",
        "Blood Cancer (Leukemia)",
        "Blood Clots (Deep Vein Thrombosis)",
        "Blood Disorders (Anemia)",
        "Thalassemia (Blood Disorder)",
        "Hemophilia (Blood Clotting Disorder)"
    )

    val filteredList = diseaseList.filter {
        it.contains(searchText, ignoreCase = true)
    }

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
            title = "Final Stretch in Sight",
            subtitle = "You're making great progress-just a little more to go!"
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
            text = "Inform us if you have\nany chronic conditions.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sharing any chronic conditions helps us provide\nmore tailored health advice and care.",
            color = Color.Gray,
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Chronic Disease",
            color = Color.Gray,
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
                    text = "Select Chronic Diseases",
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
                disabledBorderColor = Color(0xFFE5E7EB),
                disabledContainerColor = Color.White,
                disabledTextColor = Color.Black
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
                    text = disease,
                    onRemove = {
                        selectedDiseaseList =
                            selectedDiseaseList - disease
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                navController.navigate("familyHistory")
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
    }

    /// DIALOG

    if (showDialog) {

        Dialog(
            onDismissRequest = {
                showDialog = false
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

                    /// SEARCH

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
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
                                text = disease,
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
                                color = Color(0xFFF1F1F1)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedChip(
    text: String,
    onRemove: () -> Unit
) {

    Row(
        modifier = Modifier
            .background(
                Color(0xFFF3F6FF),
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