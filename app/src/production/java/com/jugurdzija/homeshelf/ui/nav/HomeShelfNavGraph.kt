package com.jugurdzija.homeshelf.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jugurdzija.homeshelf.ui.compare.CompareScreen
import com.jugurdzija.homeshelf.ui.detail.AlignedDetailScreen
import com.jugurdzija.homeshelf.ui.detail.BitmapDetailHolder
import com.jugurdzija.homeshelf.ui.detail.ImageDetailScreen
import com.jugurdzija.homeshelf.ui.edit.EditScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceCaptureScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceViewModel
import com.jugurdzija.homeshelf.ui.scan.ScanScreen
import com.jugurdzija.homeshelf.ui.settings.SettingsScreen

@Composable
fun HomeShelfNavGraph(
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.REFERENCE
    ) {
        composable(Routes.REFERENCE) {
            ReferenceScreen(
                onNavigateToEdit = { storageId ->
                    navController.navigate("edit?storageId=$storageId")
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToScan = { navController.navigate(Routes.SCAN) }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { storageId ->
                    navController.navigate(if (storageId != null) "edit?storageId=$storageId" else "edit")
                }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("storageId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val storageId = backStackEntry.arguments?.getString("storageId")
            EditScreen(
                storageId = storageId,
                onSaved = { navController.popBackStack(Routes.REFERENCE, inclusive = false) },
                onDiscarded = { navController.popBackStack() }
            )
        }
        composable(Routes.REFERENCE_CAPTURE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.REFERENCE)
            }
            val vm: ReferenceViewModel = hiltViewModel(parentEntry)
            ReferenceCaptureScreen(
                onBack = { navController.popBackStack() },
                vm = vm
            )
        }
        composable(Routes.COMPARE) {
            CompareScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Routes.DETAIL_BITMAP) }
            )
        }
        composable(
            route = Routes.DETAIL_FILE,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = Uri.decode(encoded)
            ImageDetailScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable(Routes.DETAIL_BITMAP) {
            if (BitmapDetailHolder.capturedBitmap != null) {
                AlignedDetailScreen(onBack = { navController.popBackStack() })
            } else {
                navController.popBackStack()
            }
        }
    }
}
