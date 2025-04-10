package com.cashruler.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.data.models.Expense
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.ExpenseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Expense?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = "Dépenses",
        navigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtrer"
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddExpense,
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
        } else if (expenses.isEmpty()) {
            EmptyListMessage(
                message = "Aucune dépense enregistrée",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )
        } else {
            TransactionList(
                items = expenses,
                groupBy = { it.date },
                key = { it.id },
                modifier = Modifier.padding(paddingValues)
            ) { expense ->
                TransactionListItem(
                    label = expense.description,
                    amount = -expense.amount,
                    description = expense.category,
                    date = expense.date,
                    isPositiveGood = false,
                    onClick = { onNavigateToEditExpense(expense.id) },
                    trailing = {
                        IconButton(onClick = { showDeleteDialog = expense }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer"
                            )
                        }
                    }
                )
            }
        }

        // Boîte de dialogue de confirmation de suppression
        showDeleteDialog?.let { expense ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Supprimer la dépense") },
                text = { Text("Voulez-vous vraiment supprimer cette dépense ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteExpense(expense)
                            showDeleteDialog = null
                            showSuccessMessage = true
                        }
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
                message = "Dépense supprimée avec succès",
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
fun ExpensesScreenPreview() {
    MaterialTheme {
        ExpensesScreen(
            onNavigateToAddExpense = {},
            onNavigateToEditExpense = {},
            onNavigateBack = {}
        )
    }
}
