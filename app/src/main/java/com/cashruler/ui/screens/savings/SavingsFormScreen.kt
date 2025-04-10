package com.cashruler.ui.screens.savings

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
import com.cashruler.ui.viewmodels.SavingsViewModel
import java.util.*

@Composable
fun SavingsFormScreen(
    projectId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.projectFormState
    val isLoading by viewModel.isLoading.collectAsState()

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Charge les données du projet si on est en mode édition
    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.loadProject(projectId)
        }
    }

    // Collecte les messages d'erreur
    LaunchedEffect(true) {
        viewModel.error.collect { error ->
            showErrorMessage = error
        }
    }

    // Gère le succès de la soumission
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            showSuccessMessage = true
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    FormScreenContainer(
        title = if (projectId == null) "Nouveau projet d'épargne" else "Modifier le projet",
        onNavigateBack = onNavigateBack,
        onSubmit = {
            if (projectId == null) {
                viewModel.addProject()
            } else {
                viewModel.updateProject(projectId)
            }
        },
        modifier = modifier,
        submitEnabled = !isLoading && formState.isValid
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Titre du projet
            OutlinedTextField(
                value = formState.title,
                onValueChange = { title ->
                    viewModel.updateProjectFormState { it.copy(title = title) }
                },
                label = { Text("Titre du projet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { description ->
                    viewModel.updateProjectFormState { it.copy(description = description) }
                },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Objectif
            MoneyInputField(
                value = formState.targetAmount,
                onValueChange = { amount ->
                    viewModel.updateProjectFormState { it.copy(targetAmount = amount) }
                },
                label = "Objectif"
            )

            // Montant initial
            if (projectId == null) {
                MoneyInputField(
                    value = formState.initialAmount,
                    onValueChange = { amount ->
                        viewModel.updateProjectFormState { it.copy(initialAmount = amount) }
                    },
                    label = "Montant initial"
                )
            }

            // Date d'échéance
            LabeledSwitch(
                checked = formState.hasDeadline,
                onCheckedChange = { hasDeadline ->
                    viewModel.updateProjectFormState { it.copy(hasDeadline = hasDeadline) }
                },
                label = "Date d'échéance"
            )

            if (formState.hasDeadline) {
                DateSelector(
                    date = formState.deadline ?: Date(),
                    onDateSelected = { date ->
                        viewModel.updateProjectFormState { it.copy(deadline = date) }
                    },
                    label = "Date d'échéance"
                )
            }

            // Épargne automatique
            LabeledSwitch(
                checked = formState.hasAutoSaving,
                onCheckedChange = { hasAutoSaving ->
                    viewModel.updateProjectFormState { it.copy(hasAutoSaving = hasAutoSaving) }
                },
                label = "Épargne automatique"
            )

            if (formState.hasAutoSaving) {
                MoneyInputField(
                    value = formState.autoSavingAmount ?: 0.0,
                    onValueChange = { amount ->
                        viewModel.updateProjectFormState { it.copy(autoSavingAmount = amount) }
                    },
                    label = "Montant à épargner"
                )

                OutlinedTextField(
                    value = formState.autoSavingFrequency?.toString() ?: "",
                    onValueChange = { frequency ->
                        viewModel.updateProjectFormState {
                            it.copy(autoSavingFrequency = frequency.toIntOrNull())
                        }
                    },
                    label = { Text("Fréquence (jours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Catégorie
            CategorySelector(
                categories = viewModel.allCategories.value,
                selectedCategory = formState.category,
                onCategorySelected = { category ->
                    viewModel.updateProjectFormState { it.copy(category = category) }
                },
                label = "Catégorie"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Messages
        if (showSuccessMessage) {
            MessageSnackbar(
                message = if (projectId == null)
                    "Projet créé avec succès"
                else
                    "Projet mis à jour avec succès",
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

@Preview(showBackground = true)
@Composable
fun SavingsFormScreenPreview() {
    MaterialTheme {
        SavingsFormScreen(
            projectId = null,
            onNavigateBack = {}
        )
    }
}
