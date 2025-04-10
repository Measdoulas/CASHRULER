package com.cashruler.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.monthlyStats.collectAsState()
    val activeLimits by viewModel.exceededLimits.collectAsState()
    val upcomingIncomes by viewModel.upcomingIncomes.collectAsState()
    val savingsProjects by viewModel.activeSavingsProjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            errorMessage = error
            showErrorDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de bord") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            LoadingState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Résumé mensuel
                item {
                    MonthlySummaryCard(
                        income = uiState.income,
                        expenses = uiState.expenses,
                        balance = uiState.balance,
                        previousMonthIncome = uiState.previousMonthIncome,
                        previousMonthExpenses = uiState.previousMonthExpenses
                    )
                }

                // Actions rapides
                item {
                    QuickActions(
                        onAddExpense = onNavigateToExpenses,
                        onAddIncome = onNavigateToIncome,
                        onViewStatistics = onNavigateToStatistics
                    )
                }

                // Limites dépassées
                if (activeLimits.isNotEmpty()) {
                    item {
                        ListSection(
                            title = "Limites dépassées",
                            subtitle = "${activeLimits.size} limites nécessitent votre attention"
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(activeLimits) { limit ->
                                    SpendingLimitCard(
                                        category = limit.category,
                                        currentAmount = limit.currentSpent,
                                        limitAmount = limit.amount,
                                        modifier = Modifier.width(300.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Projets d'épargne
                if (savingsProjects.isNotEmpty()) {
                    item {
                        ListSection(
                            title = "Projets d'épargne",
                            action = {
                                TextButton(onClick = onNavigateToSavings) {
                                    Text("Voir tout")
                                }
                            }
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(savingsProjects) { project ->
                                    SavingsProjectCard(
                                        title = project.title,
                                        currentAmount = project.currentAmount,
                                        targetAmount = project.targetAmount,
                                        modifier = Modifier.width(300.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Revenus à venir
                if (upcomingIncomes.isNotEmpty()) {
                    item {
                        ListSection(
                            title = "Revenus à venir",
                            subtitle = "Dans les 30 prochains jours"
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                upcomingIncomes.take(3).forEach { income ->
                                    TransactionListItem(
                                        label = income.description,
                                        amount = income.amount,
                                        description = income.type,
                                        date = income.date
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Erreur") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickActions(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onViewStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FloatingActionButton(
            onClick = onAddExpense,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Icon(
                imageVector = Icons.Default.RemoveCircle,
                contentDescription = "Ajouter une dépense"
            )
        }

        FloatingActionButton(
            onClick = onAddIncome,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Ajouter un revenu"
            )
        }

        FloatingActionButton(
            onClick = onViewStatistics,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = "Voir les statistiques"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen(
            onNavigateToExpenses = {},
            onNavigateToIncome = {},
            onNavigateToSavings = {},
            onNavigateToStatistics = {},
            onNavigateToSettings = {}
        )
    }
}
