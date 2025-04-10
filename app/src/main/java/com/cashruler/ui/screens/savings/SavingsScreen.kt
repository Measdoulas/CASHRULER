package com.cashruler.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.data.models.SavingsProject
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.SavingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onNavigateToAddProject: () -> Unit,
    onNavigateToProjectDetails: (Long) -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val projects by viewModel.allProjects.collectAsState()
    val totalSaved by viewModel.totalSaved.collectAsState()
    val totalTarget by viewModel.totalTarget.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<SavingsProject?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    BaseScreen(
        title = "Épargne",
        navigateBack = onNavigateBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddProject,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text("Nouveau projet") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Résumé global
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
                        text = "Total épargné",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedMoneyDisplay(
                            amount = totalSaved,
                            textStyle = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "sur ${formatAmount(totalTarget)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isLoading) {
                LoadingState()
            } else if (projects.isEmpty()) {
                EmptyListMessage(
                    message = "Aucun projet d'épargne",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                )
            } else {
                // Liste des projets
                projects.forEach { project ->
                    SavingsProjectCard(
                        title = project.title,
                        currentAmount = project.currentAmount,
                        targetAmount = project.targetAmount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onCardClick = { onNavigateToProjectDetails(project.id) }
                    )
                }
            }
        }

        // Boîte de dialogue de confirmation de suppression
        showDeleteDialog?.let { project ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Supprimer le projet") },
                text = { Text("Voulez-vous vraiment supprimer ce projet d'épargne ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteProject(project)
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
                message = "Projet supprimé avec succès",
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

/**
 * Formatte un montant (à remplacer par l'utilitaire approprié)
 */
private fun formatAmount(amount: Double): String {
    return "%.2f €".format(amount)
}

@Preview(showBackground = true)
@Composable
fun SavingsScreenPreview() {
    MaterialTheme {
        SavingsScreen(
            onNavigateToAddProject = {},
            onNavigateToProjectDetails = {},
            onNavigateToAddTransaction = {},
            onNavigateBack = {}
        )
    }
}
