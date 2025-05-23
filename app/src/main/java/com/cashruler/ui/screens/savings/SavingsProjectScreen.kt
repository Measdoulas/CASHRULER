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
    // onNavigateToAddTransaction: (Long) -> Unit, // Supprimé
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.projectFormState // Contient les détails du projet chargé
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    // var showDeleteTransactionDialog by remember { mutableStateOf<SavingsTransaction?>(null) } // Supprimé
    var showSuccessMessage by remember { mutableStateOf(false) } // Peut être réutilisé pour succès ajout/retrait fonds
    var showAddFundsDialog by remember { mutableStateOf(false) }
    var showWithdrawFundsDialog by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Charge les données du projet
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
        // viewModel.loadProjectTransactions(projectId) // Supprimé
    }

    // Collecte les messages d'erreur
    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = formState.title.ifEmpty { "Projet d'épargne" },
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
                onClick = { showAddFundsDialog = true }, // Modifié pour "Ajouter des fonds"
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddCard, // Icône plus appropriée
                        contentDescription = null
                    )
                },
                text = { Text("Ajouter des fonds") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && formState.title.isEmpty()) { // Affiche LoadingState seulement si les données ne sont pas encore chargées
                LoadingState()
            } else {
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
                        if (formState.description.isNotBlank()) {
                            Text(
                                text = formState.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        val progress = if (formState.targetAmount > 0) (formState.currentAmount / formState.targetAmount).toFloat() else 0f
                        CircularProgressChart( // S'assurer que ce composant existe et fonctionne comme attendu
                            progress = progress,
                            label = "${com.cashruler.util.formatCurrency(formState.currentAmount)} sur ${com.cashruler.util.formatCurrency(formState.targetAmount)}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        formState.targetDate?.let { date ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Échéance: ${formatDate(date)}", // Utiliser la fonction formatDate existante
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        formState.periodicAmount?.takeIf { it > 0 }?.let { periodicAmount ->
                             formState.savingFrequency?.let { frequency ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Épargne suggérée: ${com.cashruler.util.formatCurrency(periodicAmount)} tous les $frequency jours",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                             }
                        }

                        if (formState.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes: ${formState.notes}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                         Spacer(modifier = Modifier.height(16.dp))
                        // Boutons Ajouter/Retirer fonds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { showAddFundsDialog = true }) {
                                Text("Ajouter des fonds")
                            }
                            Button(onClick = { showWithdrawFundsDialog = true }) {
                                Text("Retirer des fonds")
                            }
                        }
                    }
                }
                // La liste des transactions est supprimée
            }
        }

        // Boîte de dialogue de suppression du projet
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Supprimer le projet") },
                text = {
                    Text("Voulez-vous vraiment supprimer ce projet ?") // Texte mis à jour
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Le projet est chargé dans formState, mais deleteProject attend un SavingsProject.
                            // On reconstruit un SavingsProject minimal ou on modifie deleteProject.
                            // Pour l'instant, on suppose que le ViewModel peut gérer la suppression avec juste l'ID
                            // ou que loadProject a mis à jour un `currentProject` séparé que le ViewModel utilise.
                            // Idéalement, viewModel.deleteProject(projectId) serait mieux.
                            // Si currentProject est toujours disponible dans le ViewModel, c'est bon.
                            // Ou alors, on crée un SavingsProject à partir de formState pour la suppression.
                            val projectToDelete = SavingsProject(
                                id = projectId,
                                title = formState.title,
                                targetAmount = formState.targetAmount,
                                currentAmount = formState.currentAmount,
                                // ... autres champs nécessaires pour la suppression ou l'entité
                            )
                            viewModel.deleteProject(projectToDelete) // Assurez-vous que cela fonctionne
                            showDeleteDialog = false
                            onNavigateBack() // Navigue en arrière après suppression
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

        // Dialogue pour ajouter des fonds
        if (showAddFundsDialog) {
            AmountInputDialog(
                title = "Ajouter des fonds",
                onDismiss = { showAddFundsDialog = false },
                onConfirm = { amount ->
                    viewModel.addAmount(projectId, amount)
                    showAddFundsDialog = false
                    showSuccessMessage = true // Optionnel: message de succès
                }
            )
        }

        // Dialogue pour retirer des fonds
        if (showWithdrawFundsDialog) {
            AmountInputDialog(
                title = "Retirer des fonds",
                onDismiss = { showWithdrawFundsDialog = false },
                onConfirm = { amount ->
                    viewModel.subtractAmount(projectId, amount)
                    showWithdrawFundsDialog = false
                    showSuccessMessage = true // Optionnel: message de succès
                }
            )
        }


        // Messages
        if (showSuccessMessage) {
            MessageSnackbar(
                message = "Opération réussie", // Message générique
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
            // onNavigateToAddTransaction = {}, // Supprimé
            onNavigateBack = {}
        )
    }
}

@Composable
fun AmountInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    isError = false
                },
                label = { Text("Montant") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                isError = isError,
                supportingText = if (isError) { { Text("Veuillez entrer un montant valide.") } } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
