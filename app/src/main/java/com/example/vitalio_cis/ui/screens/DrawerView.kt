package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.AppTheme
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.LocalThemeViewModel
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.PrefsManager

@Composable
fun DrawerScreen() {


    val colors = LocalMyColorScheme.current

    val navController = LocalNavController.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var expanded by remember {
        mutableStateOf(false)
    }
    CommonAppBar(
        title = " ",
        actions = {

            Box {

                IconButton(
                    onClick = {
                        expanded = true
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("Logout")
                        },
                        onClick = {
                            PrefsManager(context).clearAll()
                            navController.navigate(Routes.LOGIN)
                            expanded = false
                        }
                    )
                }
            }
        }
    ) {

    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {


            Spacer(modifier = Modifier.height(12.dp))

            ProfileCard()

            Spacer(modifier = Modifier.height(12.dp))

            MedicalProfileCard()

            Spacer(modifier = Modifier.height(12.dp))

            MenuCard(
                listOf(
                    "Profile" to "",
                    "Allergies" to "2",
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuCard(
                listOf(
                    "Appointments" to "2",
                    "Prescriptions" to "",
                    "Reports" to ""
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuCard(
                listOf(
                    "My Observer" to "4",
                    "Shared Accounts" to "4",
                    "Family Health History" to "",
                    "Connect Smart Watch" to "",
                    "Emergency Contacts" to "4"
                )
            )
        Spacer(modifier = Modifier.height(12.dp))
        ThemeSwitchItem()

        Spacer(modifier = Modifier.height(12.dp))
        MenuCard(
            listOf(
                "FAQ" to "4",
                "Feedback" to "4"
            )
        )
        }
    }
}


@Composable
fun ThemeSwitchItem() {

    val themeViewModel = LocalThemeViewModel.current
    val colors = LocalMyColorScheme.current

    val selectedTheme by themeViewModel.selectedTheme.collectAsState()

    val isDarkMode = selectedTheme == AppTheme.DARK

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🌙 ICON + TEXT
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = colors.textGreyColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Dark Mode",
                    color = colors.textDarkColor,
                    fontSize = 16.sp
                )
            }

            // 🔥 SWITCH
            Switch(
                colors = SwitchDefaults.colors(

                    checkedThumbColor = Color.White,

                    checkedTrackColor = colors.primaryBlueColor,

                    uncheckedThumbColor = Color.White,

                    uncheckedTrackColor = Color.LightGray
                ),

                checked = isDarkMode,

                onCheckedChange = {

                    themeViewModel.toggleTheme()
                }
            )
        }
    }
}



@Composable
fun ProfileCard() {

    var showSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier.size(90.dp)
        ) {

            /// 🔹 Profile Image
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            /// 🔹 Edit Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
                    .clickable {
                        showSheet = false
                        showSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Abhay Sharma", style = AppTextStyles.style18BCB())

        Text(
            "+91 9876543210",
            style = AppTextStyles.style14GCN(),
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    /// 🔥 BottomSheet (OUTSIDE UI)
    if (showSheet) {
        ImagePickerBottomSheet(
            onCameraClick = {
                showSheet = false
            },
            onUploadClick = {
                showSheet = false
            },
            onRemoveClick = {
                showSheet = false
            },
        )
    }
}


@Composable
fun MedicalProfileCard(
    progress: Float = 0.6f,
    onClick: () -> Unit = {}
) {



    val navController = LocalNavController.current

    val colors = LocalMyColorScheme.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable() {
                navController.navigate(Routes.MEDICALPROFILE)

            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1557FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {


    Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(70.dp)
            ) {

                CircularProgressIndicator(
                    progress = progress,
                    color = Color.White,
                    trackColor = Color.Gray,
                    strokeWidth = 6.dp,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = AppTextStyles.style14BCB()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle Content
            Column(
                modifier = Modifier.weight(1f)
            ) {

                // Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Medical Profile",
                        style = AppTextStyles.style14BCB()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Keep your health profile up to date for better care",

                    style = AppTextStyles.style12BCB()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Button
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Complete profile",

                        style = AppTextStyles.style12BCB().copy(color=colors.primaryBlueColor)
                    )
                }
            }

            // Arrow Icon
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}


@Composable
fun MenuCard(items: List<Pair<String, String>>) {

    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column {

            items.forEachIndexed { index, item ->

                MenuRow(
                    title = item.first,
                    count = item.second
                )

            }
        }
    }
}


@Composable
fun MenuRow(
    title: String,
    count: String,
) {


    val navController = LocalNavController.current
    val route = when (title) {

        "Profile" -> Routes.PERSONALIFSCREEN
        "Allergies" -> Routes.ALLERGIESSCREEN
        "Appointments" -> Routes.APPOINTMENTS
        "Prescriptions" -> Routes.PRESCRIPTION
        "Reports" -> Routes.LABREPORTS
        "My Observer" -> Routes.MYOBSERVERS
        "Family Health History" -> Routes.FAMILYHEALTH
        "Shared Accounts" -> Routes.SHAREDACCOUNT
        "Connect Smart Watch" -> Routes.CONNECTWATCH
        "Emergency Contacts" -> Routes.EMERGENCYCONTACTS
        "FAQ" -> Routes.FAQ
        "Feedback" -> Routes.FEEDBACK

        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                route?.let { navController.navigate(it) }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = AppTextStyles.style14BCB()
        )

        if (count.isNotEmpty()) {
            Text(
                text = count,
                style = AppTextStyles.style14GCN(),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onCameraClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRemoveClick: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = {},
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            /// 🔹 OPTIONS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                BottomItem("Take Photo", Icons.Default.CameraAlt, onCameraClick)
                BottomItem("Upload Image", Icons.Default.Upload, onUploadClick)
                BottomItem("Remove", Icons.Default.Delete, onRemoveClick)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun BottomItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {

        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFF2F4F7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text, fontSize = 12.sp)
    }
}