package com.cashruler.ui.screens.expensereminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cashruler.data.models.CategoryEntity
import com.cashruler.ui.theme.CashRulerTheme
import com.cashruler.ui.viewmodels.ExpenseReminderFormState
import com.cashruler.ui.viewmodels.ExpenseReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReminderFormScreen(
    navController: NavController,
    viewModel: ExpenseReminderViewModel = hiltViewModel(),
    reminderId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            viewModel.loadReminderForEditing(reminderId)
        } else {
            // ViewModel should ideally reset its form state when reminderId is null.
            // Or, provide a specific function like viewModel.resetFormState()
            // For now, assuming ViewModel handles new form state via default in its own state.
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (reminderId == null) "New Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // Make column scrollable
            verticalArrangement = Arrangement.spacedBy(10.dp) // Consistent spacing
        ) {
            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateFormState(formState.copy(description = it)) },
                label = { Text("Description") },
                isError = uiState.validationErrors.containsKey("description"),
                supportingText = { 
                    if (uiState.validationErrors.containsKey("description")) {
                        Text(uiState.validationErrors["description"]!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Amount
            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateFormState(formState.copy(amount = it)) },
                label = { Text("Amount (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey("amount"),
                supportingText = {
                    if (uiState.validationErrors.containsKey("amount")) {
                        Text(uiState.validationErrors["amount"]!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Date Picker
            OutlinedTextField(
                value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(formState.reminderDate),
                onValueChange = { /* Read-only, value changed by DatePicker */ },
                label = { Text("Reminder Date") },
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.DateRange, "Select Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            if (showDatePicker) {
                val calendar = Calendar.getInstance().apply { time = formState.reminderDate }
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = calendar.timeInMillis,
                    yearRange = (LocalDate.now().year - 10)..(LocalDate.now().year + 10) // Example range
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Ensure using UTC for selectedDateMillis as DatePicker uses it
                                val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                selectedCalendar.timeInMillis = millis
                                viewModel.updateFormState(formState.copy(reminderDate = selectedCalendar.time))
                            }
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            // Category Dropdown
            CategoryDropdown(
                categories = uiState.categories,
                selectedCategoryId = formState.categoryId,
                onCategorySelected = { categoryId ->
                    viewModel.updateFormState(formState.copy(categoryId = categoryId))
                }
            )


            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateFormState(formState.copy(notes = it)) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Is Recurring Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recurring Reminder", modifier = Modifier.weight(1f))
                Switch(
                    checked = formState.isRecurring,
                    onCheckedChange = { viewModel.updateFormState(formState.copy(isRecurring = it)) }
                )
            }

            // Frequency Days (Conditional)
            if (formState.isRecurring) {
                OutlinedTextField(
                    value = formState.frequencyDays,
                    onValueChange = { viewModel.updateFormState(formState.copy(frequencyDays = it)) },
                    label = { Text("Frequency (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.validationErrors.containsKey("frequencyDays"),
                    supportingText = {
                        if (uiState.validationErrors.containsKey("frequencyDays")) {
                            Text(uiState.validationErrors["frequencyDays"]!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Is Active Switch
             Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reminder Active", modifier = Modifier.weight(1f))
                Switch(
                    checked = formState.isActive,
                    onCheckedChange = { viewModel.updateFormState(formState.copy(isActive = it)) }
                )
            }

            Spacer(Modifier.weight(1f)) // Push button to bottom

            // Save Button
            Button(
                onClick = {
                    if (reminderId == null) {
                        viewModel.addReminder()
                    } else {
                        viewModel.updateReminder(reminderId)
                    }
                    // Navigation back should ideally be triggered by a success event from ViewModel
                },
                enabled = uiState.validationErrors.isEmpty() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (reminderId == null) "Add Reminder" else "Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategoryName,
            onValueChange = {}, // Read-only
            label = { Text("Category (Optional)") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExpenseReminderFormScreenNewPreview() {
    CashRulerTheme {
        ExpenseReminderFormScreen(navController = NavController(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseReminderFormScreenEditPreview() {
    CashRulerTheme {
        ExpenseReminderFormScreen(navController = NavController(LocalContext.current), reminderId = 1L)
    }
}
// Required for DatePickerState in Preview if not using actual VM
typealias LocalDate = java.time.LocalDate 
