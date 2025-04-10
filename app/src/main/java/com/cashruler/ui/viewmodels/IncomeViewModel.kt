package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.Income
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.data.repositories.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour la gestion des revenus
 */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    // États UI
    private val _uiState = MutableStateFlow(IncomeUiState())
    val uiState = _uiState.asStateFlow()

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Revenus
    val allIncomes = incomeRepository.getAllIncomes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Types de revenu
    val allTypes = incomeRepository.getAllTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Prochains revenus récurrents
    val upcomingRecurringIncomes = incomeRepository.getUpcomingRecurringIncomes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Met à jour l'état du formulaire et valide les champs
     */
    fun updateFormState(update: (IncomeFormState) -> IncomeFormState) {
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
    private fun validateFormState(formState: IncomeFormState): Map<String, String> {
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
        
        // Validation du type
        if (formState.type.isBlank()) {
            errors["type"] = "Le type est requis"
        }
        
        // Validation de la récurrence
        if (formState.isRecurring) {
            if (formState.recurringFrequency == null || formState.recurringFrequency <= 0) {
                errors["recurringFrequency"] = "La fréquence doit être supérieure à 0 jours"
            } else if (formState.recurringFrequency > 365) {
                errors["recurringFrequency"] = "La fréquence ne peut pas dépasser 365 jours"
            }
        }
        
        // Validation de la taxation
        if (formState.isTaxable) {
            if (formState.taxRate == null) {
                errors["taxRate"] = "Le taux d'imposition est requis pour un revenu imposable"
            } else if (formState.taxRate < 0 || formState.taxRate > 100) {
                errors["taxRate"] = "Le taux d'imposition doit être entre 0 et 100%"
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
     * Ajoute un nouveau revenu
     */
    fun addIncome() {
        if (!isFormValid()) {
            viewModelScope.launch {
                _error.emit("Veuillez corriger les erreurs du formulaire")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val formState = _uiState.value.formState
            val income = Income(
                amount = formState.amount,
                description = formState.description,
                type = formState.type,
                date = formState.date,
                isRecurring = formState.isRecurring,
                recurringFrequency = formState.recurringFrequency,
                isTaxable = formState.isTaxable,
                taxRate = formState.taxRate,
                val income = createIncomeFromForm()
                when (val result = incomeRepository.validateIncome(income)) {
                    is ValidationResult.Success -> {
                        val newIncome = income.copy(
                            nextOccurrence = if (income.isRecurring) {
                                incomeRepository.calculateNextOccurrence(income)
                            } else null
                        )
                        incomeRepository.addIncome(newIncome)
                        _uiState.update { it.copy(
                            formState = IncomeFormState(),
                            isSuccess = true,
                            validationErrors = emptyMap()
                        ) }
                    }
                    is ValidationResult.Error -> {
                        _error.emit(result.errors.joinToString("\n"))
                    }
            }.invokeOnCompletion {
                _error.emit("Erreur lors de l'ajout du revenu: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Met à jour un revenu
     */
    fun updateIncome(id: Long) {
        if (!isFormValid()) {
            viewModelScope.launch {
                _error.emit("Veuillez corriger les erreurs du formulaire")
            }
            return
        }y {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val income = createIncomeFromForm().copy(id = id)
                when (val result = incomeRepository.validateIncome(income)) {
                    is ValidationResult.Success -> {
                        val updatedIncome = income.copy(
                            nextOccurrence = if (income.isRecurring) {
                                incomeRepository.calculateNextOccurrence(income)
                            } else null
                        )
                        incomeRepository.updateIncome(updatedIncome)
                        _uiState.update { it.copy(
                            formState = IncomeFormState(),
                            isSuccess = true,
                            validationErrors = emptyMap()
                        ) }
                    }
                    is ValidationResult.Error -> {
                        _error.emit(result.errors.joinToString("\n"))
                    }
            }.invokeOnCompletion {
                _error.emit("Erreur lors de la mise à jour du revenu: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Supprime un revenu
     */
    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            try {
                incomeRepository.deleteIncome(income)
            } catch (e: Exception) {
                _error.emit("Erreur lors de la suppression du revenu: ${e.message}")
            }
        }
    }

    /**
     * Charge un revenu pour édition
     */
    fun loadIncome(id: Long) {
        viewModelScope.launch {
            incomeRepository.getIncomeById(id)
                .filterNotNull()
                .collect { income ->
                    _uiState.update { it.copy(
                        formState = IncomeFormState(
                            amount = income.amount,
                            description = income.description,
                            type = income.type,
                            date = income.date,
                            isRecurring = income.isRecurring,
                            recurringFrequency = income.recurringFrequency,
                            isTaxable = income.isTaxable,
                            taxRate = income.taxRate,
                            notes = income.notes,
                            attachmentUri = income.attachmentUri
                        ),
                        validationErrors = emptyMap()
                    ) }
                }
        }
    }

    /**
     * Réinitialise le formulaire
     */
    fun resetForm() {
        _uiState.update { it.copy(
            formState = IncomeFormState(),
            isSuccess = false,
            validationErrors = emptyMap()
        ) }
    }

    private suspend fun handleIncome(income: Income, id: Long? = null) {
        val incomeToHandle = if (id != null) income.copy(id = id) else income
        when (val result = incomeRepository.validateIncome(incomeToHandle)) {
            is ValidationResult.Success -> {
                val updatedIncome = incomeToHandle.copy(nextOccurrence = if (incomeToHandle.isRecurring) incomeRepository.calculateNextOccurrence(incomeToHandle) else null)
                if (id != null) incomeRepository.updateIncome(updatedIncome) else incomeRepository.addIncome(updatedIncome)
                _uiState.update { it.copy(formState = IncomeFormState(), isSuccess = true, validationErrors = emptyMap()) }
            }
            is ValidationResult.Error -> _error.emit(result.errors.joinToString("\n"))
        }
    }
private fun createIncomeFromForm(): Income {
    val formState = _uiState.value.formState
    return Income(amount = formState.amount, description = formState.description, type = formState.type, date = formState.date, isRecurring = formState.isRecurring, recurringFrequency = formState.recurringFrequency, isTaxable = formState.isTaxable, taxRate = formState.taxRate, notes = formState.notes.takeIf { it.isNotBlank() }, attachmentUri = formState.attachmentUri)
    }
}

/**
 * État UI pour l'écran des revenus
 */
data class IncomeUiState(
    val formState: IncomeFormState = IncomeFormState(),
    val isSuccess: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

/**
 * État du formulaire de revenu
 */
data class IncomeFormState(
    val amount: Double = 0.0,
    val description: String = "",
    val type: String = Income.DEFAULT_TYPES.first(),
    val date: Date = Date(),
    val isRecurring: Boolean = false,
    val recurringFrequency: Int? = null,
    val isTaxable: Boolean = true,
    val taxRate: Double? = null,
    val notes: String = "",
    val attachmentUri: String? = null
)
