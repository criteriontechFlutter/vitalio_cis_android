package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.utils.CommonButton

data class Observer(
    val name: String,
    val id: String
)

@Composable
fun MyObserversScreen() {

    val list = listOf(
        Observer("Abhay Sharma","AD87958"),
        Observer("Ayush Dhyan","AD87958"),
        Observer("Surya Kala","AD87958"),
        Observer("Sumit Bose","AD87958"),
    )

    val colors = LocalMyColorScheme.current

        CommonAppBar(
            title = "My Observers",
        )
        {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {

                    list.forEach {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.dashboardContainerColor)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(it.name, style = AppTextStyles.style14BCB())
                                Text(it.id, style = AppTextStyles.style12GCN())
                            }

                            Text(
                                "Remove Access",
                                style = AppTextStyles.style12GCN().copy(color = Color.Red),
                                modifier = Modifier.clickable { }
                            )
                        }
                    }
                }

                // bottom button
                CommonButton(
                    text = "Add Observer +",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { }
                )
            }
        }

}