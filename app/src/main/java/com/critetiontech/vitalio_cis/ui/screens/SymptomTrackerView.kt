package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.domain.model.SymptomToReview
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.viewmodel.SymptomQuestionViewModel
import com.critetiontech.myapplication.utils.LocalNavController

/** Opens the "add symptoms" tracker in place of this walkthrough. */
private fun androidx.navigation.NavController.openSymptomSelection() =
    navigate(Routes.SYMPTOMS) { popUpTo(Routes.SYMPTOMSTRACKER) { inclusive = true } }

/** Step 1 of the symptom tracker: "Do you still have …?" for each recorded symptom. */
@Composable
fun SymptomTrackerScreen(viewModel: SymptomQuestionViewModel = viewModel()) {

    val navController = LocalNavController.current
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val state by viewModel.state.collectAsState()

    // Every answer saved → on to adding any other symptoms.
    LaunchedEffect(Unit) { viewModel.completed.collect { navController.openSymptomSelection() } }
    LaunchedEffect(Unit) { viewModel.message.collect { showToast(context, it) } }

    CommonAppBar(title = stringResource(R.string.track_symptom_tracker)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .navigationBarsPadding()
        ) {
            val current = state.current
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        CircularProgressIndicator(color = colors.primaryBlueColor)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.track_loading_symptoms), color = colors.textGreyColor, fontSize = 14.sp)
                    }
                    state.error != null -> QuestionStateMessage(
                        icon = Icons.Rounded.WarningAmber,
                        iconTint = Color(0xFFF59E0B),
                        title = stringResource(R.string.track_unable_load_symptoms),
                        text = state.error!!,
                        actionText = stringResource(R.string.track_try_again),
                        onAction = viewModel::load,
                    )
                    current == null -> QuestionStateMessage(
                        icon = Icons.Rounded.MedicalServices,
                        iconTint = colors.textGreyColor,
                        title = stringResource(R.string.track_no_symptoms_recorded),
                        text = stringResource(R.string.track_no_symptoms_to_review),
                        actionText = stringResource(R.string.track_add_symptoms),
                        onAction = { navController.openSymptomSelection() },
                    )
                    else -> AnimatedContent(
                        targetState = current,
                        transitionSpec = { (fadeIn() + slideInVertically { it / 6 }) togetherWith fadeOut() },
                        label = "symptomQuestion",
                    ) { symptom -> QuestionContent(symptom) }
                }
            }

            if (!state.loading && current != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                ) {
                    AnswerButton(
                        text = stringResource(R.string.track_yes),
                        textColor = colors.textDarkColor,
                        background = Color.Gray.copy(alpha = 0.2f),
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.answer(stillFeeling = true) }
                    AnswerButton(
                        text = stringResource(R.string.track_no),
                        textColor = colors.primaryBlueColor,
                        background = colors.primaryBlueColor.copy(alpha = if (isAppDark) 0.25f else 0.15f),
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.answer(stillFeeling = false) }
                }
            }

            Box(Modifier.fillMaxWidth().height(38.dp).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
                } else {
                    DotsIndicator(total = state.questions.size, current = state.currentIndex)
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(symptom: SymptomToReview) {
    val colors = LocalMyColorScheme.current
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.symptom_tacker),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(40.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 10.dp),
        ) {
            Text(stringResource(R.string.track_do_you_still_have), color = colors.textDarkColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.track_symptom_question, symptom.details.trim()),
                color = colors.primaryBlueColor,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AnswerButton(
    text: String,
    textColor: Color,
    background: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .bounceClick(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(text, color = textColor.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Active step is a short pill, the rest are dots. */
@Composable
private fun DotsIndicator(total: Int, current: Int) {
    val colors = LocalMyColorScheme.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 18.dp else 6.dp, label = "dotWidth")
            Box(
                Modifier
                    .size(width = width, height = 6.dp)
                    .clip(if (active) RoundedCornerShape(6.dp) else CircleShape)
                    .background(if (active) colors.primaryBlueColor else Color.Gray.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun QuestionStateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    text: String,
    actionText: String,
    onAction: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(50.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, color = colors.textGreyColor, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(min = 200.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primaryBlueColor)
                .bounceClick(onClick = onAction)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(actionText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
