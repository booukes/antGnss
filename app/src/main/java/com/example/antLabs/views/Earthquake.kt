package com.example.antLabs.views

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log
import kotlin.math.sqrt

// --- Data classes for USGS API ---
@Serializable
data class EarthquakeFeature(
    val properties: Properties
)

@Serializable
data class Properties(
    val place: String,
    val mag: Float,
    val time: Long
)

@Serializable
data class EarthquakeResponse(
    val features: List<EarthquakeFeature>
)

@Composable
fun Earthquake() {
    val context = LocalContext.current

    // --- Accelerometer setup ---
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var z by remember { mutableStateOf(0f) }
    var shakeDetected by remember { mutableStateOf(false) }
    var lastMagnitude by remember { mutableStateOf(0f) }
    var lastShakeTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    x = it.values[0]
                    y = it.values[1]
                    z = it.values[2]

                    val magnitude = sqrt(x*x + y*y + z*z)
                    val accelThreshold = 5f // tweak sensitivity
                    if ((magnitude - lastMagnitude) > accelThreshold) {
                        shakeDetected = true
                        lastShakeTime = System.currentTimeMillis() // record shake time
                    }
                    lastMagnitude = magnitude
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (shakeDetected) {
                val elapsed = System.currentTimeMillis() - lastShakeTime
                val delayTime = 250L - elapsed
                if (delayTime > 0) kotlinx.coroutines.delay(delayTime)
                // Only reset if no new shake happened in the meantime
                if (System.currentTimeMillis() - lastShakeTime >= 250L) {
                    shakeDetected = false
                }
            } else {
                kotlinx.coroutines.delay(50L) // small delay to prevent busy loop
            }
        }
    }

    // --- USGS Earthquake API ---
    var earthquakes by remember { mutableStateOf(listOf<EarthquakeFeature>()) }

    LaunchedEffect(Unit) {
        earthquakes = fetchEarthquakes()
    }

    // --- Compose UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shake detected: ${if (shakeDetected) "YES" else "No"}",
            color = if (shakeDetected) Color.Red else Color.Green,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Accelerometer (m/s²):",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
        Text("X: %.2f  Y: %.2f  Z: %.2f".format(x, y, z), color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Recent Earthquakes (past hour):",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )

        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(earthquakes) { eq ->
                val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(eq.properties.time))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Magnitude: ${eq.properties.mag}", color = Color.White)
                        Text("Location: ${eq.properties.place}", color = Color.White)
                        Text("Time: $date", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// --- Suspend function to fetch earthquakes ---
suspend fun fetchEarthquakes(): List<EarthquakeFeature> = withContext(Dispatchers.IO) {
    val client = HttpClient(CIO)
    try {
        val response: String = client.get(
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson"
        ).bodyAsText()
        Log.d("EarthquakeAPI", response)
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(EarthquakeResponse.serializer(), response)

        Log.d("EarthquakeAPI", "Parsed features: ${parsed.features.size}")
        parsed.features
    } catch (e: Exception) {
        Log.e("EarthquakeAPI", "Parsing error", e)
        emptyList()
    } finally {
        client.close()
    }
}
