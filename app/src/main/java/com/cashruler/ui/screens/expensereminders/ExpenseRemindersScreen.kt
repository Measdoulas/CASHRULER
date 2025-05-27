package com.cashruler.ui.screens.expensereminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew // Icon for recurring
import androidx.compose.material.icons.filled.CheckCircle // Icon for active
import androidx.compose.material.icons.filled.Error // Icon for inactive or error
import androidx.compose.material.icons.filled.Event // Icon for date
import androidx.compose.material.icons.filled.Label // Icon for category
import androidx.compose.material.icons.filled.Money // Icon for amount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.navigation.Routes
import com.cashruler.ui.theme.CashRulerTheme
import com.cashruler.ui.viewmodels.ExpenseReminderDisplayItem
import com.cashruler.ui.viewmodels.ExpenseReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRemindersScreen(
    navController: NavController,
    viewModel: ExpenseReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // LaunchedEffect to load reminders (and categories via ViewModel's init)
    // ViewModel's init block should call loadActiveReminders after categories are loaded.
    // If loadActiveReminders is not in init, call it here:
    // LaunchedEffect(Unit) {
    //     viewModel.loadActiveReminders() // Assuming ViewModel handles category loading first
    // }

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
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                uiState.remindersDisplay.isEmpty() -> {
                    Text(
                        text = "No reminders yet. Tap '+' to add one.",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.remindersDisplay) { reminderDisplayItem ->
                            ExpenseReminderListItem(
                                reminderDisplayItem = reminderDisplayItem,
                                onClick = {
                                    navController.navigate(Routes.expenseReminderEdit(reminderDisplayItem.reminder.id))
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseReminderListItem(
    reminderDisplayItem: ExpenseReminderDisplayItem,
    onClick: () -> Unit
) {
    val reminder = reminderDisplayItem.reminder
    val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(reminder.reminderDate)
    val itemColor = if (reminder.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (reminder.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = itemColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, contentDescription = "Date", modifier = Modifier.size(16.dp), tint = itemColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = itemColor
                    )
                }

                reminder.amount?.let { amount ->
                    Spacer(modifier = Modifier.height(4.dp))
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Money, contentDescription = "Amount", modifier = Modifier.size(16.dp), tint = itemColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Amount: ${com.cashruler.util.Formatters.formatCurrency(amount)}", // Assuming Formatters.formatCurrency exists
                            style = MaterialTheme.typography.bodyMedium,
                            color = itemColor
                        )
                    }
                }
                reminderDisplayItem.categoryName?.let { categoryName ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Label, contentDescription = "Category", modifier = Modifier.size(16.dp), tint = itemColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Category: $categoryName",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = itemColor
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (reminder.isRecurring) {
                    Icon(
                        Icons.Filled.Autorenew,
                        contentDescription = "Recurring",
                        tint = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    if (reminder.isActive) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = if (reminder.isActive) "Active" else "Inactive",
                    tint = if (reminder.isActive) Color.Green.copy(alpha=0.7f) else Color.Red.copy(alpha=0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExpenseRemindersScreenPreview_Empty() {
    CashRulerTheme {
        // Simulate empty state
        val mockViewModel: ExpenseReminderViewModel = hiltViewModel() // Or mock manually
        // How to set mockViewModel.uiState for preview is tricky without direct access or a PreviewParameterProvider
        // For simplicity, this preview might not fully reflect the empty state logic from ViewModel.
        Scaffold(
            topBar = { TopAppBar(title = { Text("Expense Reminders") }) },
            floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, "") } }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No reminders yet. Tap '+' to add one.")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseReminderListItemPreview() {
    CashRulerTheme {
        ExpenseReminderListItem(
            reminderDisplayItem = ExpenseReminderDisplayItem(
                reminder = ExpenseReminder(
                    id = 1,
                    description = "Monthly Netflix Subscription",
                    amount = 15.99,
                    reminderDate = Date(),
                    categoryId = 1L,
                    isRecurring = true,
                    frequencyDays = 30,
                    isActive = true,
                    notes = "Auto-renews"
                ),
                categoryName = "Entertainment"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseReminderListItemInactivePreview() {
    CashRulerTheme {
        ExpenseReminderListItem(
            reminderDisplayItem = ExpenseReminderDisplayItem(
                reminder = ExpenseReminder(
                    id = 2,
                    description = "Old Gym Membership",
                    amount = 40.00,
                    reminderDate = Date(System.currentTimeMillis() - 1000L*60*60*24*30), // a month ago
                    categoryId = 2L,
                    isRecurring = false,
                    isActive = false
                ),
                categoryName = "Health"
            ),
            onClick = {}
        )
    }
}
