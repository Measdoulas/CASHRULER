package com.cashruler.ui.screens.income

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.data.models.Income
import com.cashruler.ui.components.*
import com.cashruler.ui.theme.CashRulerTheme
import com.cashruler.ui.viewmodels.IncomeViewModel
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IncomeScreen(
    onNavigateToAddIncome: () -> Unit,
    onNavigateToEditIncome: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val incomes by viewModel.allIncomes.collectAsState()
    val types by viewModel.allTypes.collectAsState()
    val upcomingIncomes by viewModel.upcomingRecurringIncomes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Income?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }

    // Calcul des statistiques
    val statistics = remember(incomes) {
        incomes.fold(
            Triple(0.0, 0.0, 0)
        ) { (total, net, recurring), income ->
            Triple(
                total + income.amount,
                net + income.getNetAmount(),
                if (income.isRecurring) recurring + 1 else recurring
            )
        }
    }

    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = "Revenus",
        navigateBack = onNavigateBack,
        actions = {
            // Menu de filtrage par type
            var showFilterMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showFilterMenu = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtrer"
                )
            }
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Tous les types") },
                    onClick = {
                        selectedType = null
                        showFilterMenu = false
                    }
                )
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedType = type
                            showFilterMenu = false
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddIncome,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Ajouter") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            LoadingState()
        } else if (incomes.isEmpty() && upcomingIncomes.isEmpty()) {
            EmptyListMessage(
                message = "Aucun revenu enregistré",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Résumé
                item {
                    IncomeSummaryCard(
                        totalIncome = statistics.first,
                        totalNetIncome = statistics.second,
                        totalTaxes = statistics.first - statistics.second,
                        recurring = statistics.third
                    )
                }

                // Revenus à venir
                if (upcomingIncomes.isNotEmpty()) {
                    item {
                        ListSection(
                            title = "Revenus à venir",
                            subtitle = "Dans les 30 prochains jours",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    items(
                        items = upcomingIncomes.filter { income ->
                            selectedType?.let { it == income.type } ?: true
                        },
                        key = { it.id }
                    ) { income ->
                        IncomeListItem(
                            income = income,
                            onClick = { onNavigateToEditIncome(income.id) },
                            onDelete = { showDeleteDialog = income }
                        )
                    }
                }

                // Liste des revenus groupés par mois
                val dateFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.FRANCE) }
                val groupedIncomes = remember(incomes, selectedType) {
                    incomes
                        .filter { income -> selectedType?.let { it == income.type } ?: true }
                        .groupBy { income ->
                            val calendar = Calendar.getInstance().apply { time = income.date }
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            calendar.time
                        }
                        .toSortedMap(compareByDescending { it })
                }

                groupedIncomes.forEach { (month, monthIncomes) ->
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListSection(
                                title = dateFormat.format(month).replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }

                    items(
                        items = monthIncomes,
                        key = { it.id }
                    ) { income ->
                        IncomeListItem(
                            income = income,
                            onClick = { onNavigateToEditIncome(income.id) },
                            onDelete = { showDeleteDialog = income }
                        )
                    }
                }

                // Espace en bas pour le FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Boîte de dialogue de confirmation de suppression
        showDeleteDialog?.let { income ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Supprimer le revenu") },
                text = { 
                    Text(
                        if (income.isRecurring)
                            "Voulez-vous vraiment supprimer ce revenu récurrent ?"
                        else
                            "Voulez-vous vraiment supprimer ce revenu ?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteIncome(income)
                            showDeleteDialog = null
                            showSuccessMessage = true
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Message de succès
        if (showSuccessMessage) {
            MessageSnackbar(
                message = "Revenu supprimé avec succès",
                type = MessageType.SUCCESS,
                onDismiss = { showSuccessMessage = false }
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

@Preview(showBackground = true)
@Composable
fun IncomeScreenPreview() {
    CashRulerTheme {
        IncomeScreen(
            onNavigateToAddIncome = {},
            onNavigateToEditIncome = {},
            onNavigateBack = {}
        )
    }
}
