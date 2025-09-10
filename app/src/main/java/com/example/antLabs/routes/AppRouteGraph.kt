package com.example.antLabs.routes

import GNSSInfo
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.antLabs.engine.GnssPermHandler
import com.example.antLabs.views.Accelerometer
import com.example.antLabs.views.Earthquake
import com.example.antLabs.views.GNSS
import com.example.antLabs.views.Gyroscope
import com.example.antLabs.views.Magnetometer
import com.example.antLabs.views.Menu

object Routes {
    const val MENU = "menu"
    const val GYRO = "gyro"
    const val MAGNETO = "magneto"
    const val GNSS_ROUTE = "gnss"
    const val ACCEL = "accel"
    const val EARTH = "earth"
    const val GNSSINFO = "GNSSInfo" // new route
}


@Composable
fun AppRouteGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) { Menu(navController) }
        composable(Routes.GYRO) { Gyroscope() }
        composable(Routes.MAGNETO) { Magnetometer() }
        composable(Routes.ACCEL) { Accelerometer() }
        composable(Routes.EARTH) { Earthquake() }

        composable(Routes.GNSS_ROUTE) {
            GnssPermHandler {
                GNSS(
                    onInfoClick = { navController.navigate(Routes.GNSSINFO) } // hook info button
                )
            }
        }

        composable(Routes.GNSSINFO) {
            GNSSInfo(
                onBack = { navController.popBackStack() } // back navigation
            )
        }
    }
}
