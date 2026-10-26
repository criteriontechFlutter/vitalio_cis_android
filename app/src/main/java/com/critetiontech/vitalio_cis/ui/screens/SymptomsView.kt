package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.Problem
import com.critetiontech.ctvitalio.model.SymptomItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.viewmodel.SymptomSelectionViewModel
import com.critetiontech.myapplication.utils.LocalNavController

/** Step 2 of the symptom tracker: pick any other symptoms from the grid or by searching. */
@Composable
fun SymptomsView(viewModel: SymptomSelectionViewModel = viewModel()) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.message.collect { showToast(context, it) } }

    CommonAppBar(
        title = "Symptoms Tracker",
        actions = {
            Text(
                "History",
                color = colors.primaryBlueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.dashboardContainerColor)
                    .bounceClick { navController.navigate(Routes.SYMPTOMSHISTORY) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .navigationBarsPadding()
        ) {
            SymptomSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            )

            // The suggestions float over the list instead of pushing it down.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "Highlight any other symptoms you're experiencing from the list below.",
                        color = colors.textGreyColor,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                    when {
                        state.iconsLoading && state.icons.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primaryBlueColor)
                        }
                        state.iconsError -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        ) {
                            Text("Couldn't load the symptoms list.", color = colors.textGreyColor, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Try again",
                                color = colors.primaryBlueColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = viewModel::loadIcons).padding(8.dp),
                            )
                        }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.icons, key = { it.problemId }) { problem ->
                                SymptomCard(
                                    name = problem.problemName,
                                    iconUrl = problem.displayIcon,
                                    selected = state.isSelected(problem.problemId),
                                ) {
                                    haptic(HapticEvent.Selection)
                                    viewModel.toggle(problem)
                                }
                            }
                        }
                    }
                }

                if (state.query.isNotBlank()) {
                    SuggestionsDropdown(
                        searching = state.searching,
                        suggestions = state.suggestions,
                        isSelected = state::isSelected,
                        onPick = { haptic(HapticEvent.Selection); viewModel.pickSuggestion(it) },
                        modifier = Modifier.zIndex(10f).padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            if (state.selected.isNotEmpty()) {
                SelectedSymptoms(state.selected, onRemove = viewModel::remove)
            }

            CommonButton(
                text = "Save and Update Symptoms",
                isLoading = state.saving,
                enabled = state.selected.isNotEmpty(),
                cornerRadius = 14.dp,
                containerColor = colors.primaryBlueColor,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
                    .height(54.dp),
                onClick = viewModel::save,
            )
        }
    }
}

@Composable
private fun SymptomSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .border(1.dp, colors.textDarkColor.copy(alpha = 0.08f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Search Symptoms i.e cold, cough", color = colors.textGreyColor, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textDarkColor, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.primaryBlueColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = colors.textGreyColor,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun SuggestionsDropdown(
    searching: Boolean,
    suggestions: List<Problem>,
    isSelected: (Int) -> Boolean,
    onPick: (Problem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .border(1.dp, colors.textDarkColor.copy(alpha = 0.08f), shape)
    ) {
        when {
            searching && suggestions.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
                Spacer(Modifier.width(10.dp))
                Text("Searching…", color = colors.textGreyColor, fontSize = 14.sp)
            }
            suggestions.isEmpty() -> Text("No matching symptoms", color = colors.textGreyColor, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
            else -> LazyColumn(Modifier.heightIn(max = 280.dp)) {
                items(suggestions, key = { it.problemId }) { problem ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(problem) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(problem.problemName, color = colors.textDarkColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (isSelected(problem.problemId)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = colors.textDarkColor.copy(alpha = 0.06f))
                }
            }
        }
    }
}

@Composable
private fun SymptomCard(name: String, iconUrl: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val background by animateColorAsState(
        if (selected) colors.primaryBlueColor else Color.Gray.copy(alpha = 0.1f),
        label = "symptomCardBg",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .bounceClick(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 10.dp),
    ) {
        AsyncImage(
            model = iconUrl?.takeIf { it.isNotBlank() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(33.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = if (selected) Color.White else colors.textGreyColor,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SelectedSymptoms(selected: List<SymptomItem>, onRemove: (SymptomItem) -> Unit) {
    val colors = LocalMyColorScheme.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Selected Symptoms",
                color = colors.textDarkColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${selected.size}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.primaryBlueColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(selected, key = { it.detailId }) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.dashboardContainerColor)
                        .border(1.dp, colors.primaryBlueColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(item.details, color = colors.textDarkColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove ${item.details}",
                        tint = colors.textGreyColor,
                        modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onRemove(item) }.padding(5.dp),
                    )
                }
            }
        }
    }
}
