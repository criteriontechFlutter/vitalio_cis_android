package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.SymptomHistoryViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Mon, 09 Feb 26", as on iOS. */
private val DayHeaderFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yy", Locale.ENGLISH)

/** Every symptom the patient has recorded, grouped by day. */
@Composable
fun SymptomHistoryScreen(viewModel: SymptomHistoryViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val state by viewModel.state.collectAsState()

    CommonAppBar(title = stringResource(R.string.track_symptoms_history)) {
        Box(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
            when {
                state.loading && state.days.isEmpty() ->
                    CircularProgressIndicator(color = colors.primaryBlueColor, modifier = Modifier.align(Alignment.Center))
                state.error != null && state.days.isEmpty() -> HistoryMessage(
                    title = stringResource(R.string.track_unable_load_history),
                    text = state.error!!,
                    actionText = stringResource(R.string.track_try_again),
                    onAction = viewModel::load,
                )
                state.days.isEmpty() -> HistoryMessage(
                    title = stringResource(R.string.track_no_symptoms_recorded),
                    text = stringResource(R.string.track_symptoms_history_empty),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.days.forEach { day ->
                        item(key = "header-${day.date}") {
                            Text(
                                day.date?.format(DayHeaderFormat) ?: stringResource(R.string.track_unknown_date),
                                color = colors.textDarkColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        // The server can repeat a row, so keys can't come from its ids alone.
                        itemsIndexed(day.symptoms, key = { i, _ -> "${day.date}-$i" }) { _, symptom ->
                            Text(
                                symptom.details.trim(),
                                color = colors.textDarkColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.dashboardContainerColor)
                                    .padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMessage(title: String, text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Icon(Icons.Rounded.History, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, color = colors.textGreyColor, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                actionText,
                color = colors.primaryBlueColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(10.dp),
            )
        }
    }
}
