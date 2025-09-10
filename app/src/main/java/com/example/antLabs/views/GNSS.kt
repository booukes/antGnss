package com.example.antLabs.views

import android.Manifest
import android.annotation.SuppressLint
import android.location.*
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.antLabs.ui.theme.TextPrimary

data class SatelliteInfoExtended(
    val svid: Int,
    val constellation: Int,
    val cn0: Double,
    val fix: Boolean,
    val elevation: Float,
    val azimuth: Float,
    val doppler: Double,
    val relativeVelocity: Double,
    //val distance: Double
)

@SuppressLint("MissingPermission")
@Composable
fun GNSS() {
    val context = LocalContext.current
    val locationManager = context.getSystemService(LocationManager::class.java)
    var satellites by remember { mutableStateOf(listOf<SatelliteInfoExtended>()) }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Log.d("GNSS", "Permission denied")
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(Unit) {
        val gnssStatusCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val list = satellites.toMutableList()
                for (i in 0 until status.satelliteCount) {
                    val idx = list.indexOfFirst { it.svid == status.getSvid(i) }
                    val satellite = SatelliteInfoExtended(
                        svid = status.getSvid(i),
                        constellation = status.getConstellationType(i),
                        cn0 = status.getCn0DbHz(i).toDouble(),
                        elevation = status.getElevationDegrees(i),
                        azimuth = status.getAzimuthDegrees(i),
                        fix = status.usedInFix(i),
                        doppler = list.getOrNull(idx)?.doppler ?: 0.0,
                        relativeVelocity = list.getOrNull(idx)?.relativeVelocity ?: 0.0,
                        //distance = list.getOrNull(idx)?.distance ?: 0.0
                    )
                    if (idx >= 0) list[idx] = satellite else list.add(satellite)
                }
                satellites = list
            }
        }

        val gnssMeasurementsCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                object : GnssMeasurementsEvent.Callback() {
                    override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                        val list = satellites.toMutableList()
                        val c = 299_792_458.0 // speed of light in m/s

                        val fullBiasNanos = event.clock.fullBiasNanos
                        val timeNanos = event.clock.timeNanos

                        for (measurement in event.measurements) {
                            val idx = list.indexOfFirst { it.svid == measurement.svid }

                            /*val c = 299_792_458.0 // speed of light in m/s
                            val fullBiasNanos = event.clock.fullBiasNanos
                            val biasUncertaintyNanos = event.clock.biasUncertaintyNanos
                            val timeNanos = event.clock.timeNanos
                            val tRxSeconds = (timeNanos - fullBiasNanos) * 1e-9

                            var tTxSeconds = measurement.receivedSvTimeNanos * 1e-9

                            // Handle GPS week rollover
                            if (tRxSeconds - tTxSeconds < 0) {
                                tTxSeconds -= 604_800.0 // GPS week in seconds
                            }

                            // Pseudorange in meters
                            val pseudorangeMeters = (tRxSeconds - tTxSeconds) * c */


                            val doppler = measurement.pseudorangeRateMetersPerSecond
                            val velocity = measurement.pseudorangeRateUncertaintyMetersPerSecond

                            if (idx >= 0) {
                                val sat = list[idx]
                                list[idx] = sat.copy(
                                    doppler = doppler,
                                    relativeVelocity = velocity,
                                    //distance = pseudorangeMeters
                                )
                            } else {
                                list.add(
                                    SatelliteInfoExtended(
                                        svid = measurement.svid,
                                        constellation = measurement.constellationType,
                                        cn0 = measurement.cn0DbHz,
                                        fix = false,
                                        elevation = 0f,
                                        azimuth = 0f,
                                        doppler = doppler,
                                        relativeVelocity = velocity,
                                        //distance = pseudorangeMeters
                                    )
                                )
                            }
                        }
                        satellites = list
                    }
                }
            } else null

        val locationListener = LocationListener { }

        // Register callbacks
        locationManager.registerGnssStatusCallback(gnssStatusCallback)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 0f, locationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssMeasurementsCallback != null) {
            locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback)
        }

        onDispose {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssMeasurementsCallback != null) {
                locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("GNSS Satellites", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Total satellites: ${satellites.size}", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(satellites.filter { it.cn0 > 0 }) { sat ->
                val qualityColor = when {
                    sat.cn0 >= 40 -> Color(0xFF4CAF50)
                    sat.cn0 >= 30 -> Color(0xFFFFC107)
                    sat.cn0 >= 20 -> Color(0xFFFF5722)
                    else -> Color(0xFFF44336)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SVID: ${sat.svid}", color = TextPrimary)
                        Text("Constellation: ${constellationName(sat.constellation)}", color = TextPrimary)
                        Text("Signal CN0: ${"%.1f".format(sat.cn0)} dB-Hz", color = qualityColor, fontSize = 16.sp)
                        Text("Elevation: ${"%.1f".format(sat.elevation)}°", color = TextPrimary)
                        Text("Azimuth: ${"%.1f".format(sat.azimuth)}°", color = TextPrimary)
                        Text("Used in fix?: ${if (sat.fix) "Yes" else "No"}", color = TextPrimary)
                        Text("Doppler: ${"%.2f".format(sat.doppler)} m/s", color = TextPrimary)
                        Text("Pseudorange uncertainty: ${"%.2f".format(sat.relativeVelocity)} m/s", color = TextPrimary)
                        //Text("Distance: ${"%.1f".format(sat.distance / 1000)} km", color = TextPrimary) // show in km
                    }
                }
            }
        }
    }
}

fun constellationName(constellation: Int): String {
    return when (constellation) {
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
