package com.example.antLabs.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ECEFEngine calculates 3D distances between a satellite and a user
 * using the Earth-Centered, Earth-Fixed (ECEF) coordinate system.
 */
object ECEFEngine {
    private const val EARTH_RADIUS_KM = 6371.0  // Average Earth radius in kilometers
    private const val TO_RAD = Math.PI / 180    // Conversion factor from degrees to radians

    /**
     * Computes the Euclidean distance between a satellite and a user in kilometers.
     *
     * @param satLat Satellite latitude in degrees
     * @param satLon Satellite longitude in degrees
     * @param satAltKm Satellite altitude above Earth in kilometers
     * @param userLat User latitude in degrees
     * @param userLon User longitude in degrees
     * @param userAltKm User altitude above Earth in kilometers (default = 0)
     * @return Distance in kilometers
     */
    fun ECEFDistanceWorker(
        satLat: Double, satLon: Double, satAltKm: Double,
        userLat: Double, userLon: Double, userAltKm: Double = 0.0
    ): Double {
        // Radius from Earth's center to satellite and user
        val rSat = EARTH_RADIUS_KM + satAltKm
        val rUser = EARTH_RADIUS_KM + userAltKm

        // Convert latitude/longitude to radians for trigonometric functions
        val phiS = satLat * TO_RAD       // Satellite latitude in radians
        val lambdaS = satLon * TO_RAD    // Satellite longitude in radians
        val phiU = userLat * TO_RAD      // User latitude in radians
        val lambdaU = userLon * TO_RAD   // User longitude in radians

        // Convert user and satellite geodetic coordinates to ECEF (x, y, z)
        val sx = rSat * cos(phiS) * cos(lambdaS)  // Satellite x-coordinate
        val sy = rSat * cos(phiS) * sin(lambdaS)  // Satellite y-coordinate
        val sz = rSat * sin(phiS)                 // Satellite z-coordinate

        val ux = rUser * cos(phiU) * cos(lambdaU)  // User x-coordinate
        val uy = rUser * cos(phiU) * sin(lambdaU)  // User y-coordinate
        val uz = rUser * sin(phiU)                 // User z-coordinate

        // Calculate differences in each axis
        val dx = sx - ux
        val dy = sy - uy
        val dz = sz - uz

        // Return Euclidean distance
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
