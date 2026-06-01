package com.critetiontech.vitalio_cis.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.critetiontech.vitalio_cis.viewmodel.EditProfileViewModel
import java.io.File

@Composable
fun PersonalInfoScreen(
    viewModel: EditProfileViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val prefs = PrefsManager(context)
    val patient = prefs.getPatient()

    val loading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Form fields
    var firstName by rememberSaveable { mutableStateOf(patient?.firstName.orEmpty()) }
    var lastName by rememberSaveable { mutableStateOf(patient?.lastName.orEmpty()) }
    var dob by rememberSaveable { mutableStateOf(patient?.dob.orEmpty()) }
    var mobileNo by rememberSaveable { mutableStateOf(patient?.mobileNo.orEmpty()) }
    var emailId by rememberSaveable { mutableStateOf(patient?.emailAddress.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(patient?.address.orEmpty()) }
    var zip by rememberSaveable { mutableStateOf(patient?.zip.orEmpty()) }

    // Image state
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSheet by rememberSaveable { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedImageFile = FileUtils.getFile(context, it)
        }
    }

    // Camera launcher
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

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            isEditing = false
            viewModel.resetSuccess()
        }
    }

    CommonAppBar(
        title = "Personal Info",
        actions = {
            if (isEditing) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable {
                            isEditing = false
                            // Reset fields to saved values
                            val p = prefs.getPatient()
                            firstName = p?.firstName.orEmpty()
                            lastName = p?.lastName.orEmpty()
                            dob = p?.dob.orEmpty()
                            mobileNo = p?.mobileNo.orEmpty()
                            emailId = p?.emailAddress.orEmpty()
                            address = p?.address.orEmpty()
                            zip = p?.zip.orEmpty()
                            selectedImageFile = null
                            selectedImageUri = null
                        }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile image
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        val imageModel = selectedImageUri
                            ?: patient?.imageUrl?.takeIf { it.isNotEmpty() }

                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.img),
                                placeholder = painterResource(id = R.drawable.img)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1557FF))
                                    .align(Alignment.BottomEnd)
                                    .clickable { showImageSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1557FF))
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isEditing) {
                    EditableFields(
                        firstName = firstName, onFirstNameChange = { firstName = it },
                        lastName = lastName, onLastNameChange = { lastName = it },
                        dob = dob, onDobChange = { dob = it },
                        mobileNo = mobileNo, onMobileChange = { mobileNo = it },
                        emailId = emailId, onEmailChange = { emailId = it },
                        address = address, onAddressChange = { address = it },
                        zip = zip, onZipChange = { zip = it }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard {
                            ProfileInfoRow("First Name", patient?.firstName.orEmpty())
                            ProfileInfoRow("Last Name", patient?.lastName.orEmpty())
                            ProfileInfoRow("Sex", patient?.genderName.orEmpty())
                            ProfileInfoRow("Date of Birth", patient?.dob.orEmpty())
                        }
                        InfoCard {
                            ProfileInfoRow("Mobile No.", patient?.mobileNo.orEmpty())
                            ProfileInfoRow("Email", patient?.emailAddress.orEmpty())
                        }
                        InfoCard {
                            ProfileInfoRow("Street Address", patient?.address.orEmpty())
                            ProfileInfoRow("Zip Code", patient?.zip.orEmpty())
                            ProfileInfoRow("City", patient?.cityName.orEmpty())
                            ProfileInfoRow("State", patient?.stateName.orEmpty())
                            ProfileInfoRow("Country", patient?.countryName.orEmpty())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom button
            Button(
                onClick = {
                    if (isEditing) {
                        viewModel.updateProfile(
                            context = context,
                            firstName = firstName,
                            lastName = lastName,
                            dob = dob,
                            mobileNo = mobileNo,
                            emailId = emailId,
                            address = address,
                            zip = zip,
                            imageFile = selectedImageFile
                        )
                    } else {
                        isEditing = true
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1557FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(55.dp),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Save Changes" else "Edit Personal Info")
                }
            }
        }
    }

    if (showImageSheet) {
        ImagePickerBottomSheet(
            onCameraClick = {
                showImageSheet = false
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
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
}

@Composable
private fun EditableFields(
    firstName: String, onFirstNameChange: (String) -> Unit,
    lastName: String, onLastNameChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    mobileNo: String, onMobileChange: (String) -> Unit,
    emailId: String, onEmailChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    zip: String, onZipChange: (String) -> Unit
) {
    val colors = LocalMyColorScheme.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF1557FF),
        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
        focusedLabelColor = Color(0xFF1557FF),
        unfocusedLabelColor = Color.Gray,
        focusedTextColor = colors.textDarkColor,
        unfocusedTextColor = colors.textDarkColor,
        cursorColor = Color(0xFF1557FF)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName, onValueChange = onFirstNameChange,
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true
                )
                OutlinedTextField(
                    value = lastName, onValueChange = onLastNameChange,
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true
                )
                OutlinedTextField(
                    value = dob, onValueChange = onDobChange,
                    label = { Text("Date of Birth (dd-MM-yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = mobileNo, onValueChange = onMobileChange,
                    label = { Text("Mobile No.") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = emailId, onValueChange = onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = address, onValueChange = onAddressChange,
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, maxLines = 3
                )
                OutlinedTextField(
                    value = zip, onValueChange = onZipChange,
                    label = { Text("Zip Code") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    val colors = LocalMyColorScheme.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = colors.textGreyColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value.ifEmpty { "—" },
            color = colors.textDarkColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}