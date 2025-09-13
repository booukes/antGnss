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

data class GNSSReceiverData(
    val svid: Int,
    val constellation: Int,
    val cn0: Double,
    val fix: Boolean,
    val elevation: Float,
    val azimuth: Float,
    val doppler: Double,
    val pseudorangeUncertainty: Double,
    var distanceKm: Double = 0.0,
    val lastPing: Long = System.currentTimeMillis()
)

@Serializable
data class SatellitePosition(
    val satlatitude: Double,
    val satlongitude: Double,
    val sataltitude: Double,
    val eclipsed: Boolean
)

@Serializable
data class SatelliteInfo(val satname: String, val satid: Int, val transactionscount: Int)

@Serializable
data class SatelliteResponse(val info: SatelliteInfo, val positions: List<SatellitePosition>)

class GNSSEngine(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val _satellites = MutableStateFlow<List<GNSSReceiverData>>(emptyList())
    val satellites = _satellites.asStateFlow()
    private val locationListener = LocationListener { }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = _satellites.value.toMutableList()
            for (i in 0 until status.satelliteCount) {
                val idx = list.indexOfFirst { it.svid == status.getSvid(i) }
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
                if (idx >= 0) list[idx] = sat else list.add(sat)
            }
            _satellites.value = list
        }
    }

    private val gnssMeasurementsCallback = object : GnssMeasurementsEvent.Callback() {
        override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
            val list = _satellites.value.toMutableList()
            for (measurement in event.measurements) {
                val idx = list.indexOfFirst { it.svid == measurement.svid }
                val doppler = measurement.pseudorangeRateMetersPerSecond
                val velocity = measurement.pseudorangeRateUncertaintyMetersPerSecond
                if (idx >= 0) {
                    val sat = list[idx]
                    list[idx] = sat.copy(doppler = doppler, pseudorangeUncertainty = velocity)
                } else {
                    // case is less likely now since GnssStatusCallback usually spots the satellite first
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
            _satellites.value = list
        }
    }

    @SuppressLint("MissingPermission")
    fun registerCallbacks() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100L,
                0f,
                locationListener
            )
            locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback)
        }
    }

    fun unregisterCallbacks() {
        locationManager.removeUpdates(locationListener)
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback)
    }
}