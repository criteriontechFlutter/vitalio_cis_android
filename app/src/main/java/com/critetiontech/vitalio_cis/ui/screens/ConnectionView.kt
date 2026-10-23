package com.critetiontech.ctvitalio.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.critetiontech.ctvitalio.ui.components.Caption
import com.critetiontech.ctvitalio.ui.components.DeviceGreen
import com.critetiontech.ctvitalio.ui.components.brandBrush
import com.critetiontech.ctvitalio.ui.components.tintFill
import com.critetiontech.ctvitalio.ui.components.syncedAgo
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.omron.OmronKind
import com.critetiontech.ctvitalio.omron.OmronManager
import com.critetiontech.ctvitalio.omron.OmronModels
import com.critetiontech.ctvitalio.omron.PairedOmronDevice
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.VitalDetailViewModel
import com.critetiontech.myapplication.utils.LocalNavController

data class Device(
    val name: String,
    val type: String,
    /** Omron model number, for devices that can be paired. */
    val code: String? = null
)

/** Only the Omron devices that can measure this vital: BP machines for BP/pulse, scales for weight. */
internal fun omronDevicesFor(vitalName: String): List<Device> {
    val vital = vitalName.lowercase()
    val kind = when {
        listOf("pressure", "bp", "pulse", "heart").any { it in vital } -> OmronKind.BP
        "weight" in vital -> OmronKind.SCALE
        else -> return emptyList()
    }
    return OmronModels.ALL.filter { it.kind == kind }.map { Device("Omron ${it.code}", it.label, it.code) }
}

/* ================= INPUT RULES ================= */

/**
 * One number the patient types. [min]..[max] is what a meter can plausibly read (not the normal
 * range) — anything outside is almost certainly a typo.
 */
private data class VitalField(
    val key: String,
    val label: String,
    val fullName: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val decimals: Int = 0,
    val required: Boolean = true,
) {
    /** Longest text allowed while typing, e.g. "250" or "104.5". */
    val maxLength: Int get() = max.toInt().toString().length + if (decimals > 0) decimals + 1 else 0
}

/**
 * A unit the patient can type in (°C, lb, ft·in…). [toCanonical] turns the typed numbers into the
 * vital's canonical unit (°C, kg or cm) and [fromCanonical] does the reverse, so a value survives a
 * unit switch and is always sent in the backend's unit.
 */
private data class UnitMode(
    val id: String,
    val chip: String,
    val fields: List<VitalField>,
    val toCanonical: (Map<String, Double>) -> Double = { it.values.first() },
    val fromCanonical: (Double) -> Map<String, String> = { mapOf(fields.first().key to formatValue(round1(it))) },
)

/** What can be entered for a vital: one or more unit modes, and how to reach the backend's unit. */
private data class VitalSpec(
    val modes: List<UnitMode>,
    /** Canonical value → the number the backend stores (in [backendUnit]). */
    val toBackend: (Double) -> Double = { it },
    val backendUnit: String = "",
)

private fun round1(v: Double) = Math.round(v * 10) / 10.0

private const val LB_PER_KG = 2.20462
private const val CM_PER_INCH = 2.54

/** Plain single-unit vital: what's typed is what's sent. */
private fun single(field: VitalField) = VitalSpec(listOf(UnitMode("", "", listOf(field))), backendUnit = field.unit)

/**
 * The fields for [vitalName] (the vitals list's display name) given the backend's [unit], or null if
 * it can't be added here. Temperature, weight and height can be typed in either system; the value is
 * converted to the backend's unit before it is sent.
 */
private fun specFor(vitalName: String, unit: String): VitalSpec? {
    val backendUnit = unit.trim().removePrefix("/").trim()
    return when (vitalName.trim().lowercase()) {
        "blood pressure" -> VitalSpec(
            listOf(UnitMode("", "", listOf(
                VitalField("sys", "SYS", "Systolic pressure", backendUnit.ifBlank { "mmHg" }, 50.0, 250.0),
                VitalField("dia", "DIA", "Diastolic pressure", backendUnit.ifBlank { "mmHg" }, 30.0, 150.0),
                VitalField("pulse", "Pulse", "Pulse", "/min", 30.0, 220.0, required = false),
            ))),
            backendUnit = backendUnit.ifBlank { "mmHg" },
        )
        "pulse" -> single(VitalField("value", "Pulse", "Pulse", unit, 30.0, 220.0))
        "heart rate", "heartrate" -> single(VitalField("value", "Heart Rate", "Heart rate", unit, 30.0, 220.0))
        "spo2" -> single(VitalField("value", "SpO2", "SpO2", unit, 50.0, 100.0))
        "respiratory rate", "resprate" -> single(VitalField("value", "Resp. Rate", "Respiratory rate", unit, 5.0, 60.0))
        "blood glucose", "rbs" -> single(VitalField("value", "Glucose", "Blood glucose", unit, 20.0, 600.0))
        "temperature" -> {
            // Canonical: °C. Backend usually stores °F.
            val backendIsC = backendUnit.contains("C", true) && !backendUnit.contains("F", true)
            VitalSpec(
                modes = listOf(
                    UnitMode(
                        "F", "°F", listOf(VitalField("value", "Temp", "Temperature", "°F", 86.0, 110.0, decimals = 1)),
                        toCanonical = { (it.getValue("value") - 32) * 5 / 9 },
                        fromCanonical = { mapOf("value" to formatValue(round1(it * 9 / 5 + 32))) },
                    ),
                    UnitMode(
                        "C", "°C", listOf(VitalField("value", "Temp", "Temperature", "°C", 30.0, 43.5, decimals = 1)),
                    ),
                ),
                toBackend = { c -> if (backendIsC) c else c * 9 / 5 + 32 },
                backendUnit = backendUnit.ifBlank { "°F" },
            )
        }
        "weight" -> {
            // Canonical: kg. Backend stores kg.
            val backendIsLb = backendUnit.contains("lb", true)
            VitalSpec(
                modes = listOf(
                    UnitMode("kg", "kg", listOf(VitalField("value", "Weight", "Weight", "kg", 2.0, 300.0, decimals = 1))),
                    UnitMode(
                        "lb", "lb", listOf(VitalField("value", "Weight", "Weight", "lb", 4.0, 660.0, decimals = 1)),
                        toCanonical = { it.getValue("value") / LB_PER_KG },
                        fromCanonical = { mapOf("value" to formatValue(round1(it * LB_PER_KG))) },
                    ),
                ),
                toBackend = { kg -> if (backendIsLb) kg * LB_PER_KG else kg },
                backendUnit = backendUnit.ifBlank { "kg" },
            )
        }
        "height" -> {
            // Canonical: cm. Backend stores cm.
            val backendIsInch = backendUnit.equals("in", true) || backendUnit.contains("inch", true)
            VitalSpec(
                modes = listOf(
                    UnitMode("cm", "cm", listOf(VitalField("value", "Height", "Height", "cm", 30.0, 250.0, decimals = 1))),
                    UnitMode(
                        "ftin", "ft · in",
                        listOf(
                            VitalField("ft", "Feet", "Feet", "ft", 1.0, 8.0),
                            VitalField("in", "Inches", "Inches", "in", 0.0, 11.0, required = false),
                        ),
                        toCanonical = { ((it["ft"] ?: 0.0) * 12 + (it["in"] ?: 0.0)) * CM_PER_INCH },
                        fromCanonical = { cm ->
                            val totalIn = Math.round(cm / CM_PER_INCH).toInt()
                            mapOf("ft" to (totalIn / 12).toString(), "in" to (totalIn % 12).toString())
                        },
                    ),
                ),
                toBackend = { cm -> if (backendIsInch) cm / CM_PER_INCH else cm },
                backendUnit = backendUnit.ifBlank { "cm" },
            )
        }
        else -> null
    }
}

/**
 * Vitals the AddVital API ignores today: it answers "success" for vmValueWeight/vmValueHeight but
 * doesn't store them. Their entry (with kg/lb and cm/ft·in conversion) is ready; remove a name here
 * once the backend saves it.
 */
private val NOT_SAVED_BY_BACKEND = setOf("weight", "height")

/** Whether this vital can be entered manually. */
fun canAddVitalManually(vitalName: String): Boolean =
    vitalName.trim().lowercase() !in NOT_SAVED_BY_BACKEND && specFor(vitalName, "") != null

/** "°F" and "F" (the backend sends both) compare equal. */
private fun sameUnit(a: String, b: String) = a.replace("°", "").trim().equals(b.replace("°", "").trim(), ignoreCase = true)

/** Keeps only what [field] can hold: digits, and one decimal point with limited places. */
private fun sanitize(input: String, field: VitalField): String {
    var dotSeen = false
    val cleaned = buildString {
        input.forEach { c ->
            when {
                c.isDigit() -> append(c)
                c == '.' && field.decimals > 0 && !dotSeen -> { dotSeen = true; append(c) }
            }
        }
    }
    val (whole, frac) = cleaned.split('.', limit = 2).let { it[0] to it.getOrNull(1) }
    val trimmed = if (frac != null) "$whole.${frac.take(field.decimals)}" else whole
    return trimmed.take(field.maxLength)
}

/** Why [text] isn't acceptable for [field], or null if it is. Empty optional fields are fine. */
private fun fieldError(field: VitalField, text: String): String? {
    if (text.isBlank()) return if (field.required) "Enter ${field.fullName.lowercase()}" else null
    val value = text.toDoubleOrNull() ?: return "Enter a valid number"
    if (value < field.min || value > field.max) {
        return "${field.fullName} should be between ${formatValue(field.min)} and ${formatValue(field.max)} ${field.unit}".trim()
    }
    return null
}

private const val SHOW_NEW_DEVICE = false

/** Remembers the unit each patient prefers per vital (e.g. °C for temperature). */
private const val UNIT_PREFS = "vital_entry_units"

/* ================= SCREEN ================= */

@Composable
fun ConnectionScreen(
    vitalName: String,
    unitName: String,
    viewModel: VitalDetailViewModel = viewModel()
) {
    val devices = remember(vitalName) { omronDevicesFor(vitalName) }

    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    var showDialog by remember { mutableStateOf(false) }

    // Re-read paired Omron devices whenever the screen comes back (e.g. after pairing)
    val context = LocalContext.current
    var paired by remember { mutableStateOf(emptyMap<String?, PairedOmronDevice>()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                paired = OmronManager.pairedDevices(context).associateBy { it.code }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        RoundBackTopBar(
            title = "Connection",
            // New Device is hidden for now; wearables are reached from the row below the devices
            actions = { if (SHOW_NEW_DEVICE) NewDevicePill { navController.navigate(Routes.HEALTHCONNECTSYNC) } },
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Your devices, then watches, then the Omron models that can still be paired
            val (mine, others) = devices.partition { paired[it.code] != null }
            var index = 0
            fun open(device: Device) { device.code?.let { navController.navigate("${Routes.OMRON_DEVICE}/$it") } }
            if (mine.isNotEmpty()) {
                AnimatedEnter(index = index++) { Caption("Your devices", modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
                mine.forEach { device -> AnimatedEnter(index = index++) { DeviceRow(device, paired = paired[device.code]) { open(device) } } }
            }
            AnimatedEnter(index = index++) { Caption("Watches & health apps", modifier = Modifier.padding(start = 4.dp, top = 10.dp)) }
            AnimatedEnter(index = index++) { WearablesRow { navController.navigate(Routes.HEALTHCONNECTSYNC) } }
            if (others.isNotEmpty()) {
                AnimatedEnter(index = index++) { Caption("Add an Omron device", modifier = Modifier.padding(start = 4.dp, top = 10.dp)) }
                others.forEach { device -> AnimatedEnter(index = index++) { DeviceRow(device, paired = null) { open(device) } } }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            "Add Vital Manually",
            color = colors.primaryBlueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.primaryBlueColor, RoundedCornerShape(12.dp))
                .bounceClick { showDialog = true }
                .padding(vertical = 15.dp),
        )
    }

    if (showDialog) {
        AddVitalDialog(
            vitalName = vitalName,
            unitName = unitName,
            viewModel = viewModel,
            onDismiss = { showDialog = false; viewModel.clearAddError() },
        )
    }
}

@Composable
private fun NewDevicePill(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.dashboardContainerColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text("New Device", color = colors.primaryBlueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Opens Wearable Sync (Galaxy Watch, Fitbit, Garmin… via Health Connect). */
@Composable
private fun WearablesRow(onClick: () -> Unit) {
    // Brand-gradient card so the "connect anything" entry stands out from the device list
    val float = rememberInfiniteTransition(label = "watchFloat")
    val lift by float.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(brandBrush())
            .bounceClick(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)),
        ) {
            Icon(
                Icons.Rounded.Watch, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(28.dp).graphicsLayer { translationY = -3.dp.toPx() * lift }
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Smartwatch & Health Apps", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Galaxy Watch, Fitbit, Garmin & more", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
        ) { Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun DeviceRow(device: Device, paired: PairedOmronDevice?, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    val isScale = "Scale" in device.type || "Body" in device.type
    val pulse = rememberInfiniteTransition(label = "pairedPulse")
    val dot by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardContainerColor)
            .bounceClick(onClick = onClick)
            .padding(14.dp),
    ) {
        val iconBox = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (paired != null) iconBox.background(brandBrush()) else iconBox.background(tintFill(p)),
        ) {
            Icon(
                if (isScale) Icons.Rounded.MonitorWeight else Icons.Rounded.MonitorHeart, contentDescription = null,
                tint = if (paired != null) Color.White else p, modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            if (paired != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(tintFill(DeviceGreen)).padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Box(Modifier.size(6.dp).graphicsLayer { alpha = dot }.clip(CircleShape).background(DeviceGreen))
                    Spacer(Modifier.width(5.dp))
                    Text(pairedStatus(paired), color = DeviceGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            } else {
                Text(device.type + " · Tap to pair", color = colors.textGreyColor, fontSize = 12.sp)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(22.dp))
    }
}

private fun pairedStatus(device: PairedOmronDevice): String =
    if (device.lastSyncAt == 0L) "Paired · User ${device.user}"
    else "User ${device.user} · synced ${syncedAgo(device.lastSyncAt)}"

/* ================= DIALOG ================= */

@Composable
private fun AddVitalDialog(
    vitalName: String,
    unitName: String,
    viewModel: VitalDetailViewModel,
    onDismiss: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val haptic = rememberHaptics()
    val focusManager = LocalFocusManager.current
    val addLoading by viewModel.addLoading.collectAsState()
    val addError by viewModel.addError.collectAsState()

    val spec = remember(vitalName, unitName) { specFor(vitalName, unitName.trim()) }
    val prefs = remember { runCatching { context.getSharedPreferences(UNIT_PREFS, android.content.Context.MODE_PRIVATE) }.getOrNull() }
    // Start in the patient's last unit, else the one the backend uses.
    var modeId by remember(spec) {
        val saved = runCatching { prefs?.getString(vitalName, null) }.getOrNull()
        val backendMatch = spec?.modes?.firstOrNull { m -> m.id.isNotEmpty() && m.fields.size == 1 && sameUnit(m.fields.first().unit, spec.backendUnit) }?.id
        mutableStateOf(spec?.modes?.firstOrNull { it.id == saved }?.id ?: backendMatch ?: spec?.modes?.firstOrNull()?.id.orEmpty())
    }
    val mode = spec?.modes?.firstOrNull { it.id == modeId } ?: spec?.modes?.firstOrNull()
    val fields = mode?.fields
    val values = remember { mutableStateMapOf<String, String>() }
    val touched = remember { mutableStateMapOf<String, Boolean>() }
    var attempted by remember { mutableStateOf(false) }

    val isBP = vitalName.equals("Blood Pressure", ignoreCase = true)
    val title = if (isBP) "Enter BP Value" else "Enter $vitalName Value"

    fun text(key: String) = values[key].orEmpty()
    fun errorFor(field: VitalField): String? {
        fieldError(field, text(field.key))?.let { return it }
        // Systolic is always the higher number.
        if (field.key == "dia") {
            val sys = text("sys").toDoubleOrNull()
            val dia = text("dia").toDoubleOrNull()
            if (sys != null && dia != null && dia >= sys) return "Diastolic (DIA) must be lower than systolic (SYS)"
        }
        return null
    }

    val requiredFilled = fields != null && fields.filter { it.required }.all { text(it.key).isNotBlank() }
    val allValid = fields != null && fields.all { errorFor(it) == null }

    /** The typed value(s) in the canonical unit, or null while incomplete or invalid. */
    fun canonical(m: UnitMode? = mode): Double? {
        if (m == null || isBP || !m.fields.all { errorFor(it) == null }) return null
        if (m.fields.filter { it.required }.any { text(it.key).isBlank() }) return null
        return m.toCanonical(m.fields.associate { it.key to (text(it.key).toDoubleOrNull() ?: 0.0) })
    }
    /** What will actually be sent, in the backend's unit, as text ("98.6"). */
    fun backendText(): String? = canonical()?.let { spec?.toBackend?.invoke(it) }?.let { formatValue(round1(it)) }

    fun switchMode(newId: String) {
        if (newId == modeId || spec == null) return
        haptic(HapticEvent.Selection)
        val value = canonical()
        val newMode = spec.modes.first { it.id == newId }
        values.clear()
        touched.clear()
        attempted = false
        // Carry the reading across: 37 °C becomes 98.6 °F.
        value?.let { values.putAll(newMode.fromCanonical(it)) }
        modeId = newId
        runCatching { prefs?.edit()?.putString(vitalName, newId)?.apply() }
        viewModel.clearAddError()
    }

    // Valid but outside the normal range: worth a second look before saving.
    val outOfRange = allValid && fields != null && run {
        if (isBP) {
            val sys = text("sys").toDouble(); val dia = text("dia").toDouble()
            bpStatus(sys, dia) != VitalStatus.Normal ||
                text("pulse").toDoubleOrNull()?.let { vitalStatus("pulse", it) != VitalStatus.Normal } == true
        } else {
            val v = canonical()?.let { spec?.toBackend?.invoke(it) } ?: return@run false
            vitalStatus(vitalName, v).let { it == VitalStatus.Borderline || it == VitalStatus.Abnormal }
        }
    }

    fun save() {
        attempted = true
        focusManager.clearFocus()
        if (fields == null || !allValid || (!isBP && backendText() == null)) {
            haptic(HapticEvent.Error)
            return
        }
        val onSuccess = {
            Toast.makeText(context, "$vitalName saved", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        val value = backendText().orEmpty()
        when (vitalName.trim().lowercase()) {
            "blood pressure" -> viewModel.addVital(
                vmValueBPSys = text("sys"), vmValueBPDias = text("dia"), vmValuePulse = text("pulse"), onSuccess = onSuccess,
            )
            "pulse" -> viewModel.addVital(vmValuePulse = value, onSuccess = onSuccess)
            "spo2" -> viewModel.addVital(vmValueSPO2 = value, onSuccess = onSuccess)
            "heart rate", "heartrate" -> viewModel.addVital(vmValueHeartRate = value, onSuccess = onSuccess)
            "respiratory rate", "resprate" -> viewModel.addVital(vmValueRespiratoryRate = value, onSuccess = onSuccess)
            "blood glucose", "rbs" -> viewModel.addVital(vmValueRbs = value, onSuccess = onSuccess)
            "temperature" -> viewModel.addVital(vmValueTemperature = value, onSuccess = onSuccess)
            "weight" -> viewModel.addVital(vmValueWeight = value, onSuccess = onSuccess)
            "height" -> viewModel.addVital(vmValueHeight = value, onSuccess = onSuccess)
        }
    }

    Dialog(
        onDismissRequest = { if (!addLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dashboardContainerColor)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    color = colors.textDarkColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .bounceClick(enabled = !addLoading) { onDismiss() },
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.textDarkColor, modifier = Modifier.size(18.dp))
                }
            }

            if (spec != null && spec.modes.size > 1) {
                Spacer(Modifier.height(14.dp))
                UnitSwitch(
                    options = spec.modes.map { it.id to it.chip },
                    selected = modeId,
                    enabled = !addLoading,
                    onSelect = ::switchMode,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (fields == null) {
                Text(
                    "$vitalName can't be added manually here yet.",
                    color = colors.textGreyColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            } else {
                fields.forEachIndexed { i, field ->
                    val showError = attempted || touched[field.key] == true
                    VitalInputRow(
                        field = field,
                        value = text(field.key),
                        error = errorFor(field).takeIf { showError && (text(field.key).isNotEmpty() || attempted) },
                        isLast = i == fields.lastIndex,
                        enabled = !addLoading,
                        onValueChange = {
                            values[field.key] = sanitize(it, field)
                            viewModel.clearAddError()
                        },
                        onBlur = { if (text(field.key).isNotEmpty()) touched[field.key] = true },
                        onDone = { save() },
                    )
                }
            }

            // Typed in another unit: show exactly what will be stored.
            val sent = backendText()
            val typedInBackendUnit = mode != null && mode.fields.size == 1 && spec != null && sameUnit(mode.fields.first().unit, spec.backendUnit)
            AnimatedVisibility(!isBP && sent != null && mode != null && mode.id.isNotEmpty() && !typedInBackendUnit) {
                Text(
                    "Will be saved as $sent ${displayUnit(spec?.backendUnit.orEmpty())}",
                    color = colors.textGreyColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }

            AnimatedVisibility(outOfRange && addError == null) {
                Text(
                    "This reading is outside the normal range. Please re-check it before saving.",
                    color = Color(0xFFB45309),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(10.dp),
                )
            }
            AnimatedVisibility(addError != null) {
                Text(
                    addError.orEmpty(),
                    color = Color(0xFFB91C1C),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Light outline until every required number is in, like the design; blue once it can be saved.
            val ready = requiredFilled && fields != null
            val bg by animateColorAsState(if (ready) colors.primaryBlueColor else Color.Transparent, label = "saveBg")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .then(if (ready) Modifier else Modifier.border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp)))
                    .bounceClick(enabled = ready && !addLoading) { save() },
            ) {
                if (addLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        "Save Vitals",
                        color = if (ready) Color.White else Color(0xFFB0B7C3),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Small segmented control: °F | °C, kg | lb, cm | ft · in. */
@Composable
private fun UnitSwitch(
    options: List<Pair<String, String>>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = LocalMyColorScheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(50))
            .background(colors.dashboardBackgroundColor)
            .padding(3.dp)
    ) {
        options.forEach { (id, label) ->
            val active = id == selected
            val bg by animateColorAsState(if (active) colors.primaryBlueColor else Color.Transparent, label = "unitBg")
            Text(
                label,
                color = if (active) Color.White else colors.textGreyColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .bounceClick(enabled = enabled) { onSelect(id) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun VitalInputRow(
    field: VitalField,
    value: String,
    error: String?,
    isLast: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val focusManager = LocalFocusManager.current
    var hadFocus by remember { mutableStateOf(false) }
    val errorColor = Color(0xFFDC2626)

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(field.label, color = colors.textDarkColor, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    if (!field.required) {
                        Spacer(Modifier.width(6.dp))
                        Text("optional", color = colors.textGreyColor, fontSize = 11.sp)
                    }
                }
                Text(field.unit, color = colors.textGreyColor, fontSize = 12.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (field.decimals > 0) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                    onDone = { onDone() },
                ),
                textStyle = TextStyle(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (error != null) errorColor else colors.textDarkColor,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(colors.primaryBlueColor),
                modifier = Modifier
                    .width(150.dp)
                    .onFocusChanged {
                        if (it.isFocused) hadFocus = true
                        else if (hadFocus) onBlur()
                    },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text("00", color = Color(0xFFE5E9F2), fontSize = 44.sp, fontWeight = FontWeight.Medium)
                        }
                        inner()
                    }
                },
            )
        }
        if (error != null) {
            Text(error, color = errorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (!isLast) HorizontalDivider(Modifier.padding(top = 6.dp), color = Color(0xFFF1F3F6))
    }
}
