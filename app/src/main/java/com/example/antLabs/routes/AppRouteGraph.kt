package com.example.antLabs.routes

import GNSSInfo
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.antLabs.perms.GNSSPermHandler
import com.example.antLabs.views.GNSS

object Routes {
    const val GNSS_ROUTE = "gnss"
    const val GNSSINFO = "GNSSInfo" // new route
}


@Composable
fun AppRouteGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.GNSS_ROUTE) {

        composable(Routes.GNSS_ROUTE) {
            GNSSPermHandler {
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
