package com.cashruler.ui.screens.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.data.models.Income
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.IncomeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeFormScreen(
    incomeId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState
    val validationErrors = uiState.validationErrors
    val isLoading by viewModel.isLoading.collectAsState()
    val types by viewModel.allTypes.collectAsState()

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }

    // Charge les données du revenu si on est en mode édition
    LaunchedEffect(incomeId) {
        if (incomeId != null) {
            viewModel.loadIncome(incomeId)
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
            // Retourne à l'écran précédent après un court délai
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    FormScreenContainer(
        title = if (incomeId == null) "Nouveau revenu" else "Modifier le revenu",
        onNavigateBack = onNavigateBack,
        onSubmit = {
            if (incomeId == null) {
                viewModel.addIncome()
            } else {
                viewModel.updateIncome(incomeId)
            }
        },
        modifier = modifier,
        submitEnabled = !isLoading && validationErrors.isEmpty()
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
                    viewModel.updateFormState { it.copy(amount = amount) }
                },
                label = "Montant",
                isError = "amount" in validationErrors,
                errorMessage = validationErrors["amount"],
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { description ->
                    viewModel.updateFormState { it.copy(description = description) }
                },
                label = { Text("Description") },
                isError = "description" in validationErrors,
                supportingText = validationErrors["description"]?.let { 
                    { Text(it) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Type
            CategorySelector(
                categories = types.ifEmpty { Income.DEFAULT_TYPES },
                selectedCategory = formState.type,
                onCategorySelected = { type ->
                    viewModel.updateFormState { it.copy(type = type) }
                },
                label = "Type",
                isError = "type" in validationErrors,
                errorMessage = validationErrors["type"]
            )

            // Date
            DateSelector(
                date = formState.date,
                onDateSelected = { date ->
                    viewModel.updateFormState { it.copy(date = date) }
                },
                label = "Date",
                modifier = Modifier.fillMaxWidth()
            )

            // Revenu récurrent
            LabeledSwitch(
                checked = formState.isRecurring,
                onCheckedChange = { isRecurring ->
                    viewModel.updateFormState { it.copy(isRecurring = isRecurring) }
                },
                label = "Revenu récurrent"
            )

            // Fréquence de récurrence
            if (formState.isRecurring) {
                OutlinedTextField(
                    value = formState.recurringFrequency?.toString() ?: "",
                    onValueChange = { frequency ->
                        viewModel.updateFormState {
                            it.copy(recurringFrequency = frequency.toIntOrNull())
                        }
                    },
                    label = { Text("Fréquence (jours)") },
                    isError = "recurringFrequency" in validationErrors,
                    supportingText = validationErrors["recurringFrequency"]?.let { 
                        { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Revenu imposable
            LabeledSwitch(
                checked = formState.isTaxable,
                onCheckedChange = { isTaxable ->
                    viewModel.updateFormState { it.copy(isTaxable = isTaxable) }
                },
                label = "Revenu imposable"
            )

            // Taux d'imposition
            if (formState.isTaxable) {
                OutlinedTextField(
                    value = formState.taxRate?.toString() ?: "",
                    onValueChange = { rate ->
                        viewModel.updateFormState {
                            it.copy(taxRate = rate.toDoubleOrNull())
                        }
                    },
                    label = { Text("Taux d'imposition (%)") },
                    isError = "taxRate" in validationErrors,
                    supportingText = validationErrors["taxRate"]?.let { 
                        { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { notes ->
                    viewModel.updateFormState { it.copy(notes = notes) }
                },
                label = { Text("Notes (optionnel)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Messages
        if (showSuccessMessage) {
            MessageSnackbar(
                message = if (incomeId == null)
                    "Revenu ajouté avec succès"
                else
                    "Revenu mis à jour avec succès",
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
