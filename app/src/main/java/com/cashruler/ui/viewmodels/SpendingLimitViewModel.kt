package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.SpendingLimit
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.data.repositories.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour la gestion des limites de dépenses
 */
@HiltViewModel
class SpendingLimitViewModel @Inject constructor(
    private val spendingLimitRepository: SpendingLimitRepository
) : ViewModel() {

    // États UI
    private val _uiState = MutableStateFlow(SpendingLimitUiState())
    val uiState = _uiState.asStateFlow()

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Limites actives
    val activeLimits = spendingLimitRepository.getActiveLimits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Limites dépassées
    val exceededLimits = spendingLimitRepository.getExceededLimits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Limites approchant leur seuil
    val limitsNearThreshold = spendingLimitRepository.getLimitsNearThreshold()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Statistiques globales
    val globalStats = spendingLimitRepository.getGlobalStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Réinitialise les périodes expirées au démarrage
        resetExpiredPeriods()
    }

    /**
     * Met à jour l'état du formulaire
     */
    fun updateFormState(update: (SpendingLimitFormState) -> SpendingLimitFormState) {
        _uiState.update { it.copy(formState = update(it.formState)) }
    }

    /**
     * Ajoute une nouvelle limite
     */
    fun addLimit() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val limit = createLimitFromForm()
                when (val result = spendingLimitRepository.validateLimit(limit)) {
                    is ValidationResult.Success -> {
                        spendingLimitRepository.addLimit(limit)
                        _uiState.update { it.copy(
                            formState = SpendingLimitFormState(),
                            isSuccess = true
                        ) }
                    }
                    is ValidationResult.Error -> {
                        _error.emit(result.errors.joinToString("\n"))
                    }
                }
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'ajout de la limite: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Met à jour une limite existante
     */
    fun updateLimit(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val limit = createLimitFromForm().copy(id = id)
                when (val result = spendingLimitRepository.validateLimit(limit)) {
                    is ValidationResult.Success -> {
                        spendingLimitRepository.updateLimit(limit)
                        _uiState.update { it.copy(
                            formState = SpendingLimitFormState(),
                            isSuccess = true
                        ) }
                    }
                    is ValidationResult.Error -> {
                        _error.emit(result.errors.joinToString("\n"))
                    }
                }
            } catch (e: Exception) {
                _error.emit("Erreur lors de la mise à jour de la limite: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Charge une limite pour édition
     */
    fun loadLimit(id: Long) {
        viewModelScope.launch {
            spendingLimitRepository.getLimitById(id)
                .filterNotNull()
                .collect { limit ->
                    _uiState.update { it.copy(formState = SpendingLimitFormState(
                        category = limit.category,
                        amount = limit.amount,
                        periodInDays = limit.periodInDays,
                        currentPeriodStart = limit.currentPeriodStart,
                        currentSpent = limit.currentSpent,
                        enableNotifications = limit.enableNotifications,
                        warningThreshold = limit.warningThreshold,
                        notes = limit.notes,
                        isActive = limit.isActive
                    )) }
                }
        }
    }

    /**
     * Active/Désactive une limite
     */
    fun setLimitActive(limitId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                spendingLimitRepository.setLimitActive(limitId, isActive)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement d'état de la limite: ${e.message}")
            }
        }
    }

    /**
     * Met à jour le montant dépensé
     */
    fun updateSpentAmount(limitId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                spendingLimitRepository.updateSpentAmount(limitId, amount)
            } catch (e: Exception) {
                _error.emit("Erreur lors de la mise à jour du montant: ${e.message}")
            }
        }
    }

    /**
     * Réinitialise les périodes expirées
     */
    private fun resetExpiredPeriods() {
        viewModelScope.launch {
            try {
                spendingLimitRepository.resetExpiredPeriods()
            } catch (e: Exception) {
                _error.emit("Erreur lors de la réinitialisation des périodes: ${e.message}")
            }
        }
    }

    /**
     * Réinitialise le formulaire
     */
    fun resetForm() {
        _uiState.update { it.copy(
            formState = SpendingLimitFormState(),
            isSuccess = false
        ) }
    }

    private fun createLimitFromForm(): SpendingLimit {
        return with(_uiState.value.formState) {
            SpendingLimit(
                category = category,
                amount = amount,
                periodInDays = periodInDays,
                currentPeriodStart = currentPeriodStart,
                currentSpent = currentSpent,
                enableNotifications = enableNotifications,
                warningThreshold = warningThreshold,
                notes = notes.takeIf { it.isNotBlank() },
                isActive = isActive
            )
        }
    }
}

/**
 * État UI pour l'écran des limites
 */
data class SpendingLimitUiState(
    val formState: SpendingLimitFormState = SpendingLimitFormState(),
    val isSuccess: Boolean = false
)

/**
 * État du formulaire de limite de dépenses
 */
data class SpendingLimitFormState(
    val category: String = "",
    val amount: Double = 0.0,
    val periodInDays: Int = SpendingLimit.PERIOD_MONTHLY,
    val currentPeriodStart: Date = Date(),
    val currentSpent: Double = 0.0,
    val enableNotifications: Boolean = true,
    val warningThreshold: Int = 80,
    val notes: String = "",
    val isActive: Boolean = true
)
