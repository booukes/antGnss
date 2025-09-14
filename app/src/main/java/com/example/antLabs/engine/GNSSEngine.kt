package com.example.antLabs.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

// Represents a single satellite reading from the GNSS receiver
data class GNSSReceiverData(
    val svid: Int,                    // Antenna received sat ID
    val constellation: Int,           // GNSS constellation group
    val cn0: Double,                   // Carrier-to-noise ratio in dB-Hz
    val fix: Boolean,                  // Satellite position fix usage
    val elevation: Float,              // Elevation angle in degrees
    val azimuth: Float,                // Azimuth angle in degrees
    val doppler: Double,               // Doppler velocity (m/s)
    val pseudorangeUncertainty: Double,// Measurement uncertainty
    var distanceKm: Double = 0.0,     // Optional computed distance in km
    val lastPing: Long = System.currentTimeMillis() // Last update timestamp
)

// Represents a satellite's position in geodetic coordinates (NOT ECEF)
@Serializable
data class SatellitePosition(
    val satlatitude: Double,  // Latitude in degrees
    val satlongitude: Double, // Longitude in degrees
    val sataltitude: Double,  // Altitude in km
    val eclipsed: Boolean     // Whether the satellite is in Earth's shadow
)

// Satellite metadata
@Serializable
data class SatelliteMetadata(
    val satname: String,      // Satellite name
    val transactionscount: Int // Optional transaction count for tracking
)

// Response object containing satellite info and its positions
@Serializable
data class SatelliteResponse(
    val info: SatelliteMetadata,
    val positions: List<SatellitePosition>
)

// Main GNSS engine handling satellite updates and measurements
@Suppress("DEPRECATION")
class GNSSEngine(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    // Internal state for all satellites, exposed as read-only StateFlow
    private val _satellites = MutableStateFlow<List<GNSSReceiverData>>(emptyList())
    val satellites = _satellites.asStateFlow()

    // Basic LocationListener, required to request GPS updates
    private val locationListener = LocationListener { }

    // Callback to receive GNSS satellite status updates
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = _satellites.value.toMutableList()
            for (i in 0 until status.satelliteCount) {
                // Find existing satellite data in the list
                val idx = list.indexOfFirst { it.svid == status.getSvid(i) }

                // Create or update satellite info
                val sat = GNSSReceiverData(
                    svid = status.getSvid(i),
                    constellation = status.getConstellationType(i),
                    cn0 = status.getCn0DbHz(i).toDouble(),
                    elevation = status.getElevationDegrees(i),
                    azimuth = status.getAzimuthDegrees(i),
                    fix = status.usedInFix(i),
                    doppler = list.getOrNull(idx)?.doppler ?: 0.0,
                    pseudorangeUncertainty = list.getOrNull(idx)?.pseudorangeUncertainty ?: 0.0,
                    lastPing = System.currentTimeMillis()
                )

                // Update existing satellite or add a new one
                if (idx >= 0) list[idx] = sat else list.add(sat)
            }
            _satellites.value = list // Update the StateFlow
        }
    }

    // Callback to receive GNSS raw measurement events (doppler, pseudorange, etc.)
    private val gnssMeasurementsCallback = object : GnssMeasurementsEvent.Callback() {
        override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
            val list = _satellites.value.toMutableList()
            for (measurement in event.measurements) {
                val idx = list.indexOfFirst { it.svid == measurement.svid }
                val doppler = measurement.pseudorangeRateMetersPerSecond
                val velocity = measurement.pseudorangeRateUncertaintyMetersPerSecond

                if (idx >= 0) {
                    // Update doppler & uncertainty for existing satellite
                    val sat = list[idx]
                    list[idx] = sat.copy(doppler = doppler, pseudorangeUncertainty = velocity)
                } else {
                    // Rare case: satellite not yet seen by GNSS status callback
                    list.add(
                        GNSSReceiverData(
                            svid = measurement.svid,
                            constellation = measurement.constellationType,
                            cn0 = measurement.cn0DbHz,
                            fix = false,
                            elevation = 0f,
                            azimuth = 0f,
                            doppler = doppler,
                            pseudorangeUncertainty = velocity
                        )
                    )
                }
            }
            _satellites.value = list // Update StateFlow
        }
    }

    /**
     * Register GNSS callbacks and start receiving updates
     * Requires ACCESS_FINE_LOCATION permission
     */
    @SuppressLint("MissingPermission") //shhh
    fun registerCallbacks() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100L,   // min time in ms between updates
                0f,     // min distance in meters between updates
                locationListener
            )
            locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback)
        }
    }

    /**
     * Unregister GNSS callbacks and stop receiving updates
     */
    fun unregisterCallbacks() {
        locationManager.removeUpdates(locationListener)
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback)
    }
}
