package com.example.antLabs

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.example.antLabs.routes.AppRouteGraph
import com.example.antLabs.ui.theme.AntLabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntLabsTheme {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                )
                val navController = rememberNavController()
                AppRouteGraph(navController = navController)
            }
        }
        Log.d("DBG", "Package name at runtime: ${packageName}")

    }
}
