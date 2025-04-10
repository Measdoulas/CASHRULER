package com.cashruler.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.data.models.SavingsTransaction
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.SavingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsProjectScreen(
    projectId: Long,
    onNavigateToEditProject: (Long) -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val project by viewModel.currentProject.collectAsState()
    val transactions by viewModel.projectTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteTransactionDialog by remember { mutableStateOf<SavingsTransaction?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Charge les données du projet
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        viewModel.loadProjectTransactions(projectId)
    }

    // Collecte les messages d'erreur
    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = project?.title ?: "Projet d'épargne",
        navigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { onNavigateToEditProject(projectId) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifier"
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer"
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddTransaction(projectId) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Nouvelle transaction") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            project?.let { project ->
                // Carte de résumé
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressChart(
                            progress = (project.currentAmount / project.targetAmount).toFloat(),
                            label = "${formatAmount(project.currentAmount)} sur ${formatAmount(project.targetAmount)}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        if (project.hasDeadline) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Échéance: ${formatDate(project.deadline!!)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (project.hasAutoSaving) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Épargne automatique: ${formatAmount(project.autoSavingAmount!!)} tous les ${project.autoSavingFrequency} jours",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Liste des transactions
                if (isLoading) {
                    LoadingState()
                } else if (transactions.isEmpty()) {
                    EmptyListMessage(
                        message = "Aucune transaction",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    )
                } else {
                    TransactionList(
                        items = transactions,
                        groupBy = { it.date },
                        key = { it.id }
                    ) { transaction ->
                        TransactionListItem(
                            label = transaction.description,
                            amount = transaction.amount,
                            date = transaction.date,
                            trailing = {
                                IconButton(onClick = { showDeleteTransactionDialog = transaction }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Boîte de dialogue de suppression du projet
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Supprimer le projet") },
                text = {
                    Text("Voulez-vous vraiment supprimer ce projet et toutes ses transactions ?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            project?.let { viewModel.deleteProject(it) }
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Boîte de dialogue de suppression de transaction
        showDeleteTransactionDialog?.let { transaction ->
            AlertDialog(
                onDismissRequest = { showDeleteTransactionDialog = null },
                title = { Text("Supprimer la transaction") },
                text = { Text("Voulez-vous vraiment supprimer cette transaction ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(transaction)
                            showDeleteTransactionDialog = null
                            showSuccessMessage = true
                        }
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTransactionDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Messages
        if (showSuccessMessage) {
            MessageSnackbar(
                message = "Transaction supprimée avec succès",
                type = MessageType.SUCCESS,
                onDismiss = { showSuccessMessage = false }
            )
        }

        showErrorMessage?.let { error ->
            MessageSnackbar(
                message = error,
                type = MessageType.ERROR,
                onDismiss = { showErrorMessage = null }
            )
        }
    }
}

/**
 * Formatte un montant (à remplacer par l'utilitaire approprié)
 */
private fun formatAmount(amount: Double): String {
    return "%.2f €".format(amount)
}

/**
 * Formatte une date (à remplacer par l'utilitaire approprié)
 */
private fun formatDate(date: Date): String {
    return date.toString()
}

@Preview(showBackground = true)
@Composable
fun SavingsProjectScreenPreview() {
    MaterialTheme {
        SavingsProjectScreen(
            projectId = 1L,
            onNavigateToEditProject = {},
            onNavigateToAddTransaction = {},
            onNavigateBack = {}
        )
    }
}
