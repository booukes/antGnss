package com.example.antLabs.views

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.antLabs.routes.Routes
import com.example.antLabs.ui.theme.*

@Composable
fun Menu(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(DarkBackground)
    ) {
        Button(onClick = { navController.navigate(Routes.GYRO) }) {
            Text("Gyroscope")
        }

        Button(
            onClick = { navController.navigate(Routes.MAGNETO) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Magnetometer")
        }
        Button(
            onClick = { navController.navigate(Routes.GNSS_ROUTE) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("GNSS (Experimental)")
        }
    }

}
