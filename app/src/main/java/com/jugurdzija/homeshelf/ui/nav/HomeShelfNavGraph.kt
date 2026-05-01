package com.jugurdzija.homeshelf.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jugurdzija.homeshelf.ui.compare.CompareScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen

@Composable
fun HomeShelfNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.REFERENCE
    ) {
        composable(Routes.REFERENCE) {
            ReferenceScreen(
                onNavigateToCompare = { navController.navigate(Routes.COMPARE) }
            )
        }
        composable(Routes.COMPARE) {
            CompareScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
