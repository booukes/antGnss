@file:Suppress("DEPRECATION")

package com.example.antLabs.views

import android.Manifest
import android.R.attr.radius
import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
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
import com.example.antLabs.network.ApiService.fetchSatelliteDistance
import com.example.antLabs.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// --- Data classes ---

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

@Composable
fun SatelliteDistanceText(context: Context, svid: Int, constellation: Int) {
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var eclipsed by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(svid) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
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