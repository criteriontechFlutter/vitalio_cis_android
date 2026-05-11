package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.utils.CommonButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@Composable
fun AddMemberScreen() {

    val colors = LocalMyColorScheme.current

    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    CommonAppBar(
        title = "Add Member",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.addmember),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f), // 👈 ratio adjust karo (image ke hisaab se)
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            /// 🔹 TITLE
            Text(
                "Stay Connected to Their Health Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E5BB8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            /// 🔹 CARD
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        "Receive Access to Their Profile as an Observer. Enter the Connection Code Below to Connect.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    /// 🔹 INPUT
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.uppercase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Enter connection code")
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))



            CommonButton(
                text = "Submit",
                onClick =  {

                }
            )

        }
    }
}