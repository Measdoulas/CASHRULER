package com.cashruler.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = "Paramètres",
        navigateBack = onNavigateBack,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Paramètres généraux
            SettingsSection(title = "Général") {
                SettingsSelector(
                    title = "Devise",
                    options = viewModel.availableCurrencies,
                    selectedOption = settings.currency,
                    onOptionSelected = { viewModel.updateCurrency(it) },
                    icon = Icons.Default.Euro
                )
                
                SettingsSelector(
                    title = "Premier jour de la semaine",
                    options = listOf("Lundi", "Dimanche"),
                    selectedOption = settings.firstDayOfWeek,
                    onOptionSelected = { viewModel.updateFirstDayOfWeek(it) },
                    icon = Icons.Default.CalendarToday
                )
            }

            // Notifications
            SettingsSection(title = "Notifications") {
                SettingsSwitch(
                    title = "Notifications de limites",
                    checked = settings.limitNotifications,
                    onCheckedChange = { viewModel.updateLimitNotifications(it) },
                    icon = Icons.Default.NotificationsActive,
                    subtitle = "Alertes quand les limites de dépenses sont dépassées"
                )

                SettingsSwitch(
                    title = "Rappels d'épargne",
                    checked = settings.savingsReminders,
                    onCheckedChange = { viewModel.updateSavingsReminders(it) },
                    icon = Icons.Default.Savings,
                    subtitle = "Rappels pour les objectifs d'épargne"
                )
            }

            // Affichage
            SettingsSection(title = "Affichage") {
                SettingsSelector(
                    title = "Thème",
                    options = listOf("Clair", "Sombre", "Système"),
                    selectedOption = settings.theme,
                    onOptionSelected = { viewModel.updateTheme(it) },
                    icon = Icons.Default.Palette
                )

                SettingsSwitch(
                    title = "Mode compact",
                    checked = settings.compactMode,
                    onCheckedChange = { viewModel.updateCompactMode(it) },
                    icon = Icons.Default.ViewCompact
                )
            }

            // Sécurité et données
            SettingsSection(title = "Sécurité et données") {
                SettingsItem(
                    title = "Sauvegarde",
                    subtitle = "Sauvegarder ou restaurer vos données",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToBackup
                )

                SettingsItem(
                    title = "Effacer les données",
                    subtitle = "Supprimer toutes les données de l'application",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearDataDialog = true }
                )
            }

            // À propos
            SettingsSection(title = "À propos") {
                SettingsItem(
                    title = "Version",
                    subtitle = viewModel.getAppVersion(context),
                    icon = Icons.Default.Info
                )

                SettingsItem(
                    title = "Licences",
                    subtitle = "Licences open source",
                    icon = Icons.Default.Article,
                    onClick = { /* TODO: Afficher les licences */ }
                )
            }
        }

        // Boîte de dialogue de confirmation pour l'effacement des données
        if (showClearDataDialog) {
            SettingsDialog(
                title = "Effacer les données",
                onDismiss = { showClearDataDialog = false },
                onConfirm = {
                    viewModel.clearAllData()
                    showClearDataDialog = false
                },
                confirmLabel = "Effacer",
                dismissLabel = "Annuler"
            ) {
                Text("Êtes-vous sûr de vouloir effacer toutes les données ? Cette action est irréversible.")
            }
        }

        // Message d'erreur
        showErrorMessage?.let { error ->
            MessageSnackbar(
                message = error,
                type = MessageType.ERROR,
                onDismiss = { showErrorMessage = null }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            onNavigateToBackup = {},
            onNavigateBack = {}
        )
    }
}
