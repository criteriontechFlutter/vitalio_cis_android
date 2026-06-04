  package com.critetiontech.vitalio_cis.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.ui.theme.MyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.UploadReportViewModel
import java.io.File
import androidx.core.graphics.toColorInt

  // ─────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────

@Composable
fun AddLabResultsScreen(
    viewModel: UploadReportViewModel = viewModel(),
) {
    val context      = LocalContext.current
    val navController = LocalNavController.current
    val colors       = LocalMyColorScheme.current
    val isAnalyzing  by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    // ── Form state ───────────────────────────────────────
    val testTypeList = listOf("Investigation", "Radiology", "Imaging", "Lab")
    var expanded     by remember { mutableStateOf(false) }
    var testType     by remember { mutableStateOf("Investigation") }
    var date         by remember { mutableStateOf("") }
    var testName     by remember { mutableStateOf("") }
    var findings     by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var cameraUri    by remember { mutableStateOf<Uri?>(null) }

    // ── Crop launcher ────────────────────────────────────
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { selectedFile = FileUtils.getFile(context, it) }
        } else {
            Log.e("CROP", "Crop failed: ${result.error}")
        }
    }

    @SuppressLint("UseKtx")
    fun startCrop(uri: Uri) = cropLauncher.launch(
        CropImageContractOptions(
            uri = uri,
            cropImageOptions = CropImageOptions(
                fixAspectRatio = false,
                guidelines = CropImageView.Guidelines.ON,
                activityTitle = "Crop Report Image",
                toolbarColor = "#1564ED".toColorInt(),
                toolbarBackButtonColor = android.graphics.Color.WHITE,
                toolbarTintColor = android.graphics.Color.WHITE,
                activityMenuIconColor = android.graphics.Color.WHITE,
                noOutputImage = false,
                showCropOverlay = true,
                showProgressBar = true,
                allowRotation = true,
                allowCounterRotation = true,
                allowFlipping = true,
                cropShape = CropImageView.CropShape.RECTANGLE,
                multiTouchEnabled = true,
                autoZoomEnabled = true,
                maxZoom = 8,
                initialCropWindowPaddingRatio = 0f,
                outputCompressQuality = 100
            )
        )
    )

    val cameraLauncher = rememberLauncherForActivityResult(TakePictureWithPermission()) { success ->
        if (success && cameraUri != null) startCrop(cameraUri!!)
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { startCrop(it) } }

    // ── UI ───────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        CommonAppBar(title = "Add Lab Report") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {

                // ─── Report Details section ───────────────
                LabSectionHeader(title = "Report Details", colors = colors)
                Spacer(Modifier.height(14.dp))

                // Test Type dropdown
                LabFormLabel("Test Type", colors)
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = testType,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colors.textGreyColor
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = labFieldColors(colors)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(colors.dashboardContainerColor)
                    ) {
                        testTypeList.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, style = AppTextStyles.style14BCN()) },
                                onClick = { testType = item; expanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Test name
                LabFormLabel("Test Name / Sub Category", colors)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = testName,
                    onValueChange = { testName = it },
                    placeholder = { Text("e.g. LFT, CBC, Blood Sugar", color = colors.textGreyColor.copy(alpha = 0.6f), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = labFieldColors(colors)
                )

                Spacer(Modifier.height(14.dp))

                // Date
                LabFormLabel("Date & Time", colors)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    placeholder = { Text("YYYY-MM-DD  HH:mm", color = colors.textGreyColor.copy(alpha = 0.6f), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = colors.primaryBlueColor)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = labFieldColors(colors)
                )

                Spacer(Modifier.height(14.dp))

                // Findings
                LabFormLabel("Findings / Remarks", colors)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = findings,
                    onValueChange = { findings = it },
                    placeholder = { Text("Add remarks or findings…", color = colors.textGreyColor.copy(alpha = 0.6f), fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = labFieldColors(colors)
                )

                Spacer(Modifier.height(26.dp))

                // ─── Attach Report section ────────────────
                LabSectionHeader(title = "Attach Report Image", colors = colors)
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UploadOptionCard(
                        title = "Camera",
                        subtitle = "Capture photo",
                        icon = Icons.Default.CameraAlt,
                        isSelected = false,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    ) { cameraPermLauncher.launch(Manifest.permission.CAMERA) }

                    UploadOptionCard(
                        title = "Gallery",
                        subtitle = "Choose image",
                        icon = Icons.Default.Photo,
                        isSelected = selectedFile != null,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    ) { galleryLauncher.launch("image/*") }
                }

                // Image preview
                selectedFile?.let { file ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = Uri.fromFile(file),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.primaryBlueColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = colors.primaryBlueColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, style = AppTextStyles.style14BCB(), maxLines = 1)
                                    Text("${file.length() / 1024} KB", style = AppTextStyles.style12GCN())
                                }
                                IconButton(onClick = { selectedFile = null }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = colors.textGreyColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Analyze button
                Button(
                    onClick = {
                        selectedFile?.let { file ->
                            viewModel.aiReport(
                                file = file,
                                navController = navController,
                                category = testType,
                                dateTime = date.ifEmpty { "2026-01-01 00:00" },
                                subCategory = testName.ifEmpty { "General" },
                                remark = findings.ifEmpty { "-" }
                            )
                        }
                    },
                    enabled = selectedFile != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryBlueColor,
                        disabledContainerColor = colors.borderGreyLightColor
                    )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Analyze with AI",
                        style = AppTextStyles.style16WCB()
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // ── Analyzing overlay ────────────────────────────
        if (isAnalyzing) {
            AnalyzingOverlay(colors = colors)
        }
    }
}

// ─────────────────────────────────────────────────────────
// ANALYZING OVERLAY
// ─────────────────────────────────────────────────────────

@Composable
fun AnalyzingOverlay(colors: MyColorScheme) {

    val infiniteTransition = rememberInfiniteTransition(label = "ai_anim")

    // Outer ring pulse
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )

    // Inner icon pulse
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_scale"
    )

    // Rotation for ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Dot animations (staggered)
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0; 1f at 200; 0.2f at 400; 0.2f at 1200
            },
            repeatMode = RepeatMode.Restart
        ), label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0; 0.2f at 200; 1f at 400; 0.2f at 600; 0.2f at 1200
            },
            repeatMode = RepeatMode.Restart
        ), label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0; 0.2f at 400; 1f at 600; 0.2f at 800; 0.2f at 1200
            },
            repeatMode = RepeatMode.Restart
        ), label = "d3"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Animated icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(outerScale)
                                .clip(CircleShape)
                                .background(colors.primaryBlueColor.copy(alpha = 0.12f))
                        )
                        // Rotating arc (dashed border effect)
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(84.dp)
                                .graphicsLayer { rotationZ = rotation },
                            color = colors.primaryBlueColor,
                            strokeWidth = 3.dp,
                            trackColor = colors.primaryBlueColor.copy(alpha = 0.15f)
                        )
                        // Inner icon
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .scale(innerScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            colors.primaryBlueColor,
                                            Color(0xFF0A3FA8)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "AI is Analyzing",
                        style = AppTextStyles.style18BCB(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Reading your lab report…",
                        style = AppTextStyles.style14GCN(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Animated dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryBlueColor.copy(alpha = alpha))
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "This may take a few seconds",
                        style = AppTextStyles.style12GCN(),
                        textAlign = TextAlign.Center,
                        color = colors.textGreyColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────

@Composable
fun LabSectionHeader(title: String, colors: MyColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primaryBlueColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = AppTextStyles.style16BCB())
    }
}

@Composable
fun LabFormLabel(text: String, colors: MyColorScheme) {
    Text(
        text = text,
        style = AppTextStyles.style12GCB(),
        color = colors.textGreyColor
    )
}

@Composable
fun labFieldColors(colors: MyColorScheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = colors.primaryBlueColor,
    unfocusedBorderColor = colors.borderGreyLightColor,
    focusedContainerColor   = colors.dashboardContainerColor,
    unfocusedContainerColor = colors.dashboardContainerColor,
    focusedTextColor   = colors.textDarkColor,
    unfocusedTextColor = colors.textDarkColor,
    cursorColor        = colors.primaryBlueColor
)

@Composable
fun UploadOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    colors: MyColorScheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) colors.primaryBlueColor else colors.borderGreyLightColor
    val bgColor     = if (isSelected) colors.primaryBlueColor.copy(alpha = 0.07f) else colors.dashboardContainerColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) colors.primaryBlueColor.copy(alpha = 0.15f)
                    else colors.dashboardBackgroundColor
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) colors.primaryBlueColor else colors.textGreyColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = AppTextStyles.style14BCB(),
            color = if (isSelected) colors.primaryBlueColor else colors.textDarkColor
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            style = AppTextStyles.style12GCN(),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────
// FILE UTILS
// ─────────────────────────────────────────────────────────

object FileUtils {
    fun getFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}

fun createImageUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "camera_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )!!
}

class TakePictureWithPermission : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, input)
        }
}
