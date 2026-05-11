package com.example.vitalio_cis.ui.screens



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme


data class Watch(
    val name: String,
    val battery: Int
)

@Composable
fun ConnectWatchScreen() {
    var watches by remember {
        mutableStateOf(
            listOf(
                Watch("Samsung Galaxy 7 Watch", 91),
                Watch("Apple Watch Series 10", 55)
            )
        )
    }

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Connect Watch",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {

            watches.forEach { watch ->
                WatchItem(
                    watch = watch,
                    onRemove = {
                        watches = watches - watch
                    }
                )
            }
        }
    }
}

@Composable
fun WatchItem(
    watch: Watch,
    onRemove: () -> Unit
) {

    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(watch.name, style = AppTextStyles.style14BCB())
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔋 ${watch.battery}%",
                    style = AppTextStyles.style12GCN())
            }

            TextButton(onClick = onRemove) {
                Text(
                    text = "Remove",
                    style = AppTextStyles.style14GCN().copy(color = Color.Red))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red
                )
            }
        }
    }
}