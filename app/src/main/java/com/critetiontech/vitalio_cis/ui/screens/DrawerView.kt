package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material.icons.automirrored.rounded.Logout

import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.HapticSwitch
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import androidx.compose.material.icons.filled.Vibration
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.adaptive.AdaptiveTwoPane
import com.critetiontech.ctvitalio.ui.adaptive.isTablet
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.AppTheme
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.LocalThemeViewModel
import com.critetiontech.ctvitalio.utils.PrefsManager

@Composable
fun DrawerScreen() {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        com.critetiontech.ctvitalio.ui.components.AppDialog(
            title = "Log out?",
            message = "You'll need to sign in again to see your vitals, appointments and reports. Medicine reminders on this phone will stop.",
            icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.Logout,
            destructive = true,
            confirmText = "Log out",
            dismissText = "Cancel",
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                PrefsManager(context).clearAll()
                // Patient is gone, so this cancels every medicine alarm.
                com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler.requestSync(context)
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }

    CommonAppBar(
        title = " ",
        actions = {
            Box {
                HapticIconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            expanded = false
                            showLogoutDialog = true
                        }
                    )
                }
            }
        }
    ) {

    if (isTablet()) {
        AdaptiveTwoPane(
            firstPane = {
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
                }
            },
            secondPane = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.dashboardBackgroundColor)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuCard(
                        listOf(
                            "Profile" to "",
                            "Allergies" to "2",
                            "Breathing Therapy" to "",
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
                    ThemeSwitchItem()
                    Spacer(modifier = Modifier.height(12.dp))
                    HapticsSwitchItem()
            ServerItem()
                    ServerItem()
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuCard(
                        listOf(
                            "Permissions & Privacy" to "",
                            "FAQ" to "4",
                            "Feedback" to "4"
                        )
                    )
                }
            }
        )
    } else {
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

//            MedicalProfileCard()

            Spacer(modifier = Modifier.height(12.dp))

            MenuCard(
                listOf(
                    "Profile" to "",
                    "Allergies" to "2",
                    "Breathing Therapy" to "",
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
//
//            MenuCard(
//                listOf(
//                    "My Observer" to "4",
//                    "Shared Accounts" to "4",
//                    "Family Health History" to "",
//                    "Connect Smart Watch" to "",
//                    "Emergency Contacts" to "4"
//                )
//            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeSwitchItem()
            Spacer(modifier = Modifier.height(12.dp))
            HapticsSwitchItem()
            ServerItem()

            Spacer(modifier = Modifier.height(12.dp))
            MenuCard(
                listOf(
                    "Permissions & Privacy" to "",
                    "FAQ" to "4",
                    "Feedback" to "4"
                )
            )
        }
    }
    }
}


@Composable
fun ThemeSwitchItem() {
    val themeViewModel = LocalThemeViewModel.current
    val selectedTheme by themeViewModel.selectedTheme.collectAsState()

    SettingSwitchRow(
        icon = Icons.Default.DarkMode,
        title = "Dark Mode",
        checked = selectedTheme == AppTheme.DARK,
        onCheckedChange = { themeViewModel.toggleTheme() },
    )
}

@Composable
fun HapticsSwitchItem() {
    val context = LocalContext.current
    val haptic = rememberHaptics()
    var enabled by remember { mutableStateOf(Haptics.isEnabled(context)) }

    SettingSwitchRow(
        icon = Icons.Default.Vibration,
        title = "Haptic feedback",
        subtitle = "Gentle vibration on taps and actions",
        checked = enabled,
        onCheckedChange = { on ->
            Haptics.setEnabled(context, on)
            enabled = on
            // The switch's own tap haptic ran while haptics were still off; confirm turning on.
            if (on) haptic(HapticEvent.Toggle)
        },
    )
}

/** Testers only: shows which backend this build talks to and opens the switcher. */
@Composable
fun ServerItem() {
    if (!com.critetiontech.ctvitalio.config.ApiConfig.canSwitch) return
    val colors = LocalMyColorScheme.current
    val env = com.critetiontech.ctvitalio.config.ApiConfig.environment
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) com.critetiontech.ctvitalio.ui.components.EnvironmentSheet(onDismiss = { showSheet = false })

    Spacer(modifier = Modifier.height(12.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "Server: ${env.label}", color = colors.textDarkColor, fontSize = 16.sp)
                Text(text = env.host.substringAfter("://"), color = colors.textGreyColor, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

/** A settings card with an icon, title, optional subtitle and a switch, as used in the drawer. */
@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    val colors = LocalMyColorScheme.current

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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textGreyColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, color = colors.textDarkColor, fontSize = 16.sp)
                    if (subtitle != null) {
                        Text(text = subtitle, color = colors.textGreyColor, fontSize = 12.sp)
                    }
                }
            }

            HapticSwitch(
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.primaryBlueColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                ),
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}



@Composable
fun ProfileCard(
    photoViewModel: com.critetiontech.ctvitalio.viewmodel.EditProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    var patient by remember { mutableStateOf(PrefsManager(context).getPatient()) }
    val fullName = remember(patient) {
        listOfNotNull(patient?.firstName, patient?.lastName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifEmpty { "—" }
    }
    val mobileDisplay = remember(patient) {
        patient?.let { p ->
            val prefix = p.countryCallingCode.takeIf { it.isNotBlank() }?.let { "+$it " } ?: ""
            "$prefix${p.mobileNo}".trim().ifEmpty { "—" }
        } ?: "—"
    }
    val uhid = remember(patient) { patient?.uhId?.ifEmpty { null } }

    var showSheet by rememberSaveable { mutableStateOf(false) }

    // ── Photo change: same upload as Edit Profile, keeping every other field as saved ──
    val uploading by photoViewModel.loading.collectAsState()
    val uploaded by photoViewModel.updateSuccess.collectAsState()
    val uploadError by photoViewModel.errorMessage.collectAsState()
    var previewUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun upload(uri: android.net.Uri) {
        val p = PrefsManager(context).getPatient() ?: run {
            android.widget.Toast.makeText(context, "Session expired. Please log in again.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val file = runCatching { FileUtils.getFile(context, uri) }.getOrNull()
        if (file == null || !file.exists()) {
            android.widget.Toast.makeText(context, "Couldn't read that image. Please try another one.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        previewUri = uri
        photoViewModel.updateProfile(
            firstName = p.firstName,
            lastName = p.lastName.orEmpty(),
            dob = p.dob.substringBefore('T'),
            mobileNo = p.mobileNo,
            emailId = p.emailAddress.orEmpty(),
            address = p.address.orEmpty(),
            zip = p.zip.orEmpty(),
            imageFile = file,
            bloodGroupId = p.bloodGroupId,
            countryId = p.countryId,
            stateId = p.stateId,
            cityId = p.cityId,
        )
    }

    LaunchedEffect(uploaded) {
        if (uploaded) {
            patient = PrefsManager(context).getPatient()
            android.widget.Toast.makeText(context, "Profile photo updated", android.widget.Toast.LENGTH_SHORT).show()
            photoViewModel.resetSuccess()
        }
    }
    LaunchedEffect(uploadError) {
        uploadError?.let {
            previewUri = null
            android.widget.Toast.makeText(context, "Photo not updated: $it", android.widget.Toast.LENGTH_LONG).show()
            photoViewModel.resetError()
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::upload) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        TakePictureWithPermission()
    ) { ok -> if (ok) cameraUri?.let(::upload) }

    fun launchCamera() {
        val uri = createImageUri(context)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermission = com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest(
        com.critetiontech.ctvitalio.permissions.AppPermission.Camera, "to take your profile photo"
    ) { launchCamera() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier.size(90.dp)
        ) {

            /// 🔹 Profile Image, inside the same turning ring as the dashboard
            com.critetiontech.ctvitalio.ui.components.ProfileRing(size = 90.dp, ringWidth = 3.dp, gap = 3.dp) {
                AsyncImage(
                    model = previewUri ?: patient?.imageUrl
                        ?.takeIf { it.isNotBlank() },

                    placeholder = painterResource(R.drawable.img),
                    error = painterResource(R.drawable.img),

                    contentDescription = "Profile",

                    contentScale = ContentScale.Crop,

                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                if (uploading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
            }

            /// 🔹 Edit Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primaryBlueColor)
                    .border(2.dp, colors.dashboardBackgroundColor, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable(enabled = !uploading) {
                        showSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(fullName, style = AppTextStyles.style18BCB())

        if (uhid != null) {
            Text(
                "UHID: $uhid",
                style = AppTextStyles.style14GCN(),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            mobileDisplay,
            style = AppTextStyles.style14GCN(),
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    /// 🔥 BottomSheet (OUTSIDE UI)
    if (showSheet) {
        // The API can replace the photo but not delete it, so there's no Remove here.
        ImagePickerBottomSheet(
            onDismiss = { showSheet = false },
            onCameraClick = {
                showSheet = false
                cameraPermission.launch()
            },
            onUploadClick = {
                showSheet = false
                galleryLauncher.launch("image/*")
            },
            onRemoveClick = null,
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
            .clickable {
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
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                strokeWidth = 6.dp,
                trackColor = Color.Gray,
                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
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

            items.forEachIndexed { _, item ->

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
        "Breathing Therapy" -> Routes.BREATHING_THERAPY
        "Allergies" -> Routes.ALLERGIESSCREEN
        "Appointments" -> Routes.APPOINTMENTS
        "Prescriptions" -> Routes.PRESCRIPTION
        "Reports" -> Routes.LABREPORTS
        "My Observer" -> Routes.MYOBSERVERS
        "Family Health History" -> Routes.FAMILYHEALTH
        "Shared Accounts" -> Routes.SHAREDACCOUNT
        "Connect Smart Watch" -> Routes.CONNECTWATCH
        "Emergency Contacts" -> Routes.EMERGENCYCONTACTS
        "Permissions & Privacy" -> Routes.PERMISSIONS
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
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
    onRemoveClick: (() -> Unit)?,
    onDismiss: () -> Unit = {},
) {
    com.critetiontech.ctvitalio.ui.components.AppBottomSheet(
        onDismissRequest = onDismiss,
        title = "Profile photo",
        subtitle = "Take a new photo or choose one from your gallery",
        icon = Icons.Default.CameraAlt,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BottomItem("Take Photo", Icons.Default.CameraAlt, onCameraClick, Modifier.weight(1f))
            BottomItem("Upload Image", Icons.Default.Upload, onUploadClick, Modifier.weight(1f))
            if (onRemoveClick != null) {
                BottomItem("Remove", Icons.Default.Delete, onRemoveClick, Modifier.weight(1f), tint = com.critetiontech.ctvitalio.ui.components.DialogDangerColor)
            }
        }
    }
}

@Composable
fun BottomItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = LocalMyColorScheme.current
    val accent = tint ?: colors.primaryBlueColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardBackgroundColor)
            .bounceClick { onClick() }
            .padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (tint != null) accent else colors.textDarkColor)
    }
}