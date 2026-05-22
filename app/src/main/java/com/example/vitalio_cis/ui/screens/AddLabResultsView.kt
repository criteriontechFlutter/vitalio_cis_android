package com.example.vitalio_cis.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.viewmodel.UploadReportViewModel
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun AddLabResultsScreen(
    viewModel: UploadReportViewModel = viewModel()
) {

    val context = LocalContext.current

    val navController = LocalNavController.current

    // ---------------------------------------------------
    // DROPDOWN
    // ---------------------------------------------------

    val testTypeList = listOf(
        "Investigation",
        "Radiology",
        "Imaging",
        "Lab"
    )

    var expanded by remember {

        mutableStateOf(false)
    }

    // ---------------------------------------------------
    // FORM DATA
    // ---------------------------------------------------

    var testType by remember {

        mutableStateOf("Investigation")
    }

    var date by remember {

        mutableStateOf("2026-02-20 16:45")
    }

    var testName by remember {

        mutableStateOf("LFT")
    }

    var findings by remember {

        mutableStateOf("test")
    }

    // ---------------------------------------------------
    // FILE
    // ---------------------------------------------------

    var selectedFile by remember {

        mutableStateOf<File?>(null)
    }

    // ---------------------------------------------------
    // CAMERA URI
    // ---------------------------------------------------

    var cameraImageUri by remember {

        mutableStateOf<Uri?>(null)
    }

    // ---------------------------------------------------
    // CROP LAUNCHER
    // ---------------------------------------------------

    val cropLauncher =
        rememberLauncherForActivityResult(
            CropImageContract()
        ) { result ->

            if (result.isSuccessful) {

                val croppedUri =
                    result.uriContent

                if (croppedUri != null) {

                    selectedFile =
                        FileUtils.getFile(
                            context,
                            croppedUri
                        )

                    Log.d(
                        "CROP_IMAGE",
                        "File Path = ${selectedFile?.absolutePath}"
                    )

                    Log.d(
                        "CROP_IMAGE",
                        "File Size = ${selectedFile?.length()}"
                    )
                }

            } else {

                Log.e(
                    "CROP_IMAGE",
                    "Crop Error = ${result.error}"
                )
            }
        }

    // ---------------------------------------------------
    // START CROP
    // ---------------------------------------------------

    fun startCrop(uri: Uri) {

        cropLauncher.launch(

            CropImageContractOptions(

                uri = uri,

                cropImageOptions = CropImageOptions(

                    // -----------------------------------
                    // FREE CROP
                    // -----------------------------------

                    fixAspectRatio = false,

                    // -----------------------------------
                    // GUIDELINES
                    // -----------------------------------

                    guidelines =
                        CropImageView.Guidelines.ON,

                    // -----------------------------------
                    // TOOLBAR
                    // -----------------------------------

                    activityTitle = "Crop Image",

                    toolbarColor =
                        android.graphics.Color.BLACK,

                    toolbarBackButtonColor =
                        android.graphics.Color.WHITE,

                    toolbarTintColor =
                        android.graphics.Color.WHITE,

                    activityMenuIconColor =
                        android.graphics.Color.WHITE,

                    // -----------------------------------
                    // SHOW TOOLBAR
                    // -----------------------------------

                    noOutputImage = false,

                    // -----------------------------------
                    // OVERLAY
                    // -----------------------------------

                    showCropOverlay = true,

                    showProgressBar = true,

                    // -----------------------------------
                    // ROTATION
                    // -----------------------------------

                    allowRotation = true,

                    allowCounterRotation = true,

                    allowFlipping = true,

                    // -----------------------------------
                    // SHAPE
                    // -----------------------------------

                    cropShape =
                        CropImageView.CropShape.RECTANGLE,

                    // -----------------------------------
                    // FREE RESIZE
                    // -----------------------------------

                    multiTouchEnabled = true,

                    autoZoomEnabled = true,

                    maxZoom = 8,

                    // -----------------------------------
                    // FULL FREE CROP
                    // -----------------------------------

                    initialCropWindowPaddingRatio = 0f,

                    // -----------------------------------
                    // QUALITY
                    // -----------------------------------

                    outputCompressQuality = 100
                )
            )
        )
    }

    // ---------------------------------------------------
    // CAMERA LAUNCHER
    // ---------------------------------------------------

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.TakePicture()
        ) { success ->

            if (
                success &&
                cameraImageUri != null
            ) {

                startCrop(
                    cameraImageUri!!
                )
            }
        }

    // ---------------------------------------------------
    // CAMERA PERMISSION
    // ---------------------------------------------------

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                val uri =
                    createImageUri(context)

                cameraImageUri = uri

                cameraLauncher.launch(uri)
            }
        }

    // ---------------------------------------------------
    // GALLERY LAUNCHER
    // ---------------------------------------------------

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {

                startCrop(it)
            }
        }

    // ---------------------------------------------------
    // UI
    // ---------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F6FA))
            .padding(16.dp)
    ) {

        // ---------------------------------------------------
        // TEST TYPE
        // ---------------------------------------------------

        Text("Test Type")

        Spacer(Modifier.height(6.dp))

        Column {

            OutlinedTextField(
                value = testType,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        expanded = true
                    },

                trailingIcon = {

                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {

                    expanded = false
                }
            ) {

                testTypeList.forEach { item ->

                    DropdownMenuItem(
                        text = {

                            Text(item)
                        },

                        onClick = {

                            testType = item

                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------
        // DATE
        // ---------------------------------------------------

        Text("Date")

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = date,
            onValueChange = {

                date = it
            },
            modifier = Modifier.fillMaxWidth(),

            trailingIcon = {

                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------
        // TEST NAME
        // ---------------------------------------------------

        Text("Test Name")

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = testName,
            onValueChange = {

                testName = it
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------
        // FINDINGS
        // ---------------------------------------------------

        Text("Findings")

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = findings,
            onValueChange = {

                findings = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ---------------------------------------------------
        // CAMERA + GALLERY
        // ---------------------------------------------------

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // CAMERA

            UploadCard(
                title = "Camera",
                subtitle = "Capture from camera",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            ) {

                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }

            // GALLERY

            UploadCard(
                title = "Gallery",
                subtitle = "Upload from gallery",
                icon = Icons.Default.Upload,
                selected = true,
                modifier = Modifier.weight(1f)
            ) {

                galleryLauncher.launch(
                    "image/*"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------
        // FILE VIEW
        // ---------------------------------------------------

        selectedFile?.let { file ->

            val imageUri = remember(file) {

                Uri.fromFile(file)
            }

            Column(

                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {

                // -----------------------------------------
                // IMAGE PREVIEW
                // -----------------------------------------

                AsyncImage(

                    model = imageUri,

                    contentDescription = null,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),

                    contentScale = ContentScale.Crop
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // -----------------------------------------
                // FILE INFO
                // -----------------------------------------

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = file.name,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "${file.length() / 1024} KB",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    // -----------------------------------------
                    // REMOVE
                    // -----------------------------------------

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,

                        modifier = Modifier
                            .size(22.dp)
                            .clickable {

                                selectedFile = null
                            }
                    )
                }
            }
        }

        // ---------------------------------------------------
        // BUTTON
        // ---------------------------------------------------

        Button(
            onClick = {

                selectedFile?.let { file ->

                    Log.d(
                        "UPLOAD_FILE",
                        "Size = ${file.length()}"
                    )

                    Log.d(
                        "UPLOAD_FILE",
                        "Path = ${file.absolutePath}"
                    )
                    CoroutineScope(
                        Dispatchers.IO
                    ).launch {
                        viewModel.aiReport(
                            file , navController = navController
                            )
                    }

                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {

            Text("Upload & Save")
        }
    }
}

// ---------------------------------------------------
// CARD
// ---------------------------------------------------

@Composable
fun UploadCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected)
                    Color(0xFFE6F4F1)
                else
                    Color(0xFFF1F3F6)
            )
            .clickable {

                onClick()
            }
            .padding(16.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            icon,
            contentDescription = null
        )

        Spacer(Modifier.height(8.dp))

        Text(
            title,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------
// FILE ITEM
// ---------------------------------------------------

@Composable
fun FileItem(
    fileName: String,
    onRemove: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEDEFF3))
            .padding(12.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            fileName,
            modifier = Modifier.weight(1f)
        )

        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.clickable {

                onRemove()
            }
        )
    }
}

// ---------------------------------------------------
// FILE UTILS
// ---------------------------------------------------

object FileUtils {

    fun getFile(
        context: Context,
        uri: Uri
    ): File {

        val file = File(
            context.cacheDir,
            "cropped_${System.currentTimeMillis()}.jpg"
        )

        context.contentResolver
            .openInputStream(uri)?.use { input ->

                file.outputStream().use { output ->

                    input.copyTo(output)
                }
            }

        return file
    }
}

// ---------------------------------------------------
// CAMERA URI
// ---------------------------------------------------

fun createImageUri(
    context: Context
): Uri {

    val contentValues =
        ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "camera_image_${System.currentTimeMillis()}.jpg"
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )
        }

    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )!!
}