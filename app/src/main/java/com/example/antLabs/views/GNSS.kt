@file:Suppress("DEPRECATION")

package com.example.antLabs.views

import android.Manifest
import android.R.attr.radius
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// --- Data classes ---
val gpsSvidToNorad = mapOf(
    1 to 10684, 2 to 10893, 3 to 11054, 4 to 11141, 5 to 11690,
    6 to 11783, 8 to 14189, 9 to 15039, 10 to 15271, 11 to 16129,
    13 to 19802, 14 to 20061, 15 to 20185, 16 to 20302, 17 to 20361,
    18 to 20452, 19 to 20533, 20 to 20724, 21 to 20830, 22 to 20959,
    23 to 21552, 24 to 21890, 25 to 21930, 26 to 22014, 27 to 22108,
    28 to 22231, 29 to 22275, 30 to 22446, 31 to 22581, 32 to 22657,
    33 to 22700, 34 to 22779, 35 to 22877, 36 to 23027, 37 to 23833,
    38 to 23953, 39 to 24320, 43 to 24876, 44 to 25030, 46 to 25933,
    47 to 26360, 48 to 26407, 49 to 26605, 50 to 26690, 51 to 27663,
    52 to 27704, 53 to 28129, 54 to 28190, 55 to 28361, 56 to 28474
)
val beidouSvidToNorad = mapOf(
    1 to 26599, 2 to 26643, 3 to 27813, 4 to 30323, 5 to 31115, 6 to 34779,
    7 to 36287, 8 to 36590, 9 to 36828, 10 to 37210, 11 to 37256, 12 to 37384,
    13 to 37763, 14 to 37948, 15 to 38091, 16 to 38250, 17 to 38251, 18 to 38774,
    19 to 38775, 20 to 38953, 21 to 40549, 22 to 40748, 23 to 40749, 24 to 40938,
    25 to 41315, 26 to 41434, 27 to 41586, 28 to 43001, 29 to 43002, 30 to 43107,
    31 to 43108, 32 to 43207, 33 to 43208, 34 to 43245, 35 to 43246, 36 to 43539,
    37 to 43581, 38 to 43582, 39 to 43602, 40 to 43603, 41 to 43647, 42 to 43648,
    43 to 43683, 44 to 43706, 45 to 43707, 46 to 44231, 47 to 44542, 48 to 44543,
    49 to 44793, 50 to 44794, 51 to 44864, 52 to 44865, 53 to 45344, 54 to 45807,
    55 to 58654, 56 to 58655, 57 to 61186, 58 to 61187
)

data class SatelliteUIHelper(val name: String, val distanceKm: Double, val eclipsed: Boolean)

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

data class SatelliteInfoExtended(
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

// --- Main Composable ---
@SuppressLint("MissingPermission")
@Composable
fun GNSS(onInfoClick: () -> Unit) {
    var selectedSvid by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val locationManager = context.getSystemService(LocationManager::class.java)
    var satellites by remember { mutableStateOf(listOf<SatelliteInfoExtended>()) }
    var showSplitView by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val directionPaint = remember {
        android.graphics.Paint()
            .apply { color = android.graphics.Color.WHITE; textSize = 20f; isAntiAlias = true }
    }
    val satPaint = remember {
        android.graphics.Paint()
            .apply { color = android.graphics.Color.WHITE; textSize = 16f; isAntiAlias = true }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        fetchSatelliteDistance(context, 1, 1, 0.0)
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
            }
        }

        val gnssMeasurementsCallback = object : GnssMeasurementsEvent.Callback() {
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
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            100L,
            0f,
            locationListener
        )
        locationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback)

        onDispose {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            locationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsCallback)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "GNSS Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            IconButton(onClick = onInfoClick, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.White
                )
            }
        }

        Text("Tracked satellites: ${satellites.count { it.cn0 > 0 }}", color = TextPrimary)
        Text("Primary satellites: ${satellites.count { it.cn0 > 30 }}", color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            this@Column.AnimatedVisibility(
                visible = !showSplitView,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
                exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                ) {
                    SkyMapFull(
                        satellites,
                        directionPaint,
                        satPaint,
                        selectedSvid,
                        onSvidSelected = { svid ->
                            selectedSvid = svid  // keep the highlight
                            val index = satellites.indexOfFirst { it.svid == svid }
                            if (index >= 0) {
                                scope.launch {
                                    listState.animateScrollToItem(index)  // scroll the LazyColumn
                                }
                            }
                        }
                    )
                }
            }

            this@Column.AnimatedVisibility(
                visible = showSplitView,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Column {
                    val constellations =
                        listOf(1 to "GPS", 3 to "GLONASS", 5 to "BeiDou", -1 to "Other")
                    for (row in constellations.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { index, (constellationId, label) ->
                                val sats = if (constellationId != -1) {
                                    satellites.filter { it.constellation == constellationId && it.cn0 > 0 }
                                } else {
                                    satellites.filter {
                                        it.constellation !in listOf(
                                            1,
                                            3,
                                            5
                                        ) && it.cn0 > 0
                                    }
                                }

                                val animDelay = index * 100
                                AnimatedVisibility(
                                    visible = showSplitView,
                                    enter = fadeIn(tween(400, delayMillis = animDelay)) +
                                            slideInVertically(
                                                tween(400, delayMillis = animDelay),
                                                initialOffsetY = { it / 2 }),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .pointerInput(sats) {
                                                detectTapGestures { tapOffset ->
                                                    val clickedSat = sats.minByOrNull { sat ->
                                                        val elRad =
                                                            Math.toRadians(90.0 - sat.elevation)
                                                        val azRad =
                                                            Math.toRadians(sat.azimuth.toDouble())
                                                        val r = radius * (elRad / Math.PI)
                                                        val centerX = size.width / 2
                                                        val centerY = size.height / 2
                                                        val x = centerX + (r * sin(azRad)).toFloat()
                                                        val y = centerY - (r * cos(azRad)).toFloat()
                                                        val dx = tapOffset.x - x
                                                        val dy = tapOffset.y - y
                                                        dx * dx + dy * dy
                                                    }

                                                    clickedSat?.let { sat ->
                                                        selectedSvid = sat.svid
                                                        Log.d("SELECTEDSVIDROOT", "$selectedSvid")
                                                        val index =
                                                            satellites.indexOfFirst { it.svid == sat.svid }
                                                        if (index >= 0) {
                                                            scope.launch {
                                                                listState.animateScrollToItem(
                                                                    index
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(satellites) { sat ->
                val qualityColor = when {
                    sat.cn0 >= 30 -> Color.Green
                    sat.cn0 >= 25 -> Color.Yellow
                    sat.cn0 >= 20 -> Color(0xFFFF5722)
                    sat.cn0 > 0 -> Color.Red
                    else -> Color.DarkGray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 3.dp,
                            color = qualityColor,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SVID: ${sat.svid}", color = TextPrimary)
                        Text(
                            "Constellation: ${constellationName(sat.constellation)}",
                            color = TextPrimary
                        )
                        Text(
                            "CN0: ${"%.1f".format(sat.cn0)} dB-Hz",
                            color = qualityColor,
                            fontSize = 16.sp
                        )
                        Text("Elevation: ${"%.1f".format(sat.elevation)}°", color = TextPrimary)
                        Text("Azimuth: ${"%.1f".format(sat.azimuth)}°", color = TextPrimary)
                        Text("Used in fix?: ${if (sat.fix) "Yes" else "No"}", color = TextPrimary)
                        Text("Doppler: ${"%.2f".format(sat.doppler)} m/s", color = TextPrimary)
                        Text(
                            "Pseudorange uncertainty: ${"%.2f".format(sat.pseudorangeUncertainty)} m/s",
                            color = TextPrimary
                        )
                        SatelliteDistanceText(context, sat.svid, sat.constellation)
                    }
                }
            }
        }
    }
}

// SkyMapMini
@Composable
fun SkyMapMini(
    satellites: List<SatelliteInfoExtended>, label: String,
    directionPaint: android.graphics.Paint, satPaint: android.graphics.Paint
) {
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

            for (i in 1..3) drawCircle(
                Color.LightGray.copy(alpha = 0.4f),
                radius * i / 3,
                center,
                style = Stroke(width = 1.5f)
            )

            val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            for (i in directions.indices) {
                val angle = Math.toRadians(i * 45.0)
                val x = center.x + radius * sin(angle).toFloat()
                val y = center.y - radius * cos(angle).toFloat()
                drawLine(Color.LightGray.copy(alpha = 0.3f), center, Offset(x, y), 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    directions[i],
                    x + 4,
                    y - 4,
                    directionPaint
                )
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
                drawCircle(color, dotSize, Offset(x, y), style = Stroke(width = 2f))
                drawContext.canvas.nativeCanvas.drawText("${sat.svid}", x + 6, y - 6, satPaint)
            }

            drawContext.canvas.nativeCanvas.drawText(
                label,
                center.x - 20,
                size.height - 4.dp.toPx(),
                directionPaint
            )
        }
    }
}

// SkyMapFull
@Composable
fun SkyMapFull(
    satellites: List<SatelliteInfoExtended>,
    directionPaint: android.graphics.Paint,
    satPaint: android.graphics.Paint,
    selectedSvid: Int?,
    onSvidSelected: (Int) -> Unit // Callback for updating selection
) {
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(satellites) {
                detectTapGestures { tapOffset ->
                    // Find the closest satellite to the tap
                    val clickedSat = satellites.minByOrNull { sat ->
                        val elRad = Math.toRadians(90.0 - sat.elevation)
                        val azRad = Math.toRadians(sat.azimuth.toDouble())
                        val radius = min(size.width, size.height) / 2 - 16.dp.toPx()
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val x = centerX + (radius * (elRad / Math.PI) * sin(azRad)).toFloat()
                        val y = centerY - (radius * (elRad / Math.PI) * cos(azRad)).toFloat()
                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        dx * dx + dy * dy
                    }

                    clickedSat?.let { onSvidSelected(it.svid) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 16.dp.toPx()

            // setting up gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1A33), Color(0xFF0B0C1A), Color.Black),
                    center = center,
                    radius = radius
                ),
                size = size
            )

            // concentrics drawing
            for (i in 1..3) drawCircle(
                Color.LightGray.copy(alpha = 0.4f),
                radius * i / 3,
                center,
                style = Stroke(width = 1.5f)
            )

            // cardinals drawing
            val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            for (i in directions.indices) {
                val angle = Math.toRadians(i * 45.0)
                val x = center.x + radius * sin(angle).toFloat()
                val y = center.y - radius * cos(angle).toFloat()
                drawLine(Color.LightGray.copy(alpha = 0.3f), center, Offset(x, y), 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    directions[i],
                    x + 8,
                    y - 8,
                    directionPaint
                )
            }

            // sat drawing
            satellites.filter { it.cn0 > 0 }.forEach { sat ->
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
                val finalSize = if (sat.svid == selectedSvid) dotSize * scale else dotSize
                val finalColor = color

                if (sat.fix) drawCircle(finalColor, finalSize, Offset(x, y))
                else drawCircle(finalColor, finalSize, Offset(x, y), style = Stroke(width = 2f))

                drawContext.canvas.nativeCanvas.drawText("${sat.svid}", x + 10, y - 10, satPaint)
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

// --- Api call ---
@SuppressLint("MissingPermission")
suspend fun fetchSatelliteDistance(
    context: Context,
    svid: Int,
    constellation: Int,
    userAltKm: Double = 0.0,
): SatelliteUIHelper? {
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
        Log.e("SAT_DIST", "Error what wigetting location: ${e.message}")
        return null
    }

    if (location == null) {
        Log.e("SAT_DIST", "Location is null. Cannot fetch satellite distance.")
        return null
    }

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    return try {
        val url =
            "https://api.n2yo.com/rest/v1/satellite/positions/$satId/${location.latitude}/${location.longitude}/$userAltKm/1/&apiKey=P5SBR8-AMPS6R-XNXR4U-5KD3"
        Log.d("APIRQ", "$url")
        var response: SatelliteResponse = client.get(url).body()
        Log.d("APIRSP", response.toString())
        /*val rawResponse: HttpResponse = client.get(url)
        val textResponse: String = rawResponse.bodyAsText()
        response = Json.decodeFromString<SatelliteResponse>(textResponse)
        Log.d("RSPFMT", response.toString())*/
        val satname = response.info.satname
        val eclipsed = response.positions.firstOrNull()?.eclipsed ?: true
        response.positions.firstOrNull()?.let { pos ->
            val distanceKm = distanceToSatellite(
                pos.satlatitude, pos.satlongitude, pos.sataltitude,
                location.latitude, location.longitude, userAltKm
            )
            SatelliteUIHelper(satname, distanceKm, eclipsed)
        }
    }  catch (e: Exception) {
            Log.e("SAT_DIST", "Network call failed", e)
            null
    } finally {
        client.close()
    }
}

@Composable
fun SatelliteDistanceText(context: Context, svid: Int, constellation: Int) {
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var eclipsed by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(svid) {
        val satelliteUiHelper: SatelliteUIHelper? =
            fetchSatelliteDistance(context, svid, constellation, 0.0)
        if (satelliteUiHelper == null) {
            error = true
            return@LaunchedEffect
        }
        distanceKm = satelliteUiHelper.distanceKm
        name = satelliteUiHelper.name
        eclipsed = satelliteUiHelper.eclipsed
    }

    if (error) {
        Text("API Connection Error.", color = TextPrimary)
        return
    }

    Text(
        text = distanceKm?.let { "Distance: %.2f km".format(it) } ?: "Calculating distance...",
        color = TextPrimary
    )
    Text(
        text = name?.let { "Satellite name: $it" } ?: "Fetching name...",
        color = TextPrimary
    )
    Text(
        text = eclipsed?.let { "Eclipsed?: ${if(it) "Yes" else "No"}" } ?: "",
        color = TextPrimary
    )
}


// calculate 3D distance using euclidian model
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