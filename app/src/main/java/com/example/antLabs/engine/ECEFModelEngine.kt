package com.example.antLabs.engine

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// calculate 3D distance using euclidian model
fun ECEFModelEngine(
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