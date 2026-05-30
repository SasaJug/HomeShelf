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
import com.jugurdzija.homeshelf.ui.compare.CompareViewModel
import com.jugurdzija.homeshelf.ui.detail.ImageDetailScreen
import com.jugurdzija.homeshelf.ui.golden.GoldenCaptureHolder
import com.jugurdzija.homeshelf.ui.golden.GoldenManageScreen
import com.jugurdzija.homeshelf.ui.golden.GoldenSaveScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceCaptureScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceViewModel
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
                onNavigateToCapture = { navController.navigate(Routes.REFERENCE_CAPTURE) },
                onNavigateToCompare = { navController.navigate(Routes.COMPARE) },
                onNavigateToDetail = { filePath ->
                    navController.navigate("detail/file/${Uri.encode(filePath)}")
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToManage = { navController.navigate(Routes.GOLDEN_MANAGE) }
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
                onNavigateToGoldenSave = { navController.navigate(Routes.GOLDEN_SAVE) }
            )
        }
        composable(Routes.GOLDEN_SAVE) { backStackEntry ->
            val compareEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.COMPARE)
            }
            val compareVm: CompareViewModel = hiltViewModel(compareEntry)
            if (GoldenCaptureHolder.bitmap != null) {
                GoldenSaveScreen(
                    onBack = {
                        compareVm.onScanAgain()
                        navController.popBackStack(Routes.REFERENCE, inclusive = false)
                    }
                )
            } else {
                compareVm.onScanAgain()
                navController.popBackStack(Routes.REFERENCE, inclusive = false)
            }
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
        composable(Routes.GOLDEN_MANAGE) {
            GoldenManageScreen(
                onBack = { navController.popBackStack() },
                onNavigateToView = { navController.navigate(Routes.GOLDEN_VIEW) }
            )
        }
        composable(Routes.GOLDEN_VIEW) {
            GoldenSaveScreen(
                onBack = { navController.popBackStack() },
                readOnly = true
            )
        }
    }
}
