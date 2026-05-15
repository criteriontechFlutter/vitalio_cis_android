package com.example.vitalio_cis.ui.screens.onboarding



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
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard

/// ======================================
/// PROFILE ITEM
/// ======================================

data class ProfileItem(
    val title: String,
    val value: String
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
    data: UserProfileData
): List<ProfileItem> {

    return listOf(

        ProfileItem(
            title = "Name",
            value = data.name ?: "Add Name"
        ),

        ProfileItem(
            title = "Gender",
            value = data.gender ?: "Add Gender"
        ),

        ProfileItem(
            title = "Date of Birth",
            value = data.dob ?: "Add Date of Birth"
        ),

        ProfileItem(
            title = "Blood Group",
            value = data.bloodGroup ?: "Add Blood Group"
        ),

        ProfileItem(
            title = "Address",
            value = data.address ?: "Add Address"
        ),

        ProfileItem(
            title = "Weight",
            value = data.weight ?: "Add Weight"
        ),

        ProfileItem(
            title = "Height",
            value = data.height ?: "Add Height"
        ),

        ProfileItem(
            title = "Chronic Conditions",
            value = data.chronicCondition ?: "Add Condition"
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
    /// DYNAMIC LIST

    val profileList = getProfilePreviewList(userData)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
            .padding(16.dp)
    ) {

        /// TITLE

        Text(
            text = "Profile Preview",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = "All Done!",
            subtitle = "You're just one step away from completing the process."
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

        Button(
            onClick = {
                navController.navigate("thankYou")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {
            Text("Next", color = Color.White)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

            },

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                        Color(0xFFEAF1FF),
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
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.value,

                    color = if (item.value.startsWith("Add"))
                        Color.Red
                    else
                        Color.Black,

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
                        Color(0xFFF4F4F4),
                        CircleShape
                    )
                    .clickable {

                    },

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}