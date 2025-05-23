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
// import com.cashruler.ui.screens.savings.SavingsTransactionFormScreen // Supprimé
import com.cashruler.ui.screens.savings.SavingsFormScreen
import com.cashruler.ui.screens.savings.SavingsProjectScreen
import com.cashruler.ui.screens.savings.SavingsScreen
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
                ExpensesScreen(
                    onNavigateToAddExpense = { navController.navigate(Routes.EXPENSE_FORM_NEW) },
                    onNavigateToEditExpense = { expenseId -> navController.navigate(Routes.expenseDetails(expenseId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.INCOME,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                IncomeScreen(
                    onNavigateToAddIncome = { navController.navigate(Routes.INCOME_FORM_NEW) },
                    onNavigateToEditIncome = { incomeId -> navController.navigate(Routes.incomeDetails(incomeId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.SAVINGS,
                enterTransition = { slideInHorizontally() },
                exitTransition = { slideOutHorizontally() }
            ) {
                SavingsScreen(
                    onNavigateToAddProject = { navController.navigate(Routes.SAVINGS_FORM_NEW) },
                    onNavigateToProjectDetails = { projectId -> navController.navigate(Routes.savingsProjectDetails(projectId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
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

            // Formulaires et Détails (regroupés pour clarté)
            composable(Routes.EXPENSE_FORM_NEW) {
                ExpenseFormScreen(expenseId = null, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EXPENSE_DETAILS, // Réutilise EXPENSE_DETAILS pour l'édition
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId")
                ExpenseFormScreen(expenseId = expenseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Routes.INCOME_FORM_NEW) {
                IncomeFormScreen(incomeId = null, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.INCOME_DETAILS, // Réutilise INCOME_DETAILS pour l'édition
                arguments = listOf(navArgument("incomeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val incomeId = backStackEntry.arguments?.getLong("incomeId")
                IncomeFormScreen(incomeId = incomeId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Routes.SAVINGS_FORM_NEW) {
                SavingsFormScreen(projectId = null, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.SAVINGS_FORM_EDIT, // Route pour éditer un projet existant
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId")
                SavingsFormScreen(projectId = projectId, onNavigateBack = { navController.popBackStack() })
            }


            // Projets d'épargne - Écran de détails
            composable(
                route = Routes.SAVINGS_PROJECT_DETAILS, // Renommé pour clarté vs SAVINGS_PROJECT
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
                SavingsProjectScreen(
                    projectId = projectId,
                    onNavigateToEditProject = { navController.navigate(Routes.savingsFormEdit(projectId)) },
                    onNavigateBack = { navController.popBackStack() }
                    // onNavigateToAddTransaction a été supprimé de SavingsProjectScreen
                )
            }

            // composable( // Route et écran supprimés
            //     route = Routes.SAVINGS_TRANSACTION,
            //     arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            // ) { backStackEntry ->
            //     val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            //     // SavingsTransactionFormScreen(navController, projectId) // Écran supprimé
            // }

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
