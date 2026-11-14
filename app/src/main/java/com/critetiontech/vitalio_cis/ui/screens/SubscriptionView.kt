package com.critetiontech.ctvitalio.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.SubscriptionDetails
import com.critetiontech.ctvitalio.viewmodel.SubscriptionFeature
import com.critetiontech.ctvitalio.viewmodel.SubscriptionUiState
import com.critetiontech.ctvitalio.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.util.Locale

// ─── Design tokens (from the My Subscription mockup) ──────────────────────────
private val SubBlueTop = Color(0xFF1E5FE0)
private val SubBlueBottom = Color(0xFF1544B8)
private val SubButtonBlue = Color(0xFF2563EB)
private val SubGreen = Color(0xFF16A34A)
private val SubGreenPill = Color(0xFF22C55E)
private val SubRed = Color(0xFFDC2626)
private val SubAmber = Color(0xFFB7791F)
private val SubAmberIconBg = Color(0xFFB7791F)
private val SubAmberBg = Color(0xFFFFF8E6)
private val SubAmberBorder = Color(0xFFF3D9A4)

private val SubTextDark: Color @Composable get() = LocalMyColorScheme.current.textDarkColor
private val SubTextMid: Color @Composable get() = LocalMyColorScheme.current.textGreyColor
private val SubCard: Color @Composable get() = LocalMyColorScheme.current.dashboardContainerColor
private val SubDivider: Color
    @Composable get() = if (isAppDark) LocalMyColorScheme.current.borderGreyLightColor else Color(0xFFEEF0F3)

private fun formatRupees(amount: Double?): String? {
    if (amount == null) return null
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = 0
    }
    return "₹" + nf.format(amount)
}

@Composable
fun SubscriptionScreen(vm: SubscriptionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    CommonAppBar(title = stringResource(R.string.sub_title)) {
        Box(Modifier.fillMaxSize().background(LocalMyColorScheme.current.dashboardBackgroundColor)) {
            when (val s = state) {
                SubscriptionUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SubButtonBlue, strokeWidth = 3.dp)
                }
                SubscriptionUiState.Error -> SubCenteredMessage(
                    icon = Icons.Default.ErrorOutline,
                    tint = SubRed,
                    title = stringResource(R.string.sub_load_failed),
                    body = null,
                    onRetry = vm::load,
                )
                SubscriptionUiState.None -> SubCenteredMessage(
                    icon = Icons.Rounded.WorkspacePremium,
                    tint = SubButtonBlue,
                    title = stringResource(R.string.sub_none_title),
                    body = stringResource(R.string.sub_none_body),
                    onRetry = vm::load,
                )
                is SubscriptionUiState.Loaded -> com.critetiontech.ctvitalio.ui.feedback.StretchPullRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) { SubscriptionContent(s.plan) }
            }
        }
    }
}

@Composable
private fun SubscriptionContent(plan: SubscriptionDetails) {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.sub_coming_soon)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CurrentPlanCard(plan)
        if (plan.renewsInDays != null || !plan.nextRenewalDate.isNullOrBlank()) RenewalBanner(plan)
        PlanDetailsCard(plan)
        val features = plan.parsedFeatures
        if (features.isNotEmpty()) FeaturesCard(plan.planTier, features)

        // No upgrade / payment-history APIs yet — the actions are shown as in the design and say so.
        HapticButton(
            onClick = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = SubButtonBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.sub_upgrade_plan), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        Text(
            stringResource(R.string.sub_view_payment_history),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = SubTextMid
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CurrentPlanCard(plan: SubscriptionDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(SubBlueTop, SubBlueBottom)))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.sub_current_plan),
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                fontSize = 11.sp,
                color = Color(0xFF1F2937)
            )
            Spacer(Modifier.weight(1f))
            plan.status?.takeIf { it.isNotBlank() }?.let { status ->
                Text(
                    status,
                    modifier = Modifier
                        .background(if (plan.isActive) SubGreenPill else Color(0xFF9CA3AF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        plan.planTier?.takeIf { it.isNotBlank() }?.let {
            Text(it.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        plan.planName?.takeIf { it.isNotBlank() }?.let {
            Text(it.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.2f)
        )
        formatRupees(plan.pricePerMonth)?.let { price ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) { append(price) }
                    append("  ")
                    withStyle(SpanStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))) {
                        append(stringResource(R.string.sub_per_patient_month))
                    }
                },
                color = Color.White
            )
        }
    }
}

@Composable
private fun RenewalBanner(plan: SubscriptionDetails) {
    val days = plan.renewsInDays
    val bg = softTint(SubAmber, SubAmberBg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, if (isAppDark) SubAmber.copy(alpha = 0.4f) else SubAmberBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .background(SubAmberIconBg, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                when {
                    days == null -> stringResource(R.string.sub_next_renewal_date)
                    days <= 0 -> stringResource(R.string.sub_renewal_due)
                    else -> pluralStringResource(R.plurals.sub_renews_in_days, days, days)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SubAmber
            )
            plan.nextRenewalDate?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.sub_next_renewal_on, it), fontSize = 12.sp, color = SubTextMid)
            }
        }
    }
}

@Composable
private fun PlanDetailsCard(plan: SubscriptionDetails) {
    val rows = buildList {
        plan.planStartDate?.takeIf { it.isNotBlank() }?.let { add(Triple(R.string.sub_plan_start_date, it, null)) }
        (plan.validityText?.takeIf { it.isNotBlank() } ?: plan.validityMonths?.toString())
            ?.let { add(Triple(R.string.sub_validity, it, null)) }
        plan.nextRenewalDate?.takeIf { it.isNotBlank() }?.let { add(Triple(R.string.sub_next_renewal_date, it, null)) }
        plan.status?.takeIf { it.isNotBlank() }?.let {
            add(Triple(R.string.sub_status, it, if (plan.isActive) SubGreen else SubRed))
        }
    }
    if (rows.isEmpty()) return

    SubWhiteCard {
        Text(stringResource(R.string.sub_plan_details), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SubTextDark)
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { i, (label, value, valueColor) ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = SubDivider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(label), fontSize = 13.sp, color = SubTextMid, modifier = Modifier.weight(1f))
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = if (valueColor == null) FontWeight.SemiBold else FontWeight.Medium,
                    color = valueColor ?: SubTextDark
                )
            }
        }
    }
}

@Composable
private fun FeaturesCard(tier: String?, features: List<SubscriptionFeature>) {
    SubWhiteCard {
        Text(
            stringResource(R.string.sub_included_features, tier?.takeIf { it.isNotBlank() }?.uppercase().orEmpty()).trim(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SubTextDark
        )
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp), thickness = 1.dp, color = SubDivider)
        features.forEach { FeatureRow(it) }
    }
}

@Composable
private fun FeatureRow(feature: SubscriptionFeature) {
    val details = buildList {
        if (feature.isFull) add(stringResource(R.string.sub_feature_full))
        else feature.usageLimit?.takeIf { it > 0 }?.let { add(stringResource(R.string.sub_feature_limit, it)) }
        feature.availability?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = SubGreen, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            buildAnnotatedString {
                append(feature.featureName.orEmpty())
                if (details.isNotEmpty()) {
                    withStyle(SpanStyle(color = SubTextMid)) { append(" (${details.joinToString(" · ")})") }
                }
            },
            fontSize = 13.sp,
            color = SubTextDark
        )
    }
}

@Composable
private fun SubWhiteCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SubCard)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
private fun SubCenteredMessage(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String?,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SubTextDark, textAlign = TextAlign.Center)
            body?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, fontSize = 13.sp, color = SubTextMid, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            HapticButton(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SubButtonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sub_retry), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
