package com.cashruler.ui.screens.expensereminders

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cashruler.navigation.Routes
import com.cashruler.ui.theme.CashRulerTheme
import com.cashruler.ui.viewmodels.ExpenseReminderViewModel
import com.cashruler.data.models.ExpenseReminder // Required for preview
import java.util.Date // Required for preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRemindersScreen(
    navController: NavController,
    viewModel: ExpenseReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActiveReminders()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Expense Reminders") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.EXPENSE_REMINDER_FORM_NEW)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                item {
                    CircularProgressIndicator() // Or some other loading indicator
                }
            }
            items(uiState.reminders) { reminder ->
                ListItem(
                    headlineContent = { Text(reminder.description) },
                    supportingContent = { Text("Due: ${reminder.reminderDate} - Amount: ${reminder.amount ?: "N/A"}") },
                    trailingContent = { Text(if (reminder.isActive) "Active" else "Inactive") },
                    modifier = Modifier.padding() // Add clickable modifier later
                        // .clickable { navController.navigate(Routes.expenseReminderEdit(reminder.id)) } // Navigation to edit
                )
                HorizontalDivider()
            }
            if (uiState.reminders.isEmpty() && !uiState.isLoading) {
                item {
                    Text("No reminders found. Tap the '+' button to add one.")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseRemindersScreenPreview() {
    CashRulerTheme {
        // This preview won't have a real NavController or ViewModel,
        // so it's mostly for static layout.
        // For a more interactive preview, you'd mock the ViewModel and NavController.
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Expense Reminders Preview") })
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
                }
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(listOf(
                    ExpenseReminder(id = 1, description = "Pay Rent", amount = 1200.0, reminderDate = Date(), isActive = true),
                    ExpenseReminder(id = 2, description = "Subscription", amount = 15.0, reminderDate = Date(), isActive = false)
                )) { reminder ->
                     ListItem(
                        headlineContent = { Text(reminder.description) },
                        supportingContent = { Text("Due: ${reminder.reminderDate} - Amount: ${reminder.amount ?: "N/A"}") },
                        trailingContent = { Text(if (reminder.isActive) "Active" else "Inactive") }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
