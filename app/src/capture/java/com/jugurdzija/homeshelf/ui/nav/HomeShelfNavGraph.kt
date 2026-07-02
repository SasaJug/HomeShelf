package com.jugurdzija.homeshelf.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jugurdzija.homeshelf.ui.edit.EditScreen
import com.jugurdzija.homeshelf.ui.golden.GoldenCaptureScreen
import com.jugurdzija.homeshelf.ui.golden.GoldenManageScreen
import com.jugurdzija.homeshelf.ui.golden.GoldenSaveScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen
import com.jugurdzija.homeshelf.ui.scan.ScanScreen
import com.jugurdzija.homeshelf.ui.settings.SettingsScreen
import com.jugurdzija.homeshelf.ui.test.TestResultsScreen
import com.jugurdzija.homeshelf.ui.test.TestRunScreen
import com.jugurdzija.homeshelf.ui.test.TestSelectScreen
import com.jugurdzija.homeshelf.ui.test.TestViewModel

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
                    navController.navigate(Routes.edit(storageId))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onCaptureTestPhoto = { storageId ->
                    navController.navigate(Routes.goldenCapture(storageId))
                }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReview = { storageId ->
                    navController.navigate(Routes.edit(storageId))
                },
                onNavigateToEdit = { storageId ->
                    navController.navigate(Routes.edit(storageId))
                }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("storageId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) {
            EditScreen(
                onSaved = { navController.popBackStack(Routes.REFERENCE, inclusive = false) },
                onDiscarded = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.GOLDEN_CAPTURE,
            arguments = listOf(navArgument("storageId") { type = NavType.StringType })
        ) {
            GoldenCaptureScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSave = { navController.navigate(Routes.GOLDEN_SAVE) }
            )
        }
        composable(Routes.GOLDEN_SAVE) {
            GoldenSaveScreen(
                onBack = { navController.popBackStack(Routes.REFERENCE, inclusive = false) }
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
        composable(Routes.TEST_SELECT) {
            val vm: TestViewModel = hiltViewModel()
            TestSelectScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRun = { navController.navigate(Routes.TEST_RUN) },
                vm = vm
            )
        }
        composable(Routes.TEST_RUN) { backStackEntry ->
            val selectEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.TEST_SELECT)
            }
            val vm: TestViewModel = hiltViewModel(selectEntry)
            TestRunScreen(
                onNavigateToResults = { navController.navigate(Routes.TEST_RESULTS) },
                vm = vm
            )
        }
        composable(Routes.TEST_RESULTS) { backStackEntry ->
            val selectEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.TEST_SELECT)
            }
            val vm: TestViewModel = hiltViewModel(selectEntry)
            TestResultsScreen(
                onBack = { navController.popBackStack(Routes.REFERENCE, inclusive = false) },
                vm = vm
            )
        }
    }
}
