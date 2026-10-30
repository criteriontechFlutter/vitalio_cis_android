package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.viewmodel.InteractionCheckerViewModel
import com.critetiontech.ctvitalio.viewmodel.InteractionInstructionItem
import com.critetiontech.ctvitalio.viewmodel.InteractionSeverity
import com.critetiontech.myapplication.utils.LocalNavController

@Composable
fun InteractionInstructionView(
    viewModel: InteractionCheckerViewModel
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val instructions by viewModel.displayInstructions.collectAsState()

    val title = if (instructions.isEmpty()) stringResource(R.string.med_instructions_title) else stringResource(R.string.med_instructions_found_count, instructions.size)

    CommonAppBar(
        title = title,
        onBack = { navController.popBackStack() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            if (instructions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = stringResource(R.string.med_cd_safe),
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.med_no_interactions_found),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textDarkColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.med_no_interactions_desc),
                        fontSize = 14.sp,
                        color = colors.textGreyColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        stringResource(R.string.med_interaction_warning),
                        fontSize = 12.sp,
                        color = colors.textGreyColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    instructions.forEach { item ->
                        InteractionInstructionCard(item = item)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        stringResource(R.string.med_interaction_warning),
                        fontSize = 12.sp,
                        color = colors.textGreyColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun InteractionInstructionCard(item: InteractionInstructionItem) {
    val colors = LocalMyColorScheme.current
    val isDarkMode = isAppDark

    val cardBg = if (item.severity == InteractionSeverity.Caution) 
        if (isDarkMode) Color.Yellow.copy(alpha = 0.12f) else Color(0xFFFEFCE8)
    else 
        if (isDarkMode) Color.Red.copy(alpha = 0.12f) else Color(0xFFFEF2F2)
        
    val badgeBg = if (item.severity == InteractionSeverity.Caution) 
        Color(0xFFF59E0B) 
    else 
        Color(0xFFEF4444)

    val strokeColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.02f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, strokeColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(item.severity.titleRes), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textDarkColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.drugPair, fontSize = 13.sp, color = colors.textGreyColor)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textGreyColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp).padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.med_substitute_medicine_label), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textDarkColor.copy(alpha = 0.85f))
            Text(item.substituteMedicine, fontSize = 13.sp, color = colors.textGreyColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.med_interaction_effect_label), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textDarkColor.copy(alpha = 0.85f))
            Text(item.interactionEffect, fontSize = 13.sp, color = colors.textGreyColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.med_suggestive_action_label), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textDarkColor.copy(alpha = 0.85f))
            Text(item.suggestiveAction, fontSize = 13.sp, color = colors.textGreyColor)
        }
    }
}
