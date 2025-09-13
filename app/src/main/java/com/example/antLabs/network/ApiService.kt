package com.example.antLabs.network

import android.Manifest
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.antLabs.engine.ECEFModelEngine
import com.example.antLabs.engine.SatelliteResponse
import com.example.antLabs.satmaps.beidouSvidToNorad
import com.example.antLabs.satmaps.gpsSvidToNorad
import com.example.antLabs.views.ApiResponseGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

object ApiService {
    // @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun fetchSatelliteDistance(
        context: Context,
        svid: Int,
        constellation: Int,
        userAltKm: Double = 0.0,
    ): ApiResponseGroup? {
        if (svid == 7) return null
        if (constellation != 1 && constellation != 5) return null
        val satId: Int = if (constellation == 1) {
            gpsSvidToNorad[svid] ?: return null
        } else {
            beidouSvidToNorad[svid] ?: return null
        }

        // --- Get device location ---
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val location: Location? = try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.e("SAT_DIST", "Error while getting location: ${e.message}")
            return null
        }

        if (location == null) {
            Log.e("SAT_DIST", "Location is null. Cannot fetch satellite distance.")
            return null
        }

        val client = HttpClient(OkHttp) {
            install(ContentNegotiation.Plugin) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return try {
            val url =
                "https://api.n2yo.com/rest/v1/satellite/positions/$satId/${location.latitude}/${location.longitude}/$userAltKm/1/&apiKey=P5SBR8-AMPS6R-XNXR4U-5KD3"
            Log.d("APIRQ", url)
            val response: SatelliteResponse = client.get(url).body()
            Log.d("APIRSP", response.toString())
            val satname = response.info.satname
            val eclipsed = response.positions.firstOrNull()?.eclipsed ?: true
            response.positions.firstOrNull()?.let { pos ->
                val distanceKm = ECEFModelEngine(
                    pos.satlatitude, pos.satlongitude, pos.sataltitude,
                    location.latitude, location.longitude, userAltKm
                )
                ApiResponseGroup(satname, distanceKm, eclipsed)
            }
        } catch (e: Exception) {
            Log.e("SAT_DIST", "Network call failed", e)
            null
        } finally {
            client.close()
        }
    }
}