package com.critetiontech.ctvitalio.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.config.AppFeature
import com.critetiontech.ctvitalio.config.FeatureAdmin
import com.critetiontech.ctvitalio.config.rememberFeatureFlags
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.viewmodel.FeatureFlagsAdminViewModel

/** Admin-only page: turn each main module on or off for every user of the app. */
@Composable
fun FeatureFlagsAdminScreen(viewModel: FeatureFlagsAdminViewModel = viewModel()) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    // Guard the route itself too, not just the drawer entry that leads here
    if (!FeatureAdmin.isAdmin(PrefsManager(context).getPatient())) {
        CommonAppBar(title = stringResource(R.string.ff_title)) {
            FeatureDisabledContent(message = stringResource(R.string.ff_admin_only))
        }
        return
    }

    val saved by viewModel.saved.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val live = rememberFeatureFlags()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, context.getString(R.string.ff_save_failed, it), Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    CommonAppBar(title = stringResource(R.string.ff_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryBlueColor.copy(alpha = 0.10f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.ff_info), color = colors.textDarkColor, fontSize = 13.sp)
            }

            loadError?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.ff_load_failed, it),
                    color = com.critetiontech.ctvitalio.ui.components.DialogDangerColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primaryBlueColor)
                }
                return@Column
            }

            AppFeature.entries.groupBy { it.group }.forEach { (group, features) ->
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(group),
                    color = colors.textGreyColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                DrawerCard {
                    features.forEachIndexed { index, feature ->
                        if (index > 0) DrawerDivider()
                        val liveOn = live[feature] ?: true
                        // The saved value is what the admin chose; until it's saved, show what's live
                        val checked = saved?.get(feature) ?: liveOn
                        val subtitle = when {
                            feature in saving -> stringResource(R.string.ff_status_saving)
                            checked != liveOn -> stringResource(R.string.ff_status_publishing)
                            checked -> stringResource(R.string.ff_status_on)
                            else -> stringResource(R.string.ff_status_off)
                        }
                        SettingSwitchRow(
                            icon = featureIcon(feature),
                            title = stringResource(feature.label),
                            subtitle = subtitle,
                            checked = checked,
                            onCheckedChange = { viewModel.setEnabled(feature, it) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Shows [content] while [feature] is on; otherwise a "not available" page. Wraps the module's
 * routes so a deep link, voice command or back stack entry can't reach a switched-off module.
 */
@Composable
fun FeatureGate(feature: AppFeature, content: @Composable () -> Unit) {
    if (rememberFeatureFlags()[feature] != false) {
        content()
    } else {
        CommonAppBar(title = stringResource(feature.label)) { FeatureDisabledContent() }
    }
}

/** Shown in place of a module that's been switched off. */
@Composable
fun FeatureDisabledContent(message: String = stringResource(R.string.ff_disabled_message)) {
    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = colors.textGreyColor, fontSize = 15.sp)
        }
    }
}

private fun featureIcon(feature: AppFeature): ImageVector = when (feature) {
    AppFeature.VITALS -> Icons.Rounded.MonitorHeart
    AppFeature.FLUID -> Icons.Rounded.WaterDrop
    AppFeature.SYMPTOMS -> Icons.Rounded.Sick
    AppFeature.MEDICINE -> Icons.Rounded.Medication
    AppFeature.DIET -> Icons.Rounded.Restaurant
    AppFeature.INTERACTION_CHECKER -> Icons.Rounded.Vaccines
    AppFeature.APPOINTMENTS -> Icons.Rounded.CalendarMonth
    AppFeature.LAB_REPORTS -> Icons.Rounded.Science
    AppFeature.PRESCRIPTIONS -> Icons.Rounded.Description
    AppFeature.VOICE_ASSISTANT -> Icons.Rounded.Mic
    AppFeature.ACTIVITY -> Icons.Rounded.DirectionsRun
    AppFeature.SMART_RING -> Icons.Rounded.Watch
    AppFeature.ALLERGIES -> Icons.Rounded.Healing
    AppFeature.EMERGENCY_CONTACTS -> Icons.Rounded.ContactPhone
    AppFeature.FAMILY_HEALTH_HISTORY -> Icons.Rounded.FamilyRestroom
    AppFeature.SHARE_HEALTH_RECORDS -> Icons.Rounded.Share
    AppFeature.FAQ -> Icons.AutoMirrored.Rounded.HelpOutline
    AppFeature.FEEDBACK -> Icons.Rounded.RateReview
}
