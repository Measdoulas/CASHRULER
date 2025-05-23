package com.cashruler.ui.screens.expenses

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
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.ExpenseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    expenseId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState
    val validationErrors = uiState.validationErrors
    val isLoading by viewModel.isLoading.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) } // Nouveau

    // Charge les données de la dépense si on est en mode édition
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
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
        title = if (expenseId == null) "Nouvelle dépense" else "Modifier la dépense",
        onNavigateBack = onNavigateBack,
        onSubmit = {
            if (expenseId == null) {
                viewModel.addExpense()
            } else {
                viewModel.updateExpense(expenseId)
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

            // Catégorie
            CategorySelector(
                categories = categories.ifEmpty { listOf("Aucune catégorie", *Expense.DEFAULT_CATEGORIES.toTypedArray()) },
                selectedCategory = formState.category.ifEmpty { "Aucune catégorie" },
                onCategorySelected = { category ->
                    viewModel.updateFormState { it.copy(category = if (category == "Aucune catégorie") "" else category) }
                },
                onAddNewCategoryClick = { // Nouveau
                    showAddCategoryDialog = true
                },
                label = "Catégorie",
                isError = "category" in validationErrors,
                errorMessage = validationErrors["category"]
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

            // Dépense récurrente
            LabeledSwitch(
                checked = formState.isRecurring,
                onCheckedChange = { isRecurring ->
                    viewModel.updateFormState { it.copy(isRecurring = isRecurring) }
                },
                label = "Dépense récurrente"
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
                message = if (expenseId == null)
                    "Dépense ajoutée avec succès"
                else
                    "Dépense mise à jour avec succès",
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

        // Dialogue pour ajouter une nouvelle catégorie
        if (showAddCategoryDialog) {
            var newCategoryName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Nouvelle catégorie") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nom de la catégorie") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addNewCategory(newCategoryName)
                            // Optionnel: Sélectionne automatiquement la nouvelle catégorie
                            viewModel.updateFormState { it.copy(category = newCategoryName) }
                            showAddCategoryDialog = false
                        }
                    }) { Text("Ajouter") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) { Text("Annuler") }
                }
            )
        }
    }
}
