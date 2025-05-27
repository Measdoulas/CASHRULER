package com.cashruler.ui.screens.expensereminders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cashruler.ui.theme.CashRulerTheme
import com.cashruler.ui.viewmodels.ExpenseReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReminderFormScreen(
    navController: NavController,
    viewModel: ExpenseReminderViewModel = hiltViewModel(),
    reminderId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState

    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            viewModel.loadReminderForEditing(reminderId)
        } else {
            // Reset form for new entry, or ensure ViewModel handles this
            // viewModel.updateFormState(ExpenseReminderFormState()) // Consider if VM should reset itself
        }
    }
    
    // Collect error events for displaying Snackbars or Toasts
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            // Here you would show a Snackbar or Toast
            // For now, we can just print it or ignore for stub
            println("Error/Event: $message")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reminderId == null) "Add Reminder" else "Edit Reminder") },
                navigationIcon = {
                    // IconButton(onClick = { navController.popBackStack() }) {
                    //     Icon(Icons.Filled.ArrowBack, "Back")
                    // }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Reminder Details", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateFormState(formState.copy(description = it)) },
                label = { Text("Description") },
                isError = uiState.validationErrors.containsKey("description"),
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.validationErrors.containsKey("description")) {
                Text(uiState.validationErrors["description"]!!, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateFormState(formState.copy(amount = it)) },
                label = { Text("Amount (Optional)") },
                isError = uiState.validationErrors.containsKey("amount"),
                modifier = Modifier.fillMaxWidth()
            )
             if (uiState.validationErrors.containsKey("amount")) {
                Text(uiState.validationErrors["amount"]!!, color = MaterialTheme.colorScheme.error)
            }

            // Placeholder for Date Picker
            OutlinedTextField(
                value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(formState.reminderDate),
                onValueChange = { /* TODO: Date Picker Integration */ },
                label = { Text("Reminder Date") },
                readOnly = true, // Open DatePickerDialog on click
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateFormState(formState.copy(notes = it)) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = formState.isRecurring,
                    onCheckedChange = { viewModel.updateFormState(formState.copy(isRecurring = it)) }
                )
                Text("Is Recurring?")
            }

            if (formState.isRecurring) {
                OutlinedTextField(
                    value = formState.frequencyDays,
                    onValueChange = { viewModel.updateFormState(formState.copy(frequencyDays = it)) },
                    label = { Text("Frequency (Days)") },
                    isError = uiState.validationErrors.containsKey("frequencyDays"),
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.validationErrors.containsKey("frequencyDays")) {
                    Text(uiState.validationErrors["frequencyDays"]!!, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (reminderId == null) {
                        viewModel.addReminder()
                    } else {
                        viewModel.updateReminder(reminderId)
                    }
                    // Consider navigating back only on success, perhaps by observing an event from ViewModel
                    // For now, assume success and pop back or listen to an event
                    // navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (reminderId == null) "Add Reminder" else "Save Changes")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseReminderFormScreenNewPreview() {
    CashRulerTheme {
        // Mock NavController for preview if needed for more complex scenarios
        ExpenseReminderFormScreen(navController = NavController(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseReminderFormScreenEditPreview() {
    CashRulerTheme {
        // Mock NavController for preview
        ExpenseReminderFormScreen(navController = NavController(LocalContext.current), reminderId = 1L)
    }
}
