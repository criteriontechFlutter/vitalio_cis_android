package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.MyColorScheme
import com.critetiontech.ctvitalio.viewmodel.UploadPrescriptionViewModel
import java.io.File

// ─────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────

@Composable
fun UploadPrescriptionScreen(
    viewModel: UploadPrescriptionViewModel = viewModel(),
) {
    val context       = LocalContext.current
    val navController = LocalNavController.current
    val colors        = LocalMyColorScheme.current
    val uploadingFile by viewModel.uploadingFile.collectAsStateWithLifecycle()
    val selectedFile  by viewModel.selectedFile.collectAsStateWithLifecycle()
    var cameraUri     by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route -> navController.navigate(route) }
    }
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { showToast(context, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(TakePictureWithPermission()) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            copyUriToCache(context, uri)?.let { (file, mime) -> viewModel.setSelectedFile(file, mime) }
        }
    }

    val cameraPermission = rememberPermissionRequest(AppPermission.Camera, "to scan your prescription") {
        val uri = createImageUri(context)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            copyUriToCache(context, it)?.let { (file, mime) -> viewModel.setSelectedFile(file, mime) }
                ?: showToast(context, "Could not access file")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        CommonAppBar(title = "Upload Prescription") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    PrescriptionGuidanceCard(colors)

                    Spacer(Modifier.height(22.dp))

                    LabSectionHeader(title = "Select Document", colors = colors)
                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UploadOptionCard(
                            title = "Camera",
                            subtitle = "Take a photo of prescription",
                            icon = Icons.Default.CameraAlt,
                            isSelected = false,
                            colors = colors,
                            accent = CameraAccent,
                            modifier = Modifier.weight(1f)
                        ) { cameraPermission.launch() }

                        UploadOptionCard(
                            title = "Gallery / Files",
                            subtitle = "Upload image or PDF file",
                            icon = Icons.Default.Photo,
                            isSelected = selectedFile != null,
                            colors = colors,
                            accent = GalleryAccent,
                            modifier = Modifier.weight(1f)
                        ) { documentLauncher.launch(arrayOf("image/*", "application/pdf")) }
                    }

                    selectedFile?.let { file ->
                        Spacer(Modifier.height(22.dp))
                        SelectedPrescriptionCard(file, colors) { viewModel.setSelectedFile(null) }
                    }

                    Spacer(Modifier.height(22.dp))
                    PrescriptionTipsSection(colors)
                    Spacer(Modifier.height(16.dp))
                }

                // Fixed footer action button
                HapticButton(
                    onClick = { viewModel.uploadPrescriptionToAnalyse() },
                    enabled = selectedFile != null && !uploadingFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryBlueColor,
                        disabledContainerColor = colors.borderGreyLightColor
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyse Prescription", style = AppTextStyles.style16WCB())
                }
            }
        }

        if (uploadingFile) {
            AnalyzingOverlay(colors = colors, message = "Reading your prescription…")
        }
    }
}

// ─────────────────────────────────────────────────────────
// SECTIONS
// ─────────────────────────────────────────────────────────

@Composable
private fun PrescriptionGuidanceCard(colors: MyColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.primaryBlueColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("AI Prescription Reader", style = AppTextStyles.style16BCB())
                Spacer(Modifier.height(4.dp))
                Text(
                    "Upload a clear photo or PDF of your doctor's prescription to automatically extract medicines, dosage schedules, and advice.",
                    style = AppTextStyles.style12GCN()
                )
            }
        }
    }
}

@Composable
private fun SelectedPrescriptionCard(file: File, colors: MyColorScheme, onRemove: () -> Unit) {
    val isPdf = file.extension.equals("pdf", ignoreCase = true)
    Column {
        Text("Selected File", style = AppTextStyles.style14BCB())
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isPdf) {
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
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryBlueColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = colors.primaryBlueColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = AppTextStyles.style14BCB(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${file.extension.uppercase()} • ${file.length() / 1024} KB", style = AppTextStyles.style12GCN())
                    }
                    HapticIconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.textGreyColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionTipsSection(colors: MyColorScheme) {
    Column {
        Text("Tips for Best Results", style = AppTextStyles.style14BCB())
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrescriptionTipRow(Icons.Default.WbSunny, "Ensure good lighting and avoid shadows on paper.", colors)
                PrescriptionTipRow(Icons.Default.CropFree, "Capture the entire page including doctor notes and header.", colors)
                PrescriptionTipRow(Icons.Default.TextFields, "Ensure handwriting or printed text is sharply focused.", colors)
            }
        }
    }
}

@Composable
private fun PrescriptionTipRow(icon: ImageVector, text: String, colors: MyColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = AppTextStyles.style12GCN())
    }
}

// ─────────────────────────────────────────────────────────
// FILE UTILS
// ─────────────────────────────────────────────────────────

/** Copies a content Uri into cacheDir keeping its extension; returns the file and its mime type. */
private fun copyUriToCache(context: Context, uri: Uri): Pair<File, String>? = try {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
    val ext = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
        ?: if (mime == "application/pdf") "pdf" else "jpg"
    val file = File(context.cacheDir, "prescription_${System.currentTimeMillis()}.$ext")
    resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
    if (file.length() > 0) file to mime else null
} catch (e: Exception) { null }