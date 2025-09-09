package com.example.antLabs.engine

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.antLabs.ui.theme.TextPrimary

@Composable
fun GnssPermHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Ensure we have a proper Activity context
    val activity = context as? Activity

    Log.d("DBG", "Activity context: $activity")

    if (activity == null) {
        // If not attached to an Activity, show a friendly message and exit
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Cannot request location permission because the composable is not attached to an Activity.",
                color = TextPrimary
            )
        }
        return
    }

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        showRationale = !granted && activity.shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (hasPermission) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (showRationale)
                    "Location permission was denied. Please enable it in settings."
                else
                    "Location permission is required to show GNSS satellite data.",
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (showRationale) {
                Button(onClick = {
                    // Open app settings
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    activity.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            } else {
                Button(onClick = {
                    launcher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Request Permission")
                }
            }
        }
    }
}
