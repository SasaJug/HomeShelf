package com.jugurdzija.homeshelf.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jugurdzija.homeshelf.ui.edit.EditScreen
import com.jugurdzija.homeshelf.ui.markitems.MarkItemsScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceDetailScreen
import com.jugurdzija.homeshelf.ui.reference.ReferenceScreen
import com.jugurdzija.homeshelf.ui.review.ReviewScreen
import com.jugurdzija.homeshelf.ui.scan.ScanScreen
import com.jugurdzija.homeshelf.ui.settings.SettingsScreen
import com.jugurdzija.homeshelf.ui.shoppinglist.ShoppingListItemDetailScreen
import com.jugurdzija.homeshelf.ui.shoppinglist.ShoppingListScreen

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
                onNavigateToDetail = { storageId ->
                    navController.navigate(Routes.referenceDetail(storageId))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToScan = { navController.navigate(Routes.scan()) },
                onNavigateToShoppingList = { navController.navigate(Routes.SHOPPING_LIST) }
            )
        }
        composable(
            route = Routes.REFERENCE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_STORAGE_ID) { type = NavType.StringType })
        ) {
            ReferenceDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { storageId ->
                    navController.navigate(Routes.edit(storageId))
                },
                onMarkItems = { storageId ->
                    navController.navigate(Routes.markItems(storageId))
                },
                onRescan = { storageId ->
                    navController.navigate(Routes.scan(storageId))
                },
                onDeleted = { navController.popBackStack(Routes.REFERENCE, inclusive = false) }
            )
        }
        composable(
            route = Routes.SCAN,
            arguments = listOf(navArgument(Routes.ARG_STORAGE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onNavigateToReview = { storageId ->
                    navController.navigate(Routes.review(storageId))
                },
                onNavigateToEdit = { storageId ->
                    navController.navigate(Routes.edit(storageId))
                }
            )
        }
        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument(Routes.ARG_STORAGE_ID) { type = NavType.StringType })
        ) {
            ReviewScreen(
                onToReference = { navController.popBackStack(Routes.REFERENCE, inclusive = false) }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument(Routes.ARG_STORAGE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            EditScreen(
                onSaved = { navController.popBackStack(Routes.REFERENCE, inclusive = false) },
                onDiscarded = { navController.popBackStack(Routes.REFERENCE, inclusive = false) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable(
            route = Routes.MARK_ITEMS,
            arguments = listOf(navArgument(Routes.ARG_STORAGE_ID) { type = NavType.StringType })
        ) {
            MarkItemsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHOPPING_LIST) {
            ShoppingListScreen(
                onBack = { navController.popBackStack() },
                onItemClick = { itemId ->
                    navController.navigate(Routes.shoppingListItem(itemId))
                }
            )
        }
        composable(
            route = Routes.SHOPPING_LIST_ITEM,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType })
        ) {
            ShoppingListItemDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
