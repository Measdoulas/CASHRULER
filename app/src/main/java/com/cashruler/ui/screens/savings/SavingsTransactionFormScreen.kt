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
fun SavingsTransactionFormScreen(
    projectId: Long,
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.transactionFormState
    val isLoading by viewModel.isLoading.collectAsState()

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Charge les données de la transaction si on est en mode édition
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
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
        title = if (transactionId == null) "Nouvelle transaction" else "Modifier la transaction",
        onNavigateBack = onNavigateBack,
        onSubmit = {
            if (transactionId == null) {
                viewModel.addTransaction(projectId)
            } else {
                viewModel.updateTransaction(transactionId)
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
            // Montant
            MoneyInputField(
                value = formState.amount,
                onValueChange = { amount ->
                    viewModel.updateTransactionFormState { it.copy(amount = amount) }
                },
                label = "Montant"
            )

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { description ->
                    viewModel.updateTransactionFormState { it.copy(description = description) }
                },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Date
            DateSelector(
                date = formState.date,
                onDateSelected = { date ->
                    viewModel.updateTransactionFormState { it.copy(date = date) }
                },
                label = "Date"
            )

            // Type de transaction
            CategorySelector(
                categories = listOf("Dépôt", "Retrait", "Intérêts", "Autre"),
                selectedCategory = formState.type,
                onCategorySelected = { type ->
                    viewModel.updateTransactionFormState { it.copy(type = type) }
                },
                label = "Type de transaction"
            )

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { notes ->
                    viewModel.updateTransactionFormState { it.copy(notes = notes) }
                },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Prélèvement automatique
            if (transactionId == null) {
                LabeledSwitch(
                    checked = formState.isRecurring,
                    onCheckedChange = { isRecurring ->
                        viewModel.updateTransactionFormState { it.copy(isRecurring = isRecurring) }
                    },
                    label = "Transaction récurrente"
                )

                if (formState.isRecurring) {
                    OutlinedTextField(
                        value = formState.recurringFrequency?.toString() ?: "",
                        onValueChange = { frequency ->
                            viewModel.updateTransactionFormState {
                                it.copy(recurringFrequency = frequency.toIntOrNull())
                            }
                        },
                        label = { Text("Fréquence (jours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    DateSelector(
                        date = formState.nextOccurrence ?: Date(),
                        onDateSelected = { date ->
                            viewModel.updateTransactionFormState { it.copy(nextOccurrence = date) }
                        },
                        label = "Prochaine occurrence"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Messages
        if (showSuccessMessage) {
            MessageSnackbar(
                message = if (transactionId == null)
                    "Transaction ajoutée avec succès"
                else
                    "Transaction mise à jour avec succès",
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
fun SavingsTransactionFormScreenPreview() {
    MaterialTheme {
        SavingsTransactionFormScreen(
            projectId = 1L,
            onNavigateBack = {}
        )
    }
}
