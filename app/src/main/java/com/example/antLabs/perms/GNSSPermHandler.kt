package com.example.antLabs.perms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.antLabs.ui.theme.TextPrimary

/**
 * GNSSPermHandler is a composable that handles location permission requests for displaying GNSS data.
 * If the permission is granted, it shows the provided content. If not, it shows a UI asking the user to grant it.
 */
@Composable
fun GNSSPermHandler(content: @Composable () -> Unit) {
    // Get the current Compose context
    val context = LocalContext.current

    // Ensure the context is actually an Activity, because permission requests require one
    val activity = context as? Activity

    Log.d("DBG", "Activity context: $activity") // Debug log to check the activity

    if (activity == null) {
        // If the composable is not attached to an Activity, show a friendly message and exit
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

    // State to track if location permission is already granted
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // State to determine whether to show rationale (explanation for why permission is needed)
    var showRationale by remember { mutableStateOf(false) }

    // Launcher to request the location permission from the user
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted // Update permission state
        // If not granted, check if we should show rationale
        showRationale = !granted && activity.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    if (hasPermission) {
        // Permission is granted → show the actual content passed to this composable
        content()
    } else {
        // Permission not granted → show UI asking the user to grant it
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Display rationale or default message
            Text(
                if (showRationale)
                    "Location permission was denied. Please enable it in settings."
                else
                    "Location permission is required to show GNSS satellite data.",
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (showRationale) {
                // If user denied before, give a button to open app settings
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    activity.startActivity(intent) // Open app settings
                }) {
                    Text("Open Settings")
                }
            } else {
                // First-time request → show button to request permission
                Button(onClick = {
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Request Permission")
                }
            }
        }
    }
}
