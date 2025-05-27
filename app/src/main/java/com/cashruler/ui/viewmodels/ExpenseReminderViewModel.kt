package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.CategoryEntity // Added
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.data.repositories.CategoryRepositoryInterface // Added
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ExpenseReminderFormState(
    val id: Long? = null,
    val description: String = "",
    val amount: String = "", 
    val reminderDate: Date = Date(),
    val categoryId: Long? = null, 
    val notes: String = "",
    val isRecurring: Boolean = false,
    val frequencyDays: String = "", // Store as String for TextField
    val isActive: Boolean = true
)

data class ExpenseReminderUiState(
    val formState: ExpenseReminderFormState = ExpenseReminderFormState(),
    val reminders: List<ExpenseReminder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val categories: List<CategoryEntity> = emptyList(),
    val remindersDisplay: List<ExpenseReminderDisplayItem> = emptyList() // Added
)

// New data class for display
data class ExpenseReminderDisplayItem(
    val reminder: ExpenseReminder,
    val categoryName: String?
)

@HiltViewModel
class ExpenseReminderViewModel @Inject constructor(
    private val expenseReminderRepository: ExpenseReminderRepositoryInterface, 
    private val categoryRepository: CategoryRepositoryInterface 
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReminderUiState())
    val uiState = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    init {
        loadCategories() // Load categories first
        loadActiveReminders() 
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categoryList = categoryRepository.getAllCategories() 
                _uiState.update { it.copy(categories = categoryList, isLoading = false) } // Set isLoading false after categories load
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load categories: ${e.message}")}
                _errorEvents.emit("Failed to load categories: ${e.message}")
            }
        }
    }

    fun loadActiveReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Reset error on new load attempt
            expenseReminderRepository.getActiveReminders()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load reminders: ${e.message}") }
                    _errorEvents.emit("Failed to load reminders: ${e.message}")
                }
                .collect { reminders ->
                    val displayItems = reminders.map { reminder ->
                        val categoryName = uiState.value.categories.find { it.id == reminder.categoryId }?.name
                        ExpenseReminderDisplayItem(reminder, categoryName)
                    }
                    _uiState.update { it.copy(isLoading = false, remindersDisplay = displayItems, reminders = reminders) } // Keep original reminders if needed elsewhere
                }
        }
    }

    fun updateFormState(newFormState: ExpenseReminderFormState) {
        _uiState.update {
            val validation = validateFormState(newFormState)
            it.copy(formState = newFormState, validationErrors = validation)
        }
    }
    
    private fun validateFormState(formState: ExpenseReminderFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (formState.description.isBlank()) {
            errors["description"] = "Description cannot be empty"
        }
        formState.amount.toDoubleOrNull() ?: run {
            if (formState.amount.isNotBlank()) errors["amount"] = "Invalid amount"
        }
         if (formState.isRecurring) {
            formState.frequencyDays.toIntOrNull()?.let {
                if (it <= 0) errors["frequencyDays"] = "Frequency must be positive"
            } ?: run {
                 if (formState.frequencyDays.isNotBlank()) errors["frequencyDays"] = "Invalid frequency"
                 else errors["frequencyDays"] = "Frequency required for recurring reminders"
            }
        }
        // Basic date validation (e.g., not in the distant past, though Date() defaults to now)
        // More complex date validation can be added if needed.
        return errors
    }

    fun addReminder() {
        val currentFormState = _uiState.value.formState
        val validationErrors = validateFormState(currentFormState)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = validationErrors) }
            viewModelScope.launch { _errorEvents.emit("Please correct the form errors.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reminder = ExpenseReminder(
                    description = currentFormState.description,
                    amount = currentFormState.amount.toDoubleOrNull(), 
                    reminderDate = currentFormState.reminderDate,
                    categoryId = currentFormState.categoryId, // Already Long?
                    notes = currentFormState.notes.takeIf { it.isNotBlank() },
                    isRecurring = currentFormState.isRecurring,
                    frequencyDays = if (currentFormState.isRecurring) currentFormState.frequencyDays.toIntOrNull() else null,
                    isActive = currentFormState.isActive,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                expenseReminderRepository.addReminder(reminder) // Use renamed repository
                _uiState.update { it.copy(isLoading = false, formState = ExpenseReminderFormState(), validationErrors = emptyMap()) } 
                _errorEvents.emit("Reminder added successfully.") 
                loadActiveReminders() // Refresh list
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to add reminder: ${e.message}") }
                _errorEvents.emit("Failed to add reminder: ${e.message}")
            }
        }
    }

    fun updateReminder(reminderId: Long) {
        val currentFormState = _uiState.value.formState
         val validationErrors = validateFormState(currentFormState)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = validationErrors) }
            viewModelScope.launch { _errorEvents.emit("Please correct the form errors.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reminder = ExpenseReminder(
                    id = reminderId,
                    description = currentFormState.description,
                    amount = currentFormState.amount.toDoubleOrNull(),
                    reminderDate = currentFormState.reminderDate,
                    categoryId = currentFormState.categoryId,
                    notes = currentFormState.notes.takeIf { it.isNotBlank() },
                    isRecurring = currentFormState.isRecurring,
                    frequencyDays = if (currentFormState.isRecurring) currentFormState.frequencyDays.toIntOrNull() else null,
                    isActive = currentFormState.isActive,
                    // createdAt will be preserved from original, updatedAt will be set by repository/DAO
                    updatedAt = Date() 
                )
                expenseReminderRepository.updateReminder(reminder) // Use renamed repository
                _uiState.update { it.copy(isLoading = false, formState = ExpenseReminderFormState(), validationErrors = emptyMap()) } 
                 _errorEvents.emit("Reminder updated successfully.")
                loadActiveReminders() // Refresh list
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to update reminder: ${e.message}") }
                _errorEvents.emit("Failed to update reminder: ${e.message}")
            }
        }
    }

    fun deleteReminder(reminder: ExpenseReminder) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                expenseReminderRepository.deleteReminder(reminder) // Use renamed repository
                _uiState.update { it.copy(isLoading = false) }
                _errorEvents.emit("Reminder deleted successfully.")
                loadActiveReminders() // Refresh list
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to delete reminder: ${e.message}") }
                _errorEvents.emit("Failed to delete reminder: ${e.message}")
            }
        }
    }

    fun loadReminderForEditing(reminderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reminder = expenseReminderRepository.getReminderById(reminderId) // Use renamed repository
                if (reminder != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            formState = ExpenseReminderFormState(
                                id = reminder.id,
                                description = reminder.description,
                                amount = reminder.amount?.toString() ?: "",
                                reminderDate = reminder.reminderDate,
                                categoryId = reminder.categoryId,
                                notes = reminder.notes ?: "",
                                isRecurring = reminder.isRecurring,
                                frequencyDays = reminder.frequencyDays?.toString() ?: "",
                                isActive = reminder.isActive
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Reminder not found.") }
                    _errorEvents.emit("Reminder not found.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load reminder: ${e.message}") }
                _errorEvents.emit("Failed to load reminder: ${e.message}")
            }
        }
    }
    
    fun setReminderActive(reminderId: Long, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                expenseReminderRepository.setReminderActive(reminderId, isActive) // Use renamed repository
                _uiState.update { it.copy(isLoading = false) }
                _errorEvents.emit("Reminder status updated.")
                loadActiveReminders() // Refresh list
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to update reminder status: ${e.message}") }
                _errorEvents.emit("Failed to update reminder status: ${e.message}")
            }
        }
    }
}
