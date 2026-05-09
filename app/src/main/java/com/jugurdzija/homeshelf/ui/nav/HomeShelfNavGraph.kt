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
import com.jugurdzija.homeshelf.ui.detail.BitmapDetailHolder
import com.jugurdzija.homeshelf.ui.detail.ImageDetailScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceCaptureScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceViewModel

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
                onNavigateToCapture = { navController.navigate(Routes.REFERENCE_CAPTURE) },
                onNavigateToCompare = { navController.navigate(Routes.COMPARE) },
                onNavigateToDetail = { filePath ->
                    navController.navigate("detail/file/${Uri.encode(filePath)}")
                }
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
        composable(Routes.DETAIL_BITMAP) {
            val bitmap = BitmapDetailHolder.pending
            if (bitmap != null) {
                ImageDetailScreen(
                    bitmap = bitmap,
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
