package com.example.vitalio_cis.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vitalio_cis.viewmodel.UploadReportViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddLabResultsScreen(
    viewModel: UploadReportViewModel = viewModel()
) {

    val context = LocalContext.current

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

        mutableStateOf("est")
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
    // CAMERA LAUNCHER
    // ---------------------------------------------------

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->

            if (
                success
                &&
                cameraImageUri != null
            ) {

                selectedFile = FileUtils.getFile(
                    context,
                    cameraImageUri!!
                )
            }
        }

    // ---------------------------------------------------
    // GALLERY LAUNCHER
    // ---------------------------------------------------

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {

                selectedFile = FileUtils.getFile(
                    context,
                    it
                )
            }
        }

    // ---------------------------------------------------
    // UI
    // ---------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // CAMERA

            UploadCard(
                title = "Camera",
                subtitle = "Capture from camera",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            ) {

                val uri = createImageUri(context)

                cameraImageUri = uri

                cameraLauncher.launch(uri)
            }

            // GALLERY

            UploadCard(
                title = "Gallery",
                subtitle = "Upload from gallery",
                icon = Icons.Default.Upload,
                selected = true,
                modifier = Modifier.weight(1f)
            ) {

                galleryLauncher.launch("*/*")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------------------------------------------
        // FILE VIEW
        // ---------------------------------------------------

        selectedFile?.let { file ->

            FileItem(
                fileName = file.name
            ) {

                selectedFile = null
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ---------------------------------------------------
        // BUTTON
        // ---------------------------------------------------

        Button(
            onClick = {

                selectedFile?.let { file ->

                    viewModel.AddMedia(
                        context = context,
                        file = file
                    )
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

        horizontalAlignment = Alignment.CenterHorizontally
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

        verticalAlignment = Alignment.CenterVertically
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

        val inputStream =
            context.contentResolver.openInputStream(uri)

        val file = File(
            context.cacheDir,
            "temp_${System.currentTimeMillis()}"
        )

        inputStream?.use { input ->

            FileOutputStream(file).use { output ->

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

    val contentValues = ContentValues().apply {

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