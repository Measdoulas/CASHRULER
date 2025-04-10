package com.cashruler.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cashruler.ui.animations.slideInHorizontally
import com.cashruler.ui.animations.slideOutHorizontally
import com.cashruler.ui.screens.backup.BackupScreen
import com.cashruler.ui.screens.dashboard.DashboardScreen
import com.cashruler.ui.screens.expenses.ExpenseFormScreen
import com.cashruler.ui.screens.expenses.ExpensesScreen
import com.cashruler.ui.screens.income.IncomeFormScreen
import com.cashruler.ui.screens.income.IncomeScreen
import com.cashruler.ui.screens.savings.*
import com.cashruler.ui.screens.settings.SettingsScreen
import com.cashruler.ui.screens.statistics.StatisticsScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(0) }
    val items = NavigationItem.bottomNavItems

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(text = item.route) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        badge = {
                            if (item.badgeCount != null) {
                                Badge { Text(item.badgeCount.toString()) }
                            } else if (item.hasNews) {
                                Badge()
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Écrans principaux
            composable(
                route = Routes.DASHBOARD,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                DashboardScreen(navController)
            }

            composable(
                route = Routes.EXPENSES,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                ExpensesScreen(navController)
            }

            composable(
                route = Routes.INCOME,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                IncomeScreen(navController)
            }

            composable(
                route = Routes.SAVINGS,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                SavingsScreen(navController)
            }

            composable(
                route = Routes.STATISTICS,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                StatisticsScreen(navController)
            }

            composable(
                route = Routes.SETTINGS,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                SettingsScreen(navController)
            }

            // Formulaires
            composable(Routes.EXPENSE_FORM) {
                ExpenseFormScreen(navController)
            }

            composable(Routes.INCOME_FORM) {
                IncomeFormScreen(navController)
            }

            composable(Routes.SAVINGS_FORM) {
                SavingsFormScreen(navController)
            }

            // Détails
            composable(
                route = Routes.EXPENSE_DETAILS,
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: return@composable
                ExpenseFormScreen(navController, expenseId)
            }

            composable(
                route = Routes.INCOME_DETAILS,
                arguments = listOf(navArgument("incomeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val incomeId = backStackEntry.arguments?.getLong("incomeId") ?: return@composable
                IncomeFormScreen(navController, incomeId)
            }

            // Projets d'épargne
            composable(
                route = Routes.SAVINGS_PROJECT,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                SavingsProjectScreen(navController, projectId)
            }

            composable(
                route = Routes.SAVINGS_TRANSACTION,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                SavingsTransactionFormScreen(navController, projectId)
            }

            // Paramètres
            settingsGraph(navController)
        }
    }
}

private fun NavGraphBuilder.settingsGraph(navController: NavController) {
    // Écrans de paramètres
    composable(Routes.BACKUP) {
        BackupScreen(navController)
    }

    composable(Routes.NOTIFICATIONS) {
        NotificationsScreen(navController)
    }

    composable(Routes.PRIVACY) {
        PrivacyScreen(navController)
    }

    composable(Routes.ABOUT) {
        AboutScreen(navController)
    }
}
