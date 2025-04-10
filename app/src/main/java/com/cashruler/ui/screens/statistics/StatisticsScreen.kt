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
    val currentPeriod by viewModel.currentPeriod.collectAsState()
    val previousPeriod by viewModel.previousPeriod.collectAsState()
    val expenseDistribution by viewModel.expenseDistribution.collectAsState()
    val monthlyTrends by viewModel.monthlyTrends.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
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
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtrer"
                )
            }
            IconButton(onClick = { viewModel.exportData() }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Exporter"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Sélecteur de période
                PeriodSelector(
                    selectedPeriod = viewModel.selectedPeriodType.value,
                    onPeriodSelected = { viewModel.updatePeriodType(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Résumé de la période
                currentPeriod?.let { current ->
                    PeriodSummaryCard(
                        income = current.income,
                        expenses = current.expenses,
                        savings = current.savings,
                        periodLabel = current.label,
                        previousPeriodIncome = previousPeriod?.income,
                        previousPeriodExpenses = previousPeriod?.expenses,
                        previousPeriodSavings = previousPeriod?.savings,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Répartition des dépenses
                if (expenseDistribution.isNotEmpty()) {
                    ExpenseDistributionChart(
                        data = expenseDistribution,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Évolution des dépenses
                if (monthlyTrends.isNotEmpty()) {
                    ExpenseTrendChart(
                        monthlyExpenses = monthlyTrends,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Objectifs d'épargne
                if (savingsGoals.isNotEmpty()) {
                    ListSection(
                        title = "Objectifs d'épargne",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savingsGoals.forEach { goal ->
                                GoalProgressIndicator(
                                    currentValue = goal.currentAmount,
                                    targetValue = goal.targetAmount,
                                    title = goal.title
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Boîte de dialogue de filtres
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filtres") },
                text = {
                    Column {
                        // TODO: Ajouter les options de filtrage
                        Text("Options de filtrage à implémenter")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // TODO: Appliquer les filtres
                            showFilterDialog = false
                        }
                    ) {
                        Text("Appliquer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("Annuler")
                    }
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
    selectedPeriod: PeriodType,
    onPeriodSelected: (PeriodType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PeriodType.values().forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) }
            )
        }
    }
}

enum class PeriodType(val label: String) {
    WEEK("Semaine"),
    MONTH("Mois"),
    YEAR("Année")
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    MaterialTheme {
        StatisticsScreen(
            onNavigateBack = {}
        )
    }
}
