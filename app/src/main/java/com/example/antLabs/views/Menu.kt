package com.example.antLabs.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.antLabs.routes.Routes
import com.example.antLabs.ui.theme.TextPrimary

@Composable
fun Menu(navController: NavController) {
    val menuItems = listOf(
        "Gyroscope" to Routes.GYRO,
        "Magnetometer" to Routes.MAGNETO,
        "Accelerometer" to Routes.ACCEL,
        "GNSS (Experimental)" to Routes.GNSS_ROUTE,
        "Earthquake (Experimental)" to Routes.EARTH
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .statusBarsPadding()
            .background(Color.Transparent)
    ) {
        menuItems.forEachIndexed { index, item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(item.second) },
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = item.first,
                    color = TextPrimary,
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                )
            }

            if (index < menuItems.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }
    }
}
