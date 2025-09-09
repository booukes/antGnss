package com.example.antLabs.views

import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.antLabs.ui.theme.TextPrimary

data class SatelliteInfo(
    val svid: Int,
    val constellation: Int,
    val cn0: Float,
    val fix: Boolean,
    val elevation: Float,
    val azimuth: Float
)

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun GNSS() {
    val context = LocalContext.current
    val locationManager = context.getSystemService(LocationManager::class.java)

    var satellites by remember { mutableStateOf(listOf<SatelliteInfo>()) }

    DisposableEffect(Unit) {
        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val list = mutableListOf<SatelliteInfo>()
                for (i in 0 until status.satelliteCount) {
                    list.add(
                        SatelliteInfo(
                            svid = status.getSvid(i),
                            constellation = status.getConstellationType(i),
                            cn0 = status.getCn0DbHz(i),
                            elevation = status.getElevationDegrees(i),
                            azimuth = status.getAzimuthDegrees(i),
                            fix = status.usedInFix(i)
                        )
                    )
                }
                satellites = list
                Log.d("GNSS", "Satellite count: ${status.satelliteCount}")
            }
        }

        val locationListener = LocationListener { /* do nothing, just wake GPS */ }

        // Register both
        locationManager.registerGnssStatusCallback(gnssCallback)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            100L, // 1 second interval
            0f,
            locationListener
        )
        onDispose {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "GNSS Satellites",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Total satellites: ${satellites.size}",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(satellites) { sat ->
                val qualityColor = when {
                    sat.cn0 >= 40 -> Color(0xFF4CAF50) // Excellent - green
                    sat.cn0 >= 30 -> Color(0xFFFFC107) // Good - amber
                    sat.cn0 >= 20 -> Color(0xFFFF5722) // Weak - orange
                    else -> Color(0xFFF44336) // Very Weak - red
                }
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            Text("SVID: ${sat.svid}", color = TextPrimary)
                            Text("Constellation: ${constellationName(sat.constellation)}", color = TextPrimary)
                            Text(
                                "Signal CN0: ${"%.1f".format(sat.cn0)} dB-Hz",
                                color = qualityColor,
                                fontSize = 16.sp
                            )
                            Text("Elevation: ${"%.1f".format(sat.elevation)}°", color = TextPrimary)
                            Text("Azimuth: ${"%.1f".format(sat.azimuth)}°", color = TextPrimary)
                            Text("Used in fix?: ${if (sat.fix) "Yes" else "No"}", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
fun constellationName(constellation: Int): String {
    return when(constellation) {
        1 -> "GPS"
        2 -> "SBAS"
        3 -> "GLONASS"
        4 -> "QZSS"
        5 -> "BeiDou"
        6 -> "Galileo"
        7 -> "IRNSS"
        else -> "Unknown"
    }
}
