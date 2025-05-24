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
    // Renommé pour correspondre à la consigne, mais c'est bien uiState.projectFormState du ViewModel
    val formState = uiState.projectFormState 
    val validationErrors = uiState.validationErrors // Ajouté pour submitEnabled
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
        submitEnabled = !isLoading && validationErrors.isEmpty() // Modifié
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
                    viewModel.updateFormState { it.copy(title = title) }
                },
                label = { Text("Titre du projet") },
                isError = "title" in validationErrors,
                supportingText = validationErrors["title"]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { description ->
                    viewModel.updateFormState { it.copy(description = description) }
                },
                label = { Text("Description (optionnel)") },
                 isError = "description" in validationErrors,
                supportingText = validationErrors["description"]?.let { { Text(it) } },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Objectif
            MoneyInputField(
                value = formState.targetAmount,
                onValueChange = { amount ->
                    viewModel.updateFormState { it.copy(targetAmount = amount) }
                },
                label = "Objectif d'épargne",
                isError = "targetAmount" in validationErrors,
                errorMessage = validationErrors["targetAmount"]
            )

            // Montant actuel (non modifiable ici)
            Text("Montant actuel: ${com.cashruler.util.formatCurrency(formState.currentAmount)}", style = MaterialTheme.typography.bodyLarge)
            // Date de début (non modifiable)
            Text("Date de début: ${java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(formState.startDate)}", style = MaterialTheme.typography.bodyLarge)


            // Date Cible
            DateSelector(
                date = formState.targetDate ?: Date(), // Fournir une date par défaut si null, ou gérer la logique pour permettre de ne pas en avoir
                onDateSelected = { date ->
                    viewModel.updateFormState { it.copy(targetDate = date) }
                },
                label = "Date cible (optionnel)",
                isError = "targetDate" in validationErrors,
                errorMessage = validationErrors["targetDate"]
            )
            // Bouton pour effacer la date cible si elle est optionnelle
            if (formState.targetDate != null) {
                TextButton(onClick = { viewModel.updateFormState { it.copy(targetDate = null)} }) {
                    Text("Effacer la date cible")
                }
            }


            // Épargne périodique
            MoneyInputField(
                value = formState.periodicAmount ?: 0.0,
                onValueChange = { amount ->
                    viewModel.updateFormState { it.copy(periodicAmount = amount.takeIf { it > 0 }) }
                },
                label = "Montant périodique (optionnel)",
                isError = "periodicAmount" in validationErrors,
                errorMessage = validationErrors["periodicAmount"]
            )

            if (formState.periodicAmount != null && formState.periodicAmount > 0) {
                OutlinedTextField(
                    value = formState.savingFrequency?.toString() ?: "",
                    onValueChange = { frequency ->
                        viewModel.updateFormState {
                            it.copy(savingFrequency = frequency.toIntOrNull())
                        }
                    },
                    label = { Text("Fréquence d'épargne (jours)") },
                    isError = "savingFrequency" in validationErrors,
                    supportingText = validationErrors["savingFrequency"]?.let { { Text(it) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Priorité
            OutlinedTextField(
                value = formState.priority.toString(),
                onValueChange = { priority ->
                    viewModel.updateFormState { it.copy(priority = priority.toIntOrNull() ?: 0) }
                },
                label = { Text("Priorité (0 = faible)") },
                isError = "priority" in validationErrors,
                supportingText = validationErrors["priority"]?.let { { Text(it) } },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Icône
            var expandedIconSelector by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedIconSelector,
                onExpandedChange = { expandedIconSelector = it }
            ) {
                OutlinedTextField(
                    value = formState.icon ?: "Aucune icône",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Icône (optionnel)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIconSelector) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedIconSelector,
                    onDismissRequest = { expandedIconSelector = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Aucune icône") },
                        onClick = {
                            viewModel.updateFormState { it.copy(icon = null) }
                            expandedIconSelector = false
                        }
                    )
                    com.cashruler.data.models.SavingsProject.AVAILABLE_ICONS.forEach { iconName ->
                        DropdownMenuItem(
                            text = { Text(iconName) }, // Afficher l'icône ici serait mieux
                            onClick = {
                                viewModel.updateFormState { it.copy(icon = iconName) }
                                expandedIconSelector = false
                            }
                        )
                    }
                }
            }
            
            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { notes ->
                    viewModel.updateFormState { it.copy(notes = notes) }
                },
                label = { Text("Notes (optionnel)") },
                isError = "notes" in validationErrors,
                supportingText = validationErrors["notes"]?.let { { Text(it) } },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Statut Actif
            LabeledSwitch(
                checked = formState.isActive,
                onCheckedChange = { isActive ->
                    viewModel.updateFormState { it.copy(isActive = isActive) }
                },
                label = "Projet actif"
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
