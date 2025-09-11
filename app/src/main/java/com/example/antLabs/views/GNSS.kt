@file:Suppress("DEPRECATION")

package com.example.antLabs.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.antLabs.ui.theme.TextPrimary
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// --- Data classes ---

// GPS SVID to NORAD ID map
val gpsSvidToNorad = mapOf(
    1 to 10684,   // NAVSTAR 1 (OPS 5111)
    2 to 10893,   // NAVSTAR 2 (OPS 5112)
    3 to 11054,   // NAVSTAR 3 (OPS 5113)
    4 to 11141,   // NAVSTAR 4 (OPS 5114)
    5 to 11690,   // NAVSTAR 5 (OPS 5117)
    6 to 11783,   // NAVSTAR 6 (OPS 5118)
    8 to 14189,   // NAVSTAR 8 (OPS 9794)
    9 to 15039,   // NAVSTAR 9 (USA 1)
    10 to 15271,  // NAVSTAR 10 (USA 5)
    11 to 16129,  // NAVSTAR 11 (USA 10)
    13 to 19802,  // NAVSTAR 13 (USA 35)
    14 to 20061,  // NAVSTAR 14 (USA 38)
    15 to 20185,  // NAVSTAR 15 (USA 42)
    16 to 20302,  // NAVSTAR 16 (USA 47)
    17 to 20361,  // NAVSTAR 17 (USA 49)
    18 to 20452,  // NAVSTAR 18 (USA 50)
    19 to 20533,  // NAVSTAR 19 (USA 54)
    20 to 20724,  // NAVSTAR 20 (USA 63)
    21 to 20830,  // NAVSTAR 21 (USA 64)
    22 to 20959,  // NAVSTAR 22 (USA 66)
    23 to 21552,  // NAVSTAR 23 (USA 71)
    24 to 21890,  // NAVSTAR 24 (USA 79)
    25 to 21930,  // NAVSTAR 25 (USA 80)
    26 to 22014,  // NAVSTAR 26 (USA 83)
    27 to 22108,  // NAVSTAR 27 (USA 84)
    28 to 22231,  // NAVSTAR 28 (USA 85)
    29 to 22275,  // NAVSTAR 29 (USA 87)
    30 to 22446,  // NAVSTAR 30 (USA 88)
    31 to 22581,  // NAVSTAR 31 (USA 90)
    32 to 22657,  // NAVSTAR 32 (USA 91)
    33 to 22700,  // NAVSTAR 33 (USA 92)
    34 to 22779,  // NAVSTAR 34 (USA 94)
    35 to 22877,  // NAVSTAR 35 (USA 96)
    36 to 23027,  // NAVSTAR 36 (USA 100)
    37 to 23833,  // NAVSTAR 37 (USA 117)
    38 to 23953,  // NAVSTAR 38 (USA 126)
    39 to 24320,  // NAVSTAR 39 (USA 128)
    43 to 24876,  // NAVSTAR 43 (USA 132)
    44 to 25030,  // NAVSTAR 44 (USA 135)
    46 to 25933,  // NAVSTAR 46 (USA 145)
    47 to 26360,  // NAVSTAR 47 (USA 150)
    48 to 26407,  // NAVSTAR 48 (USA 151)
    49 to 26605,  // NAVSTAR 49 (USA 154)
    50 to 26690,  // NAVSTAR 50 (USA 156)
    51 to 27663,  // NAVSTAR 51 (USA 166)
    52 to 27704,  // NAVSTAR 52 (USA 168)
    53 to 28129,  // NAVSTAR 53 (USA 175)
    54 to 28190,  // NAVSTAR 54 (USA 177)
    55 to 28361,  // NAVSTAR 55 (USA 178)
    56 to 28474   // NAVSTAR 56 (USA 180)
)
val beidouSvidToNorad = mapOf(
    1 to 26599,
    2 to 26643,
    3 to 27813,
    4 to 30323,
    5 to 31115,
    6 to 34779,
    7 to 36287,
    8 to 36590,
    9 to 36828,
    10 to 37210,
    11 to 37256,
    12 to 37384,
    13 to 37763,
    14 to 37948,
    15 to 38091,
    16 to 38250,
    17 to 38251,
    18 to 38774,
    19 to 38775,
    20 to 38953,
    21 to 40549,
    22 to 40748,
    23 to 40749,
    24 to 40938,
    25 to 41315,
    26 to 41434,
    27 to 41586,
    28 to 43001,
    29 to 43002,
    30 to 43107,
    31 to 43108,
    32 to 43207,
    33 to 43208,
    34 to 43245,
    35 to 43246,
    36 to 43539,
    37 to 43581,
    38 to 43582,
    39 to 43602,
    40 to 43603,
    41 to 43647,
    42 to 43648,
    43 to 43683,
    44 to 43706,
    45 to 43707,
    46 to 44231,
    47 to 44542,
    48 to 44543,
    49 to 44793,
    50 to 44794,
    51 to 44864,
    52 to 44865,
    53 to 45344,
    54 to 45807,
    55 to 58654,
    56 to 58655,
    57 to 61186,
    58 to 61187
)

data class SatelliteUIHelper(
    val name: String,
    val distanceKm: Double
)
@Serializable
data class SatellitePosition(
    val satlatitude: Double,
    val satlongitude: Double,
    val sataltitude: Double
)

@Serializable
data class SatelliteInfo(
    val satname: String,
    val satid: Int,
    val transactionscount: Int
)
@Serializable
data class SatelliteResponse(
    val info: SatelliteInfo,
    val positions: List<SatellitePosition>
)

data class SatelliteInfoExtended(
    val svid: Int,
    val constellation: Int,
    val cn0: Double,
    val fix: Boolean,
    val elevation: Float,
    val azimuth: Float,
    val doppler: Double,
    val pseudorangeUncertainty: Double,
    var distanceKm: Double = 0.0, // new: distance from HORIZONS
    val lastPing: Long = System.currentTimeMillis()
)

@SuppressLint("MissingPermission")
@Composable
fun GNSS(onInfoClick: () -> Unit) {
    val context = LocalContext.current
    val locationManager = context.getSystemService(LocationManager::class.java)
    var satellites by remember { mutableStateOf(listOf<SatelliteInfoExtended>()) }

    // View toggle state
    var showSplitView by remember { mutableStateOf(false) }

    val directionPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            isAntiAlias = true
        }
    }

    val satPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 16f
            isAntiAlias = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
       fetchSatelliteDistance(context, 1, 1,0.0)
    }

    DisposableEffect(Unit) {
        val gnssStatusCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val list = satellites.toMutableList()
                for (i in 0 until status.satelliteCount) {
                    val idx = list.indexOfFirst { it.svid == status.getSvid(i) }
                    val sat = SatelliteInfoExtended(
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
                satellites = list
                //Log.d("GNSS", "Updated satellites: ${satellites.map { it.svid to it.cn0 to it.fix }}")
            }
        }

        val gnssMeasurementsCallback =
            object : GnssMeasurementsEvent.Callback() {
                override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                    val list = satellites.toMutableList()
                    for (measurement in event.measurements) {
                        val idx = list.indexOfFirst { it.svid == measurement.svid }
                        val doppler = measurement.pseudorangeRateMetersPerSecond
                        val velocity = measurement.pseudorangeRateUncertaintyMetersPerSecond
                        if (idx >= 0) {
                            val sat = list[idx]
                            list[idx] = sat.copy(doppler = doppler, pseudorangeUncertainty = velocity)
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
                                    pseudorangeUncertainty = velocity
                                )
                            )
                        }
                    }
                    satellites = list
                }
            }
        val locationListener = LocationListener { }
        locationManager.registerGnssStatusCallback(gnssStatusCallback)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 0f, locationListener)
        locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback)

        onDispose {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback)
        }
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "GNSS Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
        }

        Text("Tracked satellites: ${satellites.count { it.cn0 > 0 }}", color = TextPrimary)
        Text("Primary satellites: ${satellites.count { it.cn0 > 30 }}", color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        // Toggle Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { showSplitView = !showSplitView },
                modifier = Modifier.padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            ) {
                Text(if (showSplitView) "Single Map View" else "Split Map View")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

// Animated map container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize() // smooth layout changes
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Full map with fade + scale
            this@Column.AnimatedVisibility(
                visible = !showSplitView,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
                exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
            ) {
                Box(modifier = Modifier.size(300.dp)) {
                    SkyMapFull(satellites, directionPaint, satPaint)
                }
            }

            // Split maps with staggered fade + slide
            this@Column.AnimatedVisibility(
                visible = showSplitView,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Column {
                    val constellations = listOf(1 to "GPS", 3 to "GLONASS", 5 to "BeiDou", -1 to "Other")
                    for (row in constellations.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { index, (constellationId, label) ->
                                val sats = if (constellationId != -1) {
                                    satellites.filter { it.constellation == constellationId && it.cn0 > 0 }
                                } else {
                                    satellites.filter { it.constellation !in listOf(1,3,5) && it.cn0 > 0 }
                                }

                                // Staggered animation
                                val animDelay = index * 100
                                AnimatedVisibility(
                                    visible = showSplitView,
                                    enter = fadeIn(tween(400, delayMillis = animDelay)) +
                                            slideInVertically(
                                                tween(400, delayMillis = animDelay),
                                                initialOffsetY = { it / 2 }
                                            ),
                                    exit = fadeOut()
                                ) {
                                    Box(modifier = Modifier.size(150.dp)) {
                                        SkyMapMini(sats, label, directionPaint, satPaint)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))



        // Satellite info
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(satellites) { sat ->
                val qualityColor = when {
                    sat.cn0 >= 30 -> Color.Green
                    sat.cn0 >= 25 -> Color.Yellow
                    sat.cn0 >= 20 -> Color(0xFFFF5722)
                    sat.cn0 > 0 -> Color.Red
                    else -> {
                        Color.DarkGray
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(width = 3.dp, color = qualityColor, shape = RoundedCornerShape(8.dp)), // <-- border,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SVID: ${sat.svid}", color = TextPrimary)
                        Text("Constellation: ${constellationName(sat.constellation)}", color = TextPrimary)
                        Text("CN0: ${"%.1f".format(sat.cn0)} dB-Hz", color = qualityColor, fontSize = 16.sp)
                        Text("Elevation: ${"%.1f".format(sat.elevation)}°", color = TextPrimary)
                        Text("Azimuth: ${"%.1f".format(sat.azimuth)}°", color = TextPrimary)
                        Text("Used in fix?: ${if (sat.fix) "Yes" else "No"}", color = TextPrimary)
                        Text("Doppler: ${"%.2f".format(sat.doppler)} m/s", color = TextPrimary)
                        Text("Pseudorange uncertainty: ${"%.2f".format(sat.pseudorangeUncertainty)} m/s", color = TextPrimary)
                        SatelliteDistanceText(context, sat.svid, sat.constellation)

                    }
                }
            }
        }
    }
}

// SkyMapMini
@Composable
fun SkyMapMini(satellites: List<SatelliteInfoExtended>, label: String,
               directionPaint: android.graphics.Paint, satPaint: android.graphics.Paint) {
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(modifier = Modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 8.dp.toPx()

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1A33), Color(0xFF0B0C1A), Color.Black),
                    center = center,
                    radius = radius
                ),
                size = size
            )

            for (i in 1..3) drawCircle(Color.LightGray.copy(alpha = 0.4f), radius * i / 3, center, style = Stroke(width = 1.5f))

            val directions = listOf("N","NE","E","SE","S","SW","W","NW")
            for (i in directions.indices) {
                val angle = Math.toRadians(i * 45.0)
                val x = center.x + radius * sin(angle).toFloat()
                val y = center.y - radius * cos(angle).toFloat()
                drawLine(Color.LightGray.copy(alpha = 0.3f), center, Offset(x,y), 1f)
                drawContext.canvas.nativeCanvas.drawText(directions[i], x+4, y-4, directionPaint)
            }

            satellites.forEach { sat ->
                val elRad = Math.toRadians(90.0 - sat.elevation)
                val azRad = Math.toRadians(sat.azimuth.toDouble())
                val r = radius * (elRad / Math.PI)
                val x = center.x + (r * sin(azRad)).toFloat()
                val y = center.y - (r * cos(azRad)).toFloat()

                val color = when {
                    sat.cn0 >= 30 -> Color.Green
                    sat.cn0 >= 25 -> Color.Yellow
                    sat.cn0 >= 20 -> Color(0xFFFF5722)
                    else -> Color.Red
                }
                val dotSize = 3f + (sat.cn0.toFloat() / 10f)
                if (sat.fix) {
                    // Pulsing effect for satellites in fix
                    drawCircle(color.copy(alpha = 0.7f), dotSize * scale, Offset(x, y))
                } else {
                    drawCircle(color, dotSize, Offset(x, y), style = Stroke(width = 2f))
                }
                drawContext.canvas.nativeCanvas.drawText("${sat.svid}", x+6, y-6, satPaint)
            }

            drawContext.canvas.nativeCanvas.drawText(label, center.x - 20, size.height - 4.dp.toPx(), directionPaint)
        }
    }
}

// SkyMapFull
@Composable
fun SkyMapFull(
    satellites: List<SatelliteInfoExtended>,
    directionPaint: android.graphics.Paint,
    satPaint: android.graphics.Paint
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 16.dp.toPx()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D1A33), Color(0xFF0B0C1A), Color.Black),
                center = center,
                radius = radius
            ),
            size = size
        )

        for (i in 1..3) drawCircle(Color.LightGray.copy(alpha = 0.4f), radius * i / 3, center, style = Stroke(width = 1.5f))

        val directions = listOf("N","NE","E","SE","S","SW","W","NW")
        for (i in directions.indices) {
            val angle = Math.toRadians(i*45.0)
            val x = center.x + radius * sin(angle).toFloat()
            val y = center.y - radius * cos(angle).toFloat()
            drawLine(Color.LightGray.copy(alpha = 0.3f), center, Offset(x,y), 1f)
            drawContext.canvas.nativeCanvas.drawText(directions[i], x+8, y-8, directionPaint)
        }

        satellites.filter{it.cn0>0}.forEach { sat ->
            val elRad = Math.toRadians(90.0 - sat.elevation)
            val azRad = Math.toRadians(sat.azimuth.toDouble())
            val r = radius * (elRad / Math.PI)
            val x = center.x + (r * sin(azRad)).toFloat()
            val y = center.y - (r * cos(azRad)).toFloat()

            val color = when {
                sat.cn0 >= 30 -> Color.Green
                sat.cn0 >= 25 -> Color.Yellow
                sat.cn0 >= 20 -> Color(0xFFFF5722)
                else -> Color.Red
            }
            val dotSize = 4f + (sat.cn0.toFloat() / 10f)
            if (sat.fix) drawCircle(color, dotSize, Offset(x,y))
            else drawCircle(color, dotSize, Offset(x,y), style=Stroke(width=2f))
            drawContext.canvas.nativeCanvas.drawText("${sat.svid}", x+10, y-10, satPaint)
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

// --- Function ---
@SuppressLint("MissingPermission")
suspend fun fetchSatelliteDistance(
    context: Context,
    svid: Int,
    constellation: Int,
    userAltKm: Double = 0.0,
): SatelliteUIHelper? {
    var satId: Int = 1
    if (svid == 7) return null
    if(constellation != 1 && constellation != 5) return null
    if(constellation == 1) {
        satId = gpsSvidToNorad[svid] ?: return null
    } else if(constellation == 5){
        satId = beidouSvidToNorad[svid] ?: return null
    } else {
        return null
    }


    // --- Get device location ---
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val location: Location? = try {
        fusedLocationClient.lastLocation.await()
    } catch (e: Exception) {
        Log.e("SAT_DIST", "Error what wigetting location: ${e.message}")
        return null
    }

    if (location == null) {
        Log.e("SAT_DIST", "Location is null. Cannot fetch satellite distance.")
        return null
    }

    val userLat = location.latitude
    val userLon = location.longitude

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    return try {
        val url = "https://api.n2yo.com/rest/v1/satellite/positions/$satId/${location.latitude}/${location.longitude}/$userAltKm/1/&apiKey=P5SBR8-AMPS6R-XNXR4U-5KD3"
        val response: SatelliteResponse = client.get(url).body()
        val satname = response.info.satname
        response.positions.firstOrNull()?.let { pos ->
            val distanceKm = distanceToSatellite(
                pos.satlatitude, pos.satlongitude, pos.sataltitude,
                location.latitude, location.longitude, userAltKm
            )
            SatelliteUIHelper(satname, distanceKm)
        }
    } catch (e: Exception) {
        null
    } finally {
        client.close()
    }
}

@Composable
fun SatelliteDistanceText(context: Context, svid: Int, constellation: Int) {
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(svid) {
        val satelliteUiHelper: SatelliteUIHelper? = fetchSatelliteDistance(context, svid, constellation,0.0)
        distanceKm = satelliteUiHelper?.distanceKm
        name = satelliteUiHelper?.name
    }

    Text(
        text = if (distanceKm != null) {
            "Distance: %.2f km".format(distanceKm)
        } else {
            "Cannot perform distance calculation."
        },
        color = TextPrimary
    )
    Text(
        text = if (name != null) {
            "Satellite name: $name"
        } else {
            "Name not available."
        },
        color = TextPrimary
    )
}


// --- Helper: calculate 3D distance ---
fun distanceToSatellite(
    satLat: Double, satLon: Double, satAltKm: Double,
    userLat: Double, userLon: Double, userAltKm: Double = 0.0
): Double {
    val r = 6371.0 // Earth radius in km
    val toRad = Math.PI / 180

    fun toECEF(lat: Double, lon: Double, altKm: Double): Triple<Double, Double, Double> {
        val phi = lat * toRad
        val lambda = lon * toRad
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val cosLambda = cos(lambda)
        val sinLambda = sin(lambda)
        val x = (r + altKm) * cosPhi * cosLambda
        val y = (r + altKm) * cosPhi * sinLambda
        val z = (r + altKm) * sinPhi
        return Triple(x, y, z)
    }

    val (sx, sy, sz) = toECEF(satLat, satLon, satAltKm)
    val (ux, uy, uz) = toECEF(userLat, userLon, userAltKm)

    return sqrt((sx - ux).pow(2) + (sy - uy).pow(2) + (sz - uz).pow(2))
}