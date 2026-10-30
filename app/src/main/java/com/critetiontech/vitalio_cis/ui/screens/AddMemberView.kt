package com.critetiontech.ctvitalio.ui.screens


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
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.R
@Composable
fun AddMemberScreen() {

    val colors = LocalMyColorScheme.current

    var code by remember { mutableStateOf("") }

    CommonAppBar(
        title = stringResource(R.string.profile_add_member),
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
                stringResource(R.string.profile_add_member_title),
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
                        stringResource(R.string.profile_add_member_body),
                        fontSize = 13.sp,
                        color = colors.textGreyColor,
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
                            Text(stringResource(R.string.profile_connection_code_hint))
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))



            CommonButton(
                text = stringResource(R.string.profile_submit),
                onClick =  {

                }
            )

        }
    }
}