package com.critetiontech.ctvitalio.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.HapticSwitch
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.DietBoxViewModel
import com.critetiontech.myapplication.utils.LocalNavController

/** My Diet Box: saved plans with an Active switch; opens a plan, or the builder for a new one. */
@Composable
fun DietBoxScreen(viewModel: DietBoxViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val nav = LocalNavController.current
    val plans by viewModel.plans.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        DietTopBar(title = stringResource(R.string.diet_my_box))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = colors.primaryBlueColor)
        if (plans.isEmpty() && !loading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DietEmptyState(stringResource(R.string.diet_no_plan_title), stringResource(R.string.diet_no_plan_msg))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(plans, key = { _, p -> p.id }) { i, plan ->
                    AnimatedEnter(index = i) {
                        PlanRow(
                            plan = plan,
                            active = plan.id == activeId,
                            createdAt = viewModel.createdAt(plan.id),
                            onToggle = { viewModel.setActive(plan.id, it) },
                            onOpen = { nav.navigate(Routes.dietPlanBuilder(plan.id)) },
                        )
                    }
                }
            }
        }
        DietPrimaryButton(
            stringResource(R.string.diet_create_plan),
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
        ) { nav.navigate(Routes.dietPlanBuilder()) }
    }
}

@Composable
private fun PlanRow(plan: DietPlan, active: Boolean, createdAt: Long?, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) softTint(colors.primaryBlueColor, DietColors.BlueSoft) else colors.dashboardContainerColor)
            .border(1.dp, if (active) DietColors.BlueBorder else colors.borderGreyLightColor.copy(alpha = 0.25f), shape)
            .bounceClick(pressedScale = 0.98f, onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.name, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.diet_active), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF22C55E)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            // GetDietPlan has no author or date; plans made on this device are known to be the user's.
            val meta = buildList {
                add(if (createdAt != null) stringResource(R.string.diet_created_by_self) else stringResource(R.string.diet_assigned))
                if (createdAt != null) add(DateUtils.getRelativeTimeSpanString(createdAt, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString())
                add(stringResource(R.string.diet_kcal_cap, kcalText(plan.avgCaloriesPerDay)) + "/day")
            }.joinToString("  |  ")
            Text(meta, color = colors.textGreyColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        HapticSwitch(
            checked = active,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.75f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF22C55E),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.borderGreyLightColor.copy(alpha = 0.4f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(22.dp))
    }
}
