package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.critetiontech.ctvitalio.ui.feedback.HapticButton



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard

/// ======================================
/// PROFILE ITEM
/// ======================================

data class ProfileItem(
    val title: String,
    val value: String,
    /** True when [value] is the "Add …" prompt (nothing entered yet). */
    val isMissing: Boolean = false
)

/// ======================================
/// USER DATA MODEL
/// ======================================

data class UserProfileData(

    val name: String? = null,

    val gender: String? = null,

    val dob: String? = null,

    val bloodGroup: String? = null,

    val address: String? = null,

    val weight: String? = null,

    val height: String? = null,

    val chronicCondition: String? = null
)

/// ======================================
/// CONVERT DATA TO LIST
/// ======================================

fun getProfilePreviewList(
    data: UserProfileData,
    context: Context
): List<ProfileItem> {

    return listOf(

        ProfileItem(
            title = context.getString(R.string.auth_onb_profile_name),
            value = data.name ?: context.getString(R.string.auth_onb_profile_add_name),
            isMissing = data.name == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_gender_title),
            value = data.gender ?: context.getString(R.string.auth_onb_profile_add_gender),
            isMissing = data.gender == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_profile_dob),
            value = data.dob ?: context.getString(R.string.auth_onb_profile_add_dob),
            isMissing = data.dob == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_profile_blood_group),
            value = data.bloodGroup ?: context.getString(R.string.auth_onb_profile_add_blood_group),
            isMissing = data.bloodGroup == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_profile_address),
            value = data.address ?: context.getString(R.string.auth_onb_profile_add_address),
            isMissing = data.address == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_weight_label),
            value = data.weight ?: context.getString(R.string.auth_onb_profile_add_weight),
            isMissing = data.weight == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_height_label),
            value = data.height ?: context.getString(R.string.auth_onb_profile_add_height),
            isMissing = data.height == null
        ),

        ProfileItem(
            title = context.getString(R.string.auth_onb_profile_chronic),
            value = data.chronicCondition ?: context.getString(R.string.auth_onb_profile_add_condition),
            isMissing = data.chronicCondition == null
        )
    )
}

/// ======================================
/// MAIN SCREEN
/// ======================================

@Composable
fun CreateAccountPreviewScreen() {

    /// DATA FROM DIFFERENT PAGES

    val userData = UserProfileData(

        name = "Abhinav Sharma",

        gender = "Male",

        dob = "17 September 1987 (37 Years)",

        bloodGroup = null,

        address = null,

        weight = "56.3 Kg",

        height = null,

        chronicCondition = "Hypertension"
    )

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    /// DYNAMIC LIST

    val profileList = getProfilePreviewList(userData, context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAppDark) colors.dashboardBackgroundColor else Color(0xFFF8F9FD))
            .padding(16.dp)
    ) {

        /// TITLE

        Text(
            text = stringResource(R.string.auth_onb_profile_preview),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = stringResource(R.string.auth_onb_progress_all_done_title),
            subtitle = stringResource(R.string.auth_onb_progress_all_done_subtitle)
        )


        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(profileList) { item ->

                ProfileCard(
                    item = item
                )
            }
        }

        HapticButton(
            onClick = {
                navController.navigate("thankYou")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = softTint(colors.textGreyColor, Color(0xFFE5E7EB))
            )
        ) {
            Text(stringResource(R.string.auth_common_next), color = Color.White)
        }
    }
}

/// ======================================
/// PROFILE CARD
/// ======================================

@Composable
fun ProfileCard(
    item: ProfileItem
) {
    val colors = LocalMyColorScheme.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

            },

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = colors.dashboardContainerColor
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.Top
        ) {

            /// CHECK ICON

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        softTint(Color(0xFF2962FF), Color(0xFFEAF1FF)),
                        CircleShape
                    ),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF2962FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            /// CONTENT

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = item.title,
                    color = colors.textGreyColor,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.value,

                    color = if (item.isMissing)
                        Color.Red
                    else
                        colors.textDarkColor,

                    fontSize = 15.sp,

                    fontWeight = FontWeight.Medium,

                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            /// EDIT ICON

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        softTint(colors.textGreyColor, Color(0xFFF4F4F4)),
                        CircleShape
                    )
                    .clickable {

                    },

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = colors.textGreyColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}