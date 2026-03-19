package com.critetiontech.ctvitalio.ui.screens

 import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
 import com.critetiontech.ctvitalio.viewmodel.DrawerViewModel
import com.critetiontech.ctvitalio.viewmodel.LoginViewModel
import com.example.myapplication.utils.LocalNavController
 import com.example.vitalio_cis.utils.PrefsManager

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DrawerView(
    drawerViewModel: DrawerViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel(),
 ) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val navController = LocalNavController.current

    // Observe loading, success, error
    val loading by drawerViewModel.loading
    val updateSuccess by drawerViewModel.updateSuccess
    val errorMessage by drawerViewModel.errorMessage
    val selectedImageUri by drawerViewModel.selectedImageUri
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color.Cyan)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 20.dp, start = 5.dp, end = 5.dp, bottom = 15.dp)
        ) {

            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(15.dp)
                ) {
                    // Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                    }

                    // Profile Image with Edit Overlay
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clickable { /* Show Image Picker BottomSheet */ },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val imagePainter = if (selectedImageUri != null) {
                            rememberAsyncImagePainter(selectedImageUri)
                        } else {

                        }




                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Name and Phone
                    val patient = PrefsManager(context).getPatient()
                    Text(
                        text = patient?.patientName ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = patient?.uhID ?: "",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Edit Profile Button
                    Button(onClick = { navController.navigate("editProfile") }) {
                        Text("Edit Profile", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sections (Personal Info, Allergies, etc.)
            ProfileSection(
                items = listOf(
                    ProfileItem("Personal Info") { navController.navigate("editProfile") },
                    ProfileItem("Allergies") { navController.navigate("allergies") }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileSection(
                items = listOf(
                    ProfileItem("Shared Account") { /* Navigate */ },
                    ProfileItem("Connect Smart Watch") { /* Navigate */ },
                    ProfileItem("Emergency Contact") { /* Navigate */ }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileSection(
                items = listOf(
                    ProfileItem("Language") { /* Navigate */ },
                    ProfileItem("Dark Mode") { /* Navigate */ },
                    ProfileItem("FAQs") { /* Navigate */ },
                    ProfileItem("Feedback") { /* Navigate */ }
                )
            )
        }
    }

    // Loading indicator
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Show error or success messages
    if (updateSuccess) {
        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
    }
    if (errorMessage?.isNotEmpty() == true) {
//        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProfileSection(items: List<ProfileItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { item.onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.title, fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}

class ProfileItem(val title: String, val onClick: () -> Unit)