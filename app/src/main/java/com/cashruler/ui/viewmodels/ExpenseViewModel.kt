package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.data.repositories.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour la gestion des dépenses
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val spendingLimitRepository: SpendingLimitRepository
) : ViewModel() {

    // États UI
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState = _uiState.asStateFlow()

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Dépenses
    val allExpenses = expenseRepository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expensesGroupedByMonth: StateFlow<Map<YearMonth, List<Expense>>> =
        allExpenses.map { expenses ->
            expenses.groupBy { expense ->
                // Convertit la Date de l'expense en YearMonth
                val localDate = expense.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                YearMonth.from(localDate)
            }
            // Optionnel: trie la map par YearMonth décroissant pour afficher les plus récents en premier
            .toSortedMap(compareByDescending { it })
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Catégories
    val allCategories = expenseRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Met à jour l'état du formulaire et valide les champs
     */
    fun updateFormState(update: (ExpenseFormState) -> ExpenseFormState) {
        _uiState.update { currentState ->
            val newFormState = update(currentState.formState)
            val validationErrors = validateFormState(newFormState)
            currentState.copy(
                formState = newFormState,
                validationErrors = validationErrors
            )
        }
    }

    /**
     * Valide l'état actuel du formulaire
     */
    private fun validateFormState(formState: ExpenseFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        // Validation du montant
        if (formState.amount <= 0) {
            errors["amount"] = "Le montant doit être supérieur à 0"
        }
        
        // Validation de la description
        if (formState.description.isBlank()) {
            errors["description"] = "La description est requise"
        } else if (formState.description.length < 3) {
            errors["description"] = "La description doit contenir au moins 3 caractères"
        }
        
        // Validation de la catégorie
        if (formState.category.isBlank()) {
            errors["category"] = "La catégorie est requise"
        }
        
        // Validation de la récurrence
        if (formState.isRecurring) {
            if (formState.recurringFrequency == null || formState.recurringFrequency <= 0) {
                errors["recurringFrequency"] = "La fréquence doit être supérieure à 0 jours"
            } else if (formState.recurringFrequency > 365) {
                errors["recurringFrequency"] = "La fréquence ne peut pas dépasser 365 jours"
            }
        }
        
        return errors
    }

    /**
     * Vérifie si le formulaire est valide
     */
    private fun isFormValid(): Boolean {
        return _uiState.value.validationErrors.isEmpty()
    }

    /**
     * Ajoute une nouvelle dépense
     */
    fun addExpense() {
        if (!isFormValid()) {
            viewModelScope.launch {
                _error.emit("Veuillez corriger les erreurs du formulaire")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val formState = _uiState.value.formState
            val expense = Expense(
                amount = formState.amount,
                description = formState.description,
                category = formState.category,
                date = formState.date,
                isRecurring = formState.isRecurring,
                recurringFrequency = formState.recurringFrequency,
                notes = formState.notes.takeIf { it.isNotBlank() },
                attachmentUri = formState.attachmentUri,
                spendingLimitId = formState.spendingLimitId
            )
            handleExpense(expense)
        }.invokeOnCompletion { _isLoading.value = false }
    }

    /**
     * Met à jour une dépense
     */
    fun updateExpense(id: Long) {
        if (!isFormValid()) {
            viewModelScope.launch {
                _error.emit("Veuillez corriger les erreurs du formulaire")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val formState = _uiState.value.formState
            val expense = Expense(
                id = id,
                amount = formState.amount,
                description = formState.description,
                category = formState.category,
                date = formState.date,
                isRecurring = formState.isRecurring,
                recurringFrequency = formState.recurringFrequency,
                notes = formState.notes.takeIf { it.isNotBlank() },
                attachmentUri = formState.attachmentUri,
                spendingLimitId = formState.spendingLimitId
            )
            handleExpense(expense, id)
        }.invokeOnCompletion { _isLoading.value = false }
    }

    /**
     * Supprime une dépense
     */
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(expense)
                expense.spendingLimitId?.let { limitId ->
                    spendingLimitRepository.addToSpentAmount(limitId, -expense.amount)
                }
            } catch (e: Exception) {
                _error.emit("Erreur lors de la suppression de la dépense: ${e.message}")
            }
        }
    }

    /**
     * Charge une dépense pour édition
     */
    fun loadExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(id)
                .filterNotNull()
                .collect { expense ->
                    _uiState.update { it.copy(
                        formState = ExpenseFormState(
                            amount = expense.amount,
                            description = expense.description,
                            category = expense.category,
                            date = expense.date,
                            isRecurring = expense.isRecurring,
                            recurringFrequency = expense.recurringFrequency,
                            notes = expense.notes,
                            attachmentUri = expense.attachmentUri,
                            spendingLimitId = expense.spendingLimitId
                        ),
                        validationErrors = emptyMap()
                    ) }
                }
        }
    }

    private suspend fun handleExpense(expense: Expense, id: Long? = null) = try {
        when (val result = expenseRepository.validateExpense(expense)) {
            is ValidationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            formState = ExpenseFormState(),
                            isSuccess = true,
                            validationErrors = emptyMap()
                        )
                    }
                    if (id != null) {
                        expenseRepository.updateExpense(expense)
                    } else {
                        val expenseId = expenseRepository.addExpense(expense)
                        expense.spendingLimitId?.let { limitId ->
                            spendingLimitRepository.addToSpentAmount(limitId, expense.amount)
                        }
                    }
                }
                is ValidationResult.Error -> {
                    _error.emit(result.errors.joinToString("\n"))
                }
            }
        } catch (e: Exception) {
            _error.emit("Erreur lors de la mise à jour/ajout de la dépense: ${e.message}")
        }
    }







    /**
     * Réinitialise le formulaire
     */
    fun resetForm() {
        _uiState.update { it.copy(
            formState = ExpenseFormState(),
            isSuccess = false,
            validationErrors = emptyMap()
        ) }
    }

    private fun createExpenseFromForm(): Expense {
        return with(_uiState.value.formState) {
            Expense(
                amount = amount,
                description = description,
                category = category,
                date = date,
                isRecurring = isRecurring,
                recurringFrequency = recurringFrequency,
                notes = notes.takeIf { it.isNotBlank() },
                attachmentUri = attachmentUri,
                spendingLimitId = spendingLimitId
            )
        }
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    fun addNewCategory(categoryName: String) {
        if (categoryName.isBlank()) {
            viewModelScope.launch {
                _error.emit("Le nom de la catégorie ne peut pas être vide.")
            }
            return
        }
        // Vérifie si la catégorie existe déjà (insensible à la casse)
        val existingCategory = allCategories.value.firstOrNull { it.equals(categoryName, ignoreCase = true) }
        if (existingCategory != null) {
            viewModelScope.launch {
                _error.emit("La catégorie '$existingCategory' existe déjà.")
                // Optionnel: Sélectionne la catégorie existante au lieu d'émettre une erreur
                // updateFormState { it.copy(category = existingCategory) }
            }
            return
        }

        viewModelScope.launch {
            try {
                expenseRepository.addCategory(categoryName) // Nouvelle fonction dans le Repository
                // Pas besoin de rafraîchir manuellement allCategories si le Flow du repo se met à jour
                // Si la nouvelle catégorie doit être sélectionnée automatiquement après ajout:
                // updateFormState { it.copy(category = categoryName) }
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'ajout de la catégorie: ${e.message}")
            }
        }
    }
}

/**
 * État UI pour l'écran des dépenses
 */
data class ExpenseUiState(
    val formState: ExpenseFormState = ExpenseFormState(),
    val isSuccess: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

/**
 * État du formulaire de dépense
 */
data class ExpenseFormState(
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Date = Date(),
    val isRecurring: Boolean = false,
    val recurringFrequency: Int? = null,
    val notes: String = "",
    val attachmentUri: String? = null,
    val spendingLimitId: Long? = null
)
