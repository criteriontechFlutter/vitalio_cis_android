package com.critetiontech.vitalio_cis.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.critetiontech.vitalio_cis.viewmodel.EditProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────
// Blood group data
// ─────────────────────────────────────────────
data class BloodGroupOption(val id: Int, val name: String)

val bloodGroupOptions = listOf(
    BloodGroupOption(1, "A+"),
    BloodGroupOption(2, "A-"),
    BloodGroupOption(3, "B+"),
    BloodGroupOption(4, "B-"),
    BloodGroupOption(5, "O+"),
    BloodGroupOption(6, "O-"),
    BloodGroupOption(7, "AB+"),
    BloodGroupOption(8, "AB-")
)

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────
private fun genderIdFromName(name: String) = when (name) {
    "Male" -> 1
    "Female" -> 2
    else -> 3
}

private fun genderNameFromId(id: Int) = when (id) {
    1 -> "Male"
    2 -> "Female"
    else -> "Other"
}

private fun formatDisplayDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "dd-MM-yyyy"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            sdf.isLenient = false
            val d = sdf.parse(dateStr) ?: continue
            return SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
        } catch (_: Exception) { }
    }
    return dateStr
}

private fun millisToApiDate(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}

private fun apiDateToMillis(dateStr: String): Long? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (fmt in formats) {
        try {
            return SimpleDateFormat(fmt, Locale.US).parse(dateStr)?.time
        } catch (_: Exception) { }
    }
    return null
}

// ─────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val prefs = remember { PrefsManager(context) }
    val patient = remember { prefs.getPatient() }

    val loading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    // ── form fields ──
    var firstName by rememberSaveable { mutableStateOf(patient?.firstName.orEmpty()) }
    var lastName by rememberSaveable { mutableStateOf(patient?.lastName.orEmpty()) }
    var dobMillis by rememberSaveable {
        mutableStateOf(apiDateToMillis(patient?.dob.orEmpty()) ?: System.currentTimeMillis())
    }
    var gender by rememberSaveable {
        mutableStateOf(genderNameFromId(patient?.genderId ?: 1))
    }
    var mobileNo by rememberSaveable { mutableStateOf(patient?.mobileNo.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(patient?.emailAddress.orEmpty()) }
    var weight by rememberSaveable { mutableStateOf("") }
    var selectedBloodGroup by rememberSaveable {
        mutableStateOf(bloodGroupOptions.firstOrNull { it.id == patient?.bloodGroupId })
    }
    var address by rememberSaveable { mutableStateOf(patient?.address.orEmpty()) }
    var zipCode by rememberSaveable { mutableStateOf(patient?.zip.orEmpty()) }

    // ── location state ──
    val locationState by viewModel.locationState.collectAsState()

    // ── image state ──
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<java.io.File?>(null) }
    var showImageSheet by rememberSaveable { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // ── date picker state ──
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dobMillis)

    // ── validation ──
    val mobileError = when {
        mobileNo.isEmpty() -> "Mobile number is required."
        mobileNo.length < 10 -> "Mobile number must be at least 10 digits."
        else -> null
    }
    val emailRegex = Regex("^[A-Z0-9a-z._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\$")
    val emailValid = email.isEmpty() || emailRegex.matches(email)
    val isFormValid = firstName.isNotBlank() && mobileNo.length >= 10 && emailValid

    // ── launchers ──
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedImageFile = FileUtils.getFile(context, it)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let {
                selectedImageUri = it
                selectedImageFile = FileUtils.getFile(context, it)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLocationData(
            countryId = patient?.countryId ?: 101,
            stateId = patient?.stateId ?: 0,
            cityId = patient?.cityId ?: 0
        )
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    // ── Date Picker Dialog ──
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dobMillis = it }
                    showDatePicker = false
                }) { Text("OK", color = colors.primaryBlueColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textGreyColor)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Image Bottom Sheet ──
    if (showImageSheet) {
        ImagePickerBottomSheet(
            onCameraClick = {
                showImageSheet = false
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                cameraUri = uri
                uri?.let { cameraLauncher.launch(it) }
            },
            onUploadClick = {
                showImageSheet = false
                galleryLauncher.launch("image/*")
            },
            onRemoveClick = {
                showImageSheet = false
                selectedImageUri = null
                selectedImageFile = null
            }
        )
    }

    CommonAppBar(title = "Edit Profile") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .imePadding()
        ) {
            // ── scrollable content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ════ AVATAR HEADER ════
                AvatarHeaderCard(
                    imageUrl = patient?.imageUrl,
                    selectedImageUri = selectedImageUri,
                    onEditClick = { showImageSheet = true }
                )

                // ════ SECTION 1: Personal Information ════
                SectionCard(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    colors = colors
                ) {
                    // First Name (read-only like iOS)
                    EditProfileTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "First Name",
                        placeholder = "Enter first name",
                        readOnly = true,
                        colors = colors
                    )

                    // Last Name
                    EditProfileTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Last Name",
                        placeholder = "Enter last name",
                        colors = colors
                    )

                    // Date of Birth
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Date of Birth",
                            style = AppTextStyles.style14GCN(),
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(colors.dashboardContainerColor, RoundedCornerShape(10.dp))
                                .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDisplayDate(millisToApiDate(dobMillis)),
                                style = AppTextStyles.style14BCN(),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = colors.primaryBlueColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Gender Pill Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Gender",
                            style = AppTextStyles.style14GCN(),
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                GenderPill(
                                    title = g,
                                    isSelected = gender == g,
                                    onSelect = { gender = g },
                                    colors = colors,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ════ SECTION 2: Contact Details ════
                SectionCard(
                    title = "Contact Details",
                    icon = Icons.Default.Phone,
                    colors = colors
                ) {
                    EditProfileTextField(
                        value = mobileNo,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() }
                            mobileNo = if (filtered.length > 10) filtered.take(10) else filtered
                        },
                        label = "Mobile Number",
                        placeholder = "10-digit mobile number",
                        keyboardType = KeyboardType.Phone,
                        colors = colors
                    )
                    if (mobileError != null) {
                        Text(
                            text = mobileError,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    EditProfileTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "example@vitalio.care",
                        keyboardType = KeyboardType.Email,
                        colors = colors
                    )
                    if (!emailValid && email.isNotEmpty()) {
                        Text(
                            text = "Please enter a valid email address.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ════ SECTION 3: Health & Metrics ════
                SectionCard(
                    title = "Health & Metrics",
                    icon = Icons.Default.Favorite,
                    colors = colors
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Blood Group Dropdown
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Blood Group",
                                style = AppTextStyles.style14GCN(),
                                fontSize = 14.sp
                            )
                            EditProfileDropdown(
                                selectedValue = selectedBloodGroup?.name ?: "",
                                options = bloodGroupOptions.map { it.name },
                                onOptionSelected = { name ->
                                    selectedBloodGroup = bloodGroupOptions.firstOrNull { it.name == name }
                                },
                                placeholder = "Select",
                                colors = colors
                            )
                        }

                        // Weight
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EditProfileTextField(
                                value = weight,
                                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                                label = "Weight (kg)",
                                placeholder = "e.g. 65",
                                keyboardType = KeyboardType.Decimal,
                                colors = colors
                            )
                        }
                    }
                }

                // ════ SECTION 4: Location Details ════
                SectionCard(
                    title = "Location Details",
                    icon = Icons.Default.LocationOn,
                    colors = colors
                ) {
                    // Country + State row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Country
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Country", style = AppTextStyles.style14GCN(), fontSize = 14.sp)
                            EditProfileDropdown(
                                selectedValue = locationState.selectedCountryName,
                                options = locationState.countries.map { it.name },
                                onOptionSelected = { name ->
                                    val c = locationState.countries.firstOrNull { it.name == name }
                                    if (c != null) viewModel.onCountrySelected(c.id, c.name)
                                },
                                placeholder = "Select",
                                colors = colors
                            )
                        }

                        // State
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("State", style = AppTextStyles.style14GCN(), fontSize = 14.sp)
                            EditProfileDropdown(
                                selectedValue = locationState.selectedStateName,
                                options = locationState.states.map { it.name },
                                onOptionSelected = { name ->
                                    val s = locationState.states.firstOrNull { it.name == name }
                                    if (s != null) viewModel.onStateSelected(s.id, s.name)
                                },
                                placeholder = "Select",
                                colors = colors
                            )
                        }
                    }

                    // City + Zip row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // City
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("City", style = AppTextStyles.style14GCN(), fontSize = 14.sp)
                            EditProfileDropdown(
                                selectedValue = locationState.selectedCityName,
                                options = locationState.cities.map { it.name },
                                onOptionSelected = { name ->
                                    val c = locationState.cities.firstOrNull { it.name == name }
                                    if (c != null) viewModel.onCitySelected(c.id, c.name)
                                },
                                placeholder = "Select",
                                colors = colors
                            )
                        }

                        // Zip Code
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EditProfileTextField(
                                value = zipCode,
                                onValueChange = { zipCode = it.filter { c -> c.isDigit() } },
                                label = "Zip Code",
                                placeholder = "Zip Code",
                                keyboardType = KeyboardType.Number,
                                colors = colors
                            )
                        }
                    }

                    // Street Address
                    EditProfileTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Street Address",
                        placeholder = "Enter full address",
                        colors = colors
                    )
                }
            }

            // ════ PINNED SAVE BUTTON ════
            Column {
                Divider(color = colors.borderGreyLightColor.copy(alpha = 0.2f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val gradientColors = if (isFormValid)
                        listOf(colors.primaryBlueColor, colors.primaryBlueColor.copy(alpha = 0.85f))
                    else
                        listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = if (isFormValid) 8.dp else 0.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = colors.primaryBlueColor.copy(alpha = 0.3f),
                                spotColor = colors.primaryBlueColor.copy(alpha = 0.3f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(gradientColors))
                            .clickable(enabled = isFormValid && !loading) {
                                viewModel.updateProfile(
                                    firstName = firstName,
                                    lastName = lastName,
                                    dob = millisToApiDate(dobMillis),
                                    mobileNo = mobileNo,
                                    emailId = email,
                                    address = address,
                                    zip = zipCode,
                                    imageFile = selectedImageFile,
                                    gender = gender,
                                    bloodGroupId = selectedBloodGroup?.id ?: (patient?.bloodGroupId ?: 0),
                                    weight = weight,
                                    countryId = locationState.selectedCountryId ?: (patient?.countryId ?: 101),
                                    stateId = locationState.selectedStateId ?: (patient?.stateId ?: 0),
                                    cityId = locationState.selectedCityId ?: (patient?.cityId ?: 0)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save Changes",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Avatar Header Card
// ─────────────────────────────────────────────
@Composable
private fun AvatarHeaderCard(
    imageUrl: String?,
    selectedImageUri: Uri?,
    onEditClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            // Profile image
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clickable { onEditClick() }
            ) {
                val imageModel: Any? = selectedImageUri
                    ?: imageUrl?.takeIf { it.isNotEmpty() }

                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        colors.primaryBlueColor,
                                        colors.primaryBlueColor.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(R.drawable.img),
                        placeholder = androidx.compose.ui.res.painterResource(R.drawable.img)
                    )
                } else {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.img),
                        contentDescription = null,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        colors.primaryBlueColor,
                                        colors.primaryBlueColor.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Camera badge button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.primaryBlueColor)
                    .border(2.dp, colors.dashboardContainerColor, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = "Tap to change profile picture",
            style = AppTextStyles.style12GCN(),
            fontSize = 13.sp
        )
    }
}

// ─────────────────────────────────────────────
// Section Card
// ─────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    colors: com.critetiontech.vitalio_cis.ui.theme.MyColorScheme,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primaryBlueColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDarkColor
                )
            }

            Divider(color = colors.borderGreyLightColor.copy(alpha = 0.3f))

            content()
        }
    }
}

// ─────────────────────────────────────────────
// Gender Pill
// ─────────────────────────────────────────────
@Composable
private fun GenderPill(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    colors: com.critetiontech.vitalio_cis.ui.theme.MyColorScheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) colors.primaryBlueColor
                else colors.dashboardBackgroundColor
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else colors.borderGreyLightColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) Color.White else colors.textGreyColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else colors.textDarkColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────
// Edit Profile Text Field
// ─────────────────────────────────────────────
@Composable
private fun EditProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    colors: com.critetiontech.vitalio_cis.ui.theme.MyColorScheme
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primaryBlueColor,
        unfocusedBorderColor = colors.borderGreyLightColor.copy(alpha = 0.4f),
        focusedLabelColor = colors.primaryBlueColor,
        unfocusedLabelColor = colors.textGreyColor,
        focusedTextColor = colors.textDarkColor,
        unfocusedTextColor = colors.textDarkColor,
        disabledTextColor = colors.textGreyColor,
        disabledBorderColor = colors.borderGreyLightColor.copy(alpha = 0.3f),
        disabledLabelColor = colors.textGreyColor,
        cursorColor = colors.primaryBlueColor
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = colors.textGreyColor, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
        singleLine = label != "Street Address",
        maxLines = if (label == "Street Address") 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        readOnly = readOnly,
        enabled = !readOnly,
        shape = RoundedCornerShape(10.dp)
    )
}

// ─────────────────────────────────────────────
// Edit Profile Dropdown
// ─────────────────────────────────────────────
@Composable
private fun EditProfileDropdown(
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    placeholder: String,
    colors: com.critetiontech.vitalio_cis.ui.theme.MyColorScheme
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colors.dashboardContainerColor, RoundedCornerShape(10.dp))
                .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { if (options.isNotEmpty()) expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedValue.ifEmpty { placeholder },
                color = if (selectedValue.isEmpty()) colors.textGreyColor else colors.textDarkColor,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.textGreyColor
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = AppTextStyles.style14BCN()) },
                    onClick = {
                        expanded = false
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}
