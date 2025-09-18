package com.example.antLabs.network

import android.Manifest
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.antLabs.BuildConfig
import com.example.antLabs.engine.ECEFEngine.ECEFDistanceWorker
import com.example.antLabs.engine.SatelliteResponse
import com.example.antLabs.satmaps.beidouSvidToNorad
import com.example.antLabs.satmaps.galileoSvidToNorad
import com.example.antLabs.satmaps.glonassSvidToNorad
import com.example.antLabs.satmaps.gpsSvidToNorad
import com.example.antLabs.views.ApiResponseGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.tasks.await

/**
 * ApiService handles fetching satellite positions from the N2YO API
 * and calculating distances from the user's current location.
 */
object ApiService {

    /**
     * Fetches satellite distance for a given satellite ID (svid) and constellation.
     *
     * @param context Required for accessing device location
     * @param svid Satellite ID (from GNSS)
     * @param constellation Constellation type (1 = GPS, 5 = Beidou)
     * @param userAltKm User altitude in kilometers (default 0)
     * @return ApiResponseGroup containing satellite name, distance, and eclipsed status, or null if any error occurs
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun fetchSatelliteDistance(
        context: Context,
        svid: Int,
        constellation: Int,
        userAltKm: Double = 0.0,
    ): ApiResponseGroup? {
        val APIKEY = BuildConfig.APIKEY
        // --- Filter out unsupported satellites ---
        if (constellation != 1 && constellation != 3 && constellation != 5 && constellation != 6) return null

        // Map GNSS SVID to NORAD ID for API query
        val satId: Int = when (constellation) {
            1 -> {
                gpsSvidToNorad[svid] ?: return null
            }
            3 -> {
                glonassSvidToNorad[svid] ?: return null
            }
            5 -> {
                beidouSvidToNorad[svid] ?: return null
            }
            else -> {
                galileoSvidToNorad[svid] ?: return null
            }
        }

        // --- Get device's last known location ---
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val location: Location? = try {
            fusedLocationClient.lastLocation.await()  // Suspend until location is ready
        } catch (e: Exception) {
            Log.e("SAT_DIST", "Error while getting location: ${e.message}")
            return null
        }

        // If location is null, we cannot continue
        if (location == null) {
            Log.e("SAT_DIST", "Location is null. Cannot fetch satellite distance.")
            return null
        }

        // --- Set up HTTP client ---
        val client = KtorClientWrapper.http

        return try {
            // Build API URL using NORAD ID and user location
            val url =
                "https://api.n2yo.com/rest/v1/satellite/positions/$satId/${location.latitude}/${location.longitude}/$userAltKm/1/&apiKey=$APIKEY"
            Log.d("APIRQ", url)

            // Make API call and deserialize response into SatelliteResponse
            val response: SatelliteResponse = client.get(url).body()
            Log.d("APIRSP", response.toString())

            val satname = response.info.satname
            val eclipsed = response.positions.firstOrNull()?.eclipsed ?: true

            // Calculate distance using ECEF engine
            response.positions.firstOrNull()?.let { pos ->
                val distanceKm = ECEFDistanceWorker(
                    pos.satlatitude, pos.satlongitude, pos.sataltitude,
                    location.latitude, location.longitude, userAltKm
                )
                ApiResponseGroup(satname, distanceKm, eclipsed)
            }
        } catch (e: Exception) {
            // Log any API/network errors
            Log.e("SAT_DIST", "API call failed", e)
            null
        }
    }
}
