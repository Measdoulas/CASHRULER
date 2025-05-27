package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ExpenseReminderFormState(
    val id: Long? = null,
    val description: String = "",
    val amount: String = "", // Store as String for TextField, convert/validate later
    val reminderDate: Date = Date(),
    val categoryId: Long? = null, // Or String if using category names directly initially
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
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class ExpenseReminderViewModel @Inject constructor(
    private val repository: ExpenseReminderRepositoryInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseReminderUiState())
    val uiState = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    fun loadActiveReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getActiveReminders()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load reminders: ${e.message}") }
                    _errorEvents.emit("Failed to load reminders: ${e.message}")
                }
                .collect { reminders ->
                    _uiState.update { it.copy(isLoading = false, reminders = reminders) }
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
                    amount = currentFormState.amount.toDoubleOrNull(), // Amount can be null
                    reminderDate = currentFormState.reminderDate,
                    categoryId = currentFormState.categoryId,
                    notes = currentFormState.notes.takeIf { it.isNotBlank() },
                    isRecurring = currentFormState.isRecurring,
                    frequencyDays = if (currentFormState.isRecurring) currentFormState.frequencyDays.toIntOrNull() else null,
                    isActive = currentFormState.isActive,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                repository.addReminder(reminder)
                _uiState.update { it.copy(isLoading = false, formState = ExpenseReminderFormState(), validationErrors = emptyMap()) } // Reset form
                _errorEvents.emit("Reminder added successfully.") // Use event for success message
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
                    updatedAt = Date() // ViewModel sets it, repo can override
                )
                repository.updateReminder(reminder)
                _uiState.update { it.copy(isLoading = false, formState = ExpenseReminderFormState(), validationErrors = emptyMap()) } // Reset form
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
                repository.deleteReminder(reminder)
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
                val reminder = repository.getReminderById(reminderId)
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
                repository.setReminderActive(reminderId, isActive)
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
