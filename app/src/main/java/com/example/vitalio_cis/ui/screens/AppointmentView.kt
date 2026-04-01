package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.ui.theme.getColorScheme
import com.example.vitalio_cis.utils.CommonTextField

@Composable
fun FindDoctorsTopSection() {


    // Observe the selectedTheme StateFlow
    val themeViewModel: ThemeViewModel = viewModel()

    // 3️⃣ Collect colors as Compose State to trigger recomposition when theme changes
    val colors by themeViewModel.colorScheme.collectAsState()
    // Get the color scheme for the current theme
    Column(modifier = Modifier.fillMaxWidth().background(colors.white)
        .statusBarsPadding()      // Adds padding equal to status bar height
        .padding(16.dp)) {

        // Top Bar with Back button, Title, Date and Calendar Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(color =colors.primary )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp).clickable { /* TODO: Handle back */ }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Find Doctors",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "16.01.2025",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).clickable { /* TODO: Show date picker */ }
            )
        }
        Button(onClick = { themeViewModel.toggleTheme() }) {
            Text("Toggle Theme")
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clinic", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Switch Clinic >",
                color = Color(0xFF4A90E2),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    // TODO: Handle switch clinic action
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Clinic Row with Icon, Name, Address and "Switch Clinic" clickable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE9EDF3))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.intervention), // replace with actual clinic logo drawable
                contentDescription = "Clinic Logo",
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LifeSpring Medical",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Main Bazaar Road, Aluva, Kochi – 683101",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = " >",
                color = Color(0xFF4A90E2),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { /* TODO: Handle switch clinic */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(
            value =  "",
            onValueChange = {       },
            hint = "Sear"
        )

    }
}
