package com.cashruler.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.cashruler.R

sealed class NavigationItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null
) {
    data object Dashboard : NavigationItem(
        route = Routes.DASHBOARD,
        titleResId = R.string.nav_dashboard,
        icon = Icons.Default.Dashboard
    )

    data object Expenses : NavigationItem(
        route = Routes.EXPENSES,
        titleResId = R.string.nav_expenses,
        icon = Icons.Default.Receipt
    )

    data object Income : NavigationItem(
        route = Routes.INCOME,
        titleResId = R.string.nav_income,
        icon = Icons.Default.AccountBalance
    )

    data object Savings : NavigationItem(
        route = Routes.SAVINGS,
        titleResId = R.string.nav_savings,
        icon = Icons.Default.Savings
    )

    data object Statistics : NavigationItem(
        route = Routes.STATISTICS,
        titleResId = R.string.nav_statistics,
        icon = Icons.Default.PieChart
    )

    data object Settings : NavigationItem(
        route = Routes.SETTINGS,
        titleResId = R.string.nav_settings,
        icon = Icons.Default.Settings
    )

    // Liste des items pour la navigation principale
    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Expenses,
            Income,
            Savings,
            Statistics
        )

        val drawerItems = listOf(
            Dashboard,
            Expenses,
            Income,
            Savings,
            Statistics,
            Settings
        )
    }
}

// Éléments de navigation pour les paramètres
sealed class SettingsNavigationItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    data object Backup : SettingsNavigationItem(
        route = Routes.BACKUP,
        titleResId = R.string.settings_backup,
        icon = Icons.Default.Backup
    )

    data object Notifications : SettingsNavigationItem(
        route = Routes.NOTIFICATIONS,
        titleResId = R.string.settings_notifications,
        icon = Icons.Default.Notifications
    )

    data object Privacy : SettingsNavigationItem(
        route = Routes.PRIVACY,
        titleResId = R.string.settings_privacy,
        icon = Icons.Default.Security
    )

    data object About : SettingsNavigationItem(
        route = Routes.ABOUT,
        titleResId = R.string.settings_about,
        icon = Icons.Default.Info
    )

    companion object {
        val items = listOf(
            Backup,
            Notifications,
            Privacy,
            About
        )
    }
}
