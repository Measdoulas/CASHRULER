package com.cashruler.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.StatisticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState() // Enum AnalysisPeriod
    val globalStats by viewModel.stats.collectAsState() // Data class GlobalStatistics
    val expensesByCategory by viewModel.expensesByCategory.collectAsState() // Map<String, Double>
    val incomesByType by viewModel.incomesByType.collectAsState() // Map<String, Double>
    val isLoading by viewModel.isLoading.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) } // Peut être retiré ou simplifié
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Collecte les messages d'erreur
    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = "Statistiques",
        navigateBack = onNavigateBack,
        actions = {
            // IconButton(onClick = { showFilterDialog = true }) { // Simplifié/Retiré
            //     Icon(
            //         imageVector = Icons.Default.FilterList,
            //         contentDescription = "Filtrer"
            //     )
            // }
            // IconButton(onClick = { /* viewModel.exportData() */ }) { // Supprimé
            //     Icon(
            //         imageVector = Icons.Default.Share,
            //         contentDescription = "Exporter"
            //     )
            // }
             IconButton(onClick = { viewModel.refresh() }) { // Ajout d'un bouton de rafraîchissement
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rafraîchir"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Sélecteur de période
            PeriodSelector( // Utilise le PeriodSelector adapté plus bas
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period -> viewModel.setPeriod(period) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                LoadingState(modifier = Modifier.fillMaxSize())
            } else {
                // Statistiques Globales
                GlobalStatsCard(stats = globalStats, modifier = Modifier.padding(16.dp))

                // Répartition des dépenses par catégorie
                if (expensesByCategory.isNotEmpty()) {
                    ListSection(
                        title = "Dépenses par catégorie",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Ici, on pourrait utiliser un PieChart si disponible
                        // Pour l'instant, affichage sous forme de liste:
                        Column {
                            expensesByCategory.forEach { (category, amount) ->
                                Text("$category: ${com.cashruler.util.formatCurrency(amount)}")
                            }
                        }
                        // Exemple avec un composant PieChart (si ChartComponents.PieChart existe)
                        // com.cashruler.ui.components.PieChart(
                        //     data = expensesByCategory.map { PieChartData(it.key, it.value.toFloat()) },
                        //     modifier = Modifier.height(200.dp).fillMaxWidth()
                        // )
                    }
                } else {
                    Text("Aucune dépense pour cette période.", modifier = Modifier.padding(16.dp))
                }
                
                // Répartition des revenus par type (Optionnel)
                if (incomesByType.isNotEmpty()) {
                     ListSection(
                        title = "Revenus par type",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column {
                            incomesByType.forEach { (type, amount) ->
                                Text("$type: ${com.cashruler.util.formatCurrency(amount)}")
                            }
                        }
                        // Exemple avec un composant PieChart (si ChartComponents.PieChart existe)
                        // com.cashruler.ui.components.PieChart(
                        //     data = incomesByType.map { PieChartData(it.key, it.value.toFloat()) },
                        //     modifier = Modifier.height(200.dp).fillMaxWidth()
                        // )
                    }
                } else {
                     Text("Aucun revenu pour cette période.", modifier = Modifier.padding(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Boîte de dialogue de filtres (simplifié ou à retirer)
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filtres") },
                text = { Text("Le filtrage avancé n'est pas encore implémenté pour cette vue.") },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) { Text("OK") }
                }
            )
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

@Composable
private fun PeriodSelector(
    selectedPeriod: com.cashruler.ui.viewmodels.AnalysisPeriod,
    onPeriodSelected: (com.cashruler.ui.viewmodels.AnalysisPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        com.cashruler.ui.viewmodels.AnalysisPeriod.values().forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.name.lowercase().replaceFirstChar { it.titlecase() }) } // ex: Week, Month
            )
        }
    }
}


@Composable
fun GlobalStatsCard(stats: com.cashruler.ui.viewmodels.GlobalStatistics, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Statistiques Globales", style = MaterialTheme.typography.titleLarge)
            Text("Revenu Total: ${com.cashruler.util.formatCurrency(stats.totalIncome)}")
            Text("Revenu Net Total: ${com.cashruler.util.formatCurrency(stats.totalNetIncome)}")
            Text("Impôts Totaux: ${com.cashruler.util.formatCurrency(stats.totalTaxes)}")
            Text("Dépenses Totales: ${com.cashruler.util.formatCurrency(stats.totalExpenses)}")
            Text("Solde: ${com.cashruler.util.formatCurrency(stats.balance)}")
            Divider()
            Text("Épargne Actuelle: ${com.cashruler.util.formatCurrency(stats.savingsAmount)} sur ${com.cashruler.util.formatCurrency(stats.savingsTarget)}")
            Text("Projets d'épargne actifs: ${stats.activeSavingsProjects}")
            Divider()
            Text("Limites de dépenses actives: ${stats.activeLimits}")
            Text("Limites dépassées: ${stats.exceededLimits}")
            // On pourrait ajouter les taux ici (getSavingsRate, getExpenseRate, etc.)
        }
    }
}
// Supprimer l'ancien enum PeriodType s'il n'est plus utilisé ailleurs.
// enum class PeriodType(val label: String) {
//     WEEK("Semaine"),
//     MONTH("Mois"),
//     YEAR("Année")
// }

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    MaterialTheme {
        StatisticsScreen(
            onNavigateBack = {}
        )
    }
}
