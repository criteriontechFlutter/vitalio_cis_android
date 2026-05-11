package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.utils.CommonButton

/* ================= MAIN SCREEN ================= */

@Composable
fun FamilyHealthScreen() {

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Family's Health History",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {

            // 🔹 Cards
            HealthCard(
                title = "High Blood Pressure (Hypertension)",
                tags = listOf(
                    "Mother" to Color(0xFFE91E63),
                    "Brother" to Color(0xFFFFC107),
                    "Father" to Color(0xFF2196F3)
                )
            )

            Spacer(Modifier.height(12.dp))

            HealthCard(
                title = "Hypertension",
                tags = listOf(
                    "Father" to Color(0xFF2196F3)
                )
            )

            Spacer(Modifier.height(12.dp))

            HealthCard(
                title = "Asthma",
                tags = listOf(
                    "Brother" to Color(0xFFFFC107)
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            CommonButton(
                text = "+ Add Family's Health History",
                onClick =  {

                },
                containerColor = colors.btnWhiteColor,
                textStyle = AppTextStyles.style16BCB()
            )


            // 🔹 Bottom Button

        }
    }
}

/* ================= CARD ================= */

@Composable
fun HealthCard(
    title: String,
    tags: List<Pair<String, Color>>
) {
    val colors = LocalMyColorScheme.current

    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title,
                style = AppTextStyles.style14BCN())

            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.clickable() {
                        expanded = true
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            expanded = false
                            // handle edit
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            expanded = false
                            // handle remove
                        }
                    )
                }
            }





        }

        Spacer(Modifier.height(8.dp))

        Row {
            tags.forEach {
                TagChip(text = it.first, color = it.second)
                Spacer(Modifier.width(6.dp))
            }
        }
    }

    }

/* ================= CHIP ================= */

@Composable
fun TagChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = AppTextStyles.style12WCN())
    }
}


@Composable
fun ActionMenu(
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFFF5F5F5))
                .padding(vertical = 8.dp)
        ) {
            MenuItem(
                text = "Edit",
                icon = Icons.Default.Edit,
                onClick = onEdit
            )
            Divider()
            MenuItem(
                text = "Remove",
                icon = Icons.Default.Delete,
                onClick = onRemove
            )
        }
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = Color.DarkGray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.DarkGray)
    }
}