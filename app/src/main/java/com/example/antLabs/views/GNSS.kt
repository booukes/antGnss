@file:Suppress("DEPRECATION")

package com.example.antLabs.views

import android.Manifest
import android.R.attr.radius
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.runtime.collectAsState
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
import com.example.antLabs.engine.GNSSEngine
import com.example.antLabs.engine.GNSSReceiverData
import com.example.antLabs.network.ApiService.fetchSatelliteDistance
import com.example.antLabs.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// --- Data classes (UI / API specific) ---

data class ApiResponseGroup(val name: String, val distanceKm: Double, val eclipsed: Boolean)



// --- Main Composable ---
@SuppressLint("MissingPermission")
@Composable
fun GNSS(onInfoClick: () -> Unit) {
    var selectedSvid by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    var showSplitView by remember { mutableStateOf(false) }
    val gnssEngine = remember { GNSSEngine(context) }
    val satellites by gnssEngine.satellites.collectAsState()
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
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // If permission is granted, start listening
                gnssEngine.registerCallbacks()
            }
        }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(gnssEngine) {
        gnssEngine.registerCallbacks()
        onDispose {
            gnssEngine.unregisterCallbacks()
        }
    }


    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center ) {
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

        Text("Tracked satellites: ${satellites.count { it.cn0 > 0 }}", color = TextPrimary, modifier=Modifier.align(Alignment.CenterHorizontally))
        Text("Primary satellites: ${satellites.count { it.cn0 > 30 }}", color = TextPrimary, modifier=Modifier.align(Alignment.CenterHorizontally))
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
                        APIBridge(context, sat.svid, sat.constellation)
                    }
                }
            }
        }
    }
}


@Composable
fun SkyMapMini(
    satellites: List<GNSSReceiverData>, label: String,
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

@Composable
fun SkyMapFull(
    satellites: List<GNSSReceiverData>,
    directionPaint: android.graphics.Paint,
    satPaint: android.graphics.Paint,
    selectedSvid: Int?,
    onSvidSelected: (Int) -> Unit
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
                    x + 8,
                    y - 8,
                    directionPaint
                )
            }

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

@Composable
fun APIBridge(context: Context, svid: Int, constellation: Int) {
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var eclipsed by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(svid) @androidx.annotation.RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]) {
        var APIResponseGroup: ApiResponseGroup? = null
        try {
            APIResponseGroup =
                fetchSatelliteDistance(context, svid, constellation, 0.0)
        } catch (e: SecurityException){
            e.printStackTrace()
        }
        if (APIResponseGroup == null) {
            error = true
            return@LaunchedEffect
        }
        distanceKm = APIResponseGroup.distanceKm
        name = APIResponseGroup.name
        eclipsed = APIResponseGroup.eclipsed
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