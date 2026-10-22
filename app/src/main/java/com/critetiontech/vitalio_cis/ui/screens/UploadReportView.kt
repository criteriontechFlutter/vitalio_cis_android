package com.critetiontech.ctvitalio.ui.screens

import android.widget.Toast
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.MediaItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.StretchPullRefreshBox
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.ReportDownloader
import com.critetiontech.ctvitalio.viewmodel.UploadReportViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ALL_CATEGORIES = "All"
private val DownloadGreen = Color(0xFF15803D)
private val PdfRed = Color(0xFFDC2626)

// ------------------------------------------------------------
// SCREEN
// ------------------------------------------------------------

@Composable
fun LabReportsScreen(
    viewModel: UploadReportViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val haptic = rememberHaptics()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.fetchMedia() }

    val mediaList by viewModel.mediaList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }
    var isRefreshing by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<MediaItem?>(null) }

    // Newest first, like the dashboard's "recent" lists.
    val reports = remember(mediaList) { mediaList.sortedByDescending { it.dateTime ?: it.createdDate.orEmpty() } }

    val categories = remember(reports) {
        listOf(ALL_CATEGORIES to reports.size) +
            reports.groupBy { it.category?.trim().orEmpty().ifEmpty { "Other" } }
                .map { (name, items) -> name to items.size }
                .sortedByDescending { it.second }
    }

    val filtered = remember(reports, selectedCategory, query) {
        val q = query.trim()
        reports.filter { report ->
            (selectedCategory == ALL_CATEGORIES ||
                report.category?.trim().orEmpty().ifEmpty { "Other" } == selectedCategory) &&
                (q.isEmpty() || listOf(report.subcategory, report.category, report.remark, report.fileName)
                    .any { it?.contains(q, ignoreCase = true) == true })
        }
    }

    // ── Downloads ──────────────────────────────────────────────────────────────
    val downloads by ReportDownloader.states.collectAsState()
    var pendingDownload by remember { mutableStateOf<MediaItem?>(null) }

    fun startDownload(report: MediaItem) {
        val started = ReportDownloader.start(
            context, report.downloadKey(), report.url, report.fileName, report.fileType,
        ) { result ->
            scope.launch(Dispatchers.Main) {
                if (result is ReportDownloader.State.Done) {
                    haptic(HapticEvent.Success)
                    Toast.makeText(context, "Saved to Downloads/Vitalio", Toast.LENGTH_SHORT).show()
                } else {
                    haptic(HapticEvent.Error)
                    Toast.makeText(context, "Download failed. Check your connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (!started) {
            haptic(HapticEvent.Error)
            Toast.makeText(context, "This report has no file to download", Toast.LENGTH_SHORT).show()
        }
    }

    val storagePermission = rememberPermissionRequest(
        AppPermission.Storage, "to save this report in your Downloads folder",
        onDenied = { pendingDownload = null },
    ) {
        pendingDownload?.let { startDownload(it) }
        pendingDownload = null
    }

    val onDownload: (MediaItem) -> Unit = { report ->
        when (val state = downloads[report.downloadKey()]) {
            is ReportDownloader.State.Done ->
                if (!ReportDownloader.open(context, state))
                    Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
            is ReportDownloader.State.Running -> Unit
            else -> {
                pendingDownload = report
                storagePermission.launch()
            }
        }
    }

    val onView: (MediaItem) -> Unit = { report ->
        if (ReportDownloader.isImage(report.fileType, report.fileName)) viewing = report
        else if (!ReportDownloader.openRemote(context, report.url))
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    CommonAppBar(
        title = "Lab Reports",
        actions = {
            AddReportButton { navController.navigate(Routes.ADDLABRESULTS) }
            Spacer(Modifier.width(12.dp))
        }
    ) {
        StretchPullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    viewModel.fetchMedia()
                    delay(300)
                    kotlinx.coroutines.withTimeoutOrNull(20_000) { viewModel.loading.first { !it } }
                    isRefreshing = false
                }
            },
            refreshingText = "Updating your reports…",
            modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    ReportSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }

                if (reports.isNotEmpty()) item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 18.dp),
                    ) {
                        items(categories, key = { it.first }) { (name, count) ->
                            ReportCategoryCard(
                                name = name,
                                count = count,
                                selected = selectedCategory == name,
                                onClick = {
                                    if (selectedCategory != name) haptic(HapticEvent.Selection)
                                    selectedCategory = name
                                },
                            )
                        }
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 14.dp),
                    ) {
                        Text(
                            "Recent Reports",
                            color = colors.textDarkColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (filtered.isNotEmpty()) {
                            Text(
                                "${filtered.size} ${if (filtered.size == 1) "item" else "items"}",
                                color = colors.textGreyColor,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                when {
                    isLoading && reports.isEmpty() -> items(3) { LabReportCardShimmer() }
                    filtered.isEmpty() -> item {
                        ReportsEmptyState(
                            searching = query.isNotBlank() || selectedCategory != ALL_CATEGORIES,
                            onAdd = { navController.navigate(Routes.ADDLABRESULTS) },
                        )
                    }
                    else -> itemsIndexed(filtered, key = { _, r -> r.downloadKey() }) { i, report ->
                        AnimatedEnter(index = i) {
                            LabReportCard(
                                report = report,
                                downloadState = downloads[report.downloadKey()],
                                onDownload = { onDownload(report) },
                                onView = { onView(report) },
                            )
                        }
                    }
                }
            }
        }
    }

    viewing?.let { report ->
        ReportImageViewer(
            report = report,
            downloadState = downloads[report.downloadKey()],
            onDownload = { onDownload(report) },
            onDismiss = { viewing = null },
        )
    }
}

private fun MediaItem.downloadKey(): String = id?.toString() ?: url.orEmpty()

// ------------------------------------------------------------
// HEADER PIECES
// ------------------------------------------------------------

@Composable
private fun AddReportButton(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(10.dp, shape, ambientColor = colors.primaryBlueColor, spotColor = colors.primaryBlueColor)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(colors.primaryBlueColor, Color(0xFF3B5BDB))))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Add Report", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search by test name, category or notes…",
                    color = colors.textGreyColor.copy(alpha = 0.6f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textDarkColor, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primaryBlueColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            HapticIconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = colors.textGreyColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Accent colour and icon for a report category, so each type is recognisable at a glance. */
private data class CategoryStyle(val color: Color, val icon: ImageVector)

private val FallbackPalette = listOf(Color(0xFFF59E0B), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFEC4899))

@Composable
private fun categoryStyle(name: String?): CategoryStyle {
    val n = name.orEmpty().lowercase()
    return when {
        n == ALL_CATEGORIES.lowercase() -> CategoryStyle(LocalMyColorScheme.current.primaryBlueColor, Icons.Rounded.Description)
        "radio" in n -> CategoryStyle(Color(0xFF7C3AED), Icons.Rounded.MonitorHeart)
        listOf("imag", "scan", "x-ray", "xray", "mri", "ct").any { it in n } -> CategoryStyle(Color(0xFF0284C7), Icons.Rounded.Image)
        listOf("path", "lab", "blood", "urine").any { it in n } -> CategoryStyle(Color(0xFFE11D48), Icons.Rounded.Science)
        "prescri" in n -> CategoryStyle(Color(0xFF059669), Icons.Rounded.Medication)
        else -> CategoryStyle(FallbackPalette[Math.floorMod(n.hashCode(), FallbackPalette.size)], Icons.Rounded.Assignment)
    }
}

@Composable
private fun ReportCategoryCard(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val style = categoryStyle(name)
    val shape = RoundedCornerShape(22.dp)
    // Only the selected card gets an outline; the rest are plain white cards.
    val border by animateColorAsState(if (selected) style.color else Color.Transparent, label = "catBorder")
    val accent by animateColorAsState(if (selected) style.color else colors.textGreyColor, label = "catAccent")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) style.color.copy(alpha = 0.06f) else colors.dashboardContainerColor)
            .border(BorderStroke(2.dp, border), shape)
            .bounceClick(onClick = onClick)
            .padding(start = 12.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (selected) style.color.copy(alpha = 0.14f) else colors.textGreyColor.copy(alpha = 0.10f)),
        ) {
            Icon(style.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                name,
                color = if (selected) colors.textDarkColor else colors.textGreyColor,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
            Text("$count ${if (count == 1) "Report" else "Reports"}", color = accent, fontSize = 13.sp)
        }
    }
}

// ------------------------------------------------------------
// REPORT CARD
// ------------------------------------------------------------

@Composable
private fun LabReportCard(
    report: MediaItem,
    downloadState: ReportDownloader.State?,
    onDownload: () -> Unit,
    onView: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val style = categoryStyle(report.category)
    val shape = RoundedCornerShape(24.dp)
    val isImage = ReportDownloader.isImage(report.fileType, report.fileName)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(style.color.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box(Modifier.size(8.dp).background(style.color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(report.category?.ifBlank { null } ?: "Other", color = style.color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.weight(1f))
            FileTypeBadge(report.fileType, report.fileName, isImage)
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    report.subcategory?.ifBlank { null } ?: "Lab report",
                    color = colors.textDarkColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatReportDate(report.dateTime ?: report.createdDate), color = colors.textGreyColor, fontSize = 14.sp)
                }
                report.remark?.takeIf { it.isNotBlank() && it != "-" }?.let { remark ->
                    Spacer(Modifier.height(10.dp))
                    Text(remark, color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            ReportThumbnail(report, isImage, onClick = onView)
        }

        HorizontalDivider(
            color = colors.borderGreyLightColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 16.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                report.fileName.orEmpty(),
                color = colors.textGreyColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            DownloadPill(downloadState, onDownload)
            Spacer(Modifier.width(8.dp))
            ActionPill(
                icon = Icons.Rounded.Visibility,
                label = "View",
                color = colors.primaryBlueColor,
                onClick = onView,
            )
        }
    }
}

@Composable
private fun FileTypeBadge(fileType: String?, fileName: String?, isImage: Boolean) {
    val colors = LocalMyColorScheme.current
    val ext = fileName?.substringAfterLast('.', "")?.lowercase()
    val isPdf = fileType?.contains("pdf", true) == true || ext == "pdf"
    val tint = if (isPdf) PdfRed else colors.primaryBlueColor
    val label = (fileType?.takeIf { it.isNotBlank() } ?: ext?.ifBlank { null } ?: "file").uppercase()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            when { isPdf -> Icons.Rounded.PictureAsPdf; isImage -> Icons.Rounded.Image; else -> Icons.Rounded.InsertDriveFile },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, maxLines = 1)
    }
}

@Composable
private fun ReportThumbnail(report: MediaItem, isImage: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(colors.dashboardBackgroundColor)
            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.6f), shape)
            .bounceClick(onClick = onClick),
    ) {
        if (isImage) {
            AsyncImage(
                model = ReportDownloader.normalizeUrl(report.url),
                contentDescription = report.subcategory,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val isPdf = report.fileType?.contains("pdf", true) == true || report.fileName?.endsWith(".pdf", true) == true
            Icon(
                if (isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = if (isPdf) PdfRed else colors.textGreyColor,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        if (leading != null) leading()
        else Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Download → progress ring while it runs → "Open" once saved; "Retry" after a failure. */
@Composable
private fun DownloadPill(state: ReportDownloader.State?, onClick: () -> Unit) {
    when (state) {
        is ReportDownloader.State.Running -> ActionPill(
            icon = Icons.Rounded.Download,
            label = state.progress?.let { "${(it * 100).toInt()}%" } ?: "Saving",
            color = DownloadGreen,
            onClick = onClick,
            leading = {
                val p = state.progress
                if (p != null && p > 0f) CircularProgressIndicator(progress = { p }, color = DownloadGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else CircularProgressIndicator(color = DownloadGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            },
        )
        is ReportDownloader.State.Done -> ActionPill(Icons.Rounded.OpenInNew, "Open", DownloadGreen, onClick, leading = {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DownloadGreen, modifier = Modifier.size(18.dp))
        })
        ReportDownloader.State.Failed -> ActionPill(Icons.Rounded.Refresh, "Retry", PdfRed, onClick)
        null -> ActionPill(Icons.Rounded.Download, "Download", DownloadGreen, onClick)
    }
}

private fun formatReportDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        LocalDateTime.parse(raw.substringBefore('.')).format(DateTimeFormatter.ofPattern("dd MMM yyyy · hh:mm a", Locale.getDefault()))
    } catch (e: Exception) {
        raw
    }
}

// ------------------------------------------------------------
// EMPTY + LOADING
// ------------------------------------------------------------

@Composable
private fun ReportsEmptyState(searching: Boolean, onAdd: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp).clip(CircleShape).background(colors.primaryBlueColor.copy(alpha = 0.08f)),
        ) {
            Icon(
                if (searching) Icons.Rounded.Search else Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = colors.primaryBlueColor,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (searching) "No matching reports" else "No reports yet",
            color = colors.textDarkColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (searching) "Try a different name or category." else "Upload a lab report and Vitalio will read it for you.",
            color = colors.textGreyColor,
            fontSize = 14.sp,
        )
        if (!searching) {
            Spacer(Modifier.height(18.dp))
            AddReportButton(onAdd)
        }
    }
}

@Composable
private fun LabReportCardShimmer() {
    val colors = LocalMyColorScheme.current
    val transition = rememberInfiniteTransition(label = "reportShimmer")
    val x by transition.animateFloat(
        initialValue = -600f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "reportShimmerX",
    )
    val brush = Brush.linearGradient(
        listOf(colors.borderGreyLightColor.copy(alpha = 0.25f), colors.borderGreyLightColor.copy(alpha = 0.55f), colors.borderGreyLightColor.copy(alpha = 0.25f)),
        start = Offset(x, 0f), end = Offset(x + 500f, 200f),
    )
    @Composable
    fun Bar(w: Float, h: Int) = Box(Modifier.fillMaxWidth(w).height(h.dp).clip(RoundedCornerShape(8.dp)).background(brush))

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.dashboardContainerColor)
            .padding(18.dp),
    ) {
        Row {
            Bar(0.3f, 26)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bar(0.5f, 22); Bar(0.7f, 14); Bar(0.4f, 14)
            }
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).background(brush))
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Bar(0.6f, 14) }
            Box(Modifier.width(110.dp).height(36.dp).clip(RoundedCornerShape(50)).background(brush))
            Box(Modifier.width(80.dp).height(36.dp).clip(RoundedCornerShape(50)).background(brush))
        }
    }
}

// ------------------------------------------------------------
// IMAGE VIEWER
// ------------------------------------------------------------

/** Full-screen report image with pinch-to-zoom, pan and double-tap zoom. */
@Composable
private fun ReportImageViewer(
    report: MediaItem,
    downloadState: ReportDownloader.State?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = ReportDownloader.normalizeUrl(report.url),
                contentDescription = report.subcategory,
                contentScale = ContentScale.Fit,
                onState = { imageState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        })
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offset.x; translationY = offset.y
                    },
            )

            when (imageState) {
                is AsyncImagePainter.State.Loading ->
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                is AsyncImagePainter.State.Error ->
                    Text("Couldn't load the report", color = Color.White, modifier = Modifier.align(Alignment.Center))
                else -> Unit
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                HapticIconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(report.subcategory ?: "Report", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(report.fileName.orEmpty(), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HapticIconButton(onClick = onDownload) {
                    when (downloadState) {
                        is ReportDownloader.State.Running -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        is ReportDownloader.State.Done -> Icon(Icons.Rounded.OpenInNew, contentDescription = "Open download", tint = Color.White)
                        else -> Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.White)
                    }
                }
            }
        }
    }
}
