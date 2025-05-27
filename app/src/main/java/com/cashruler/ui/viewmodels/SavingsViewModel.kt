package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SavingsTransaction // Added
import com.cashruler.data.repositories.SavingsRepository
import com.cashruler.data.repositories.SavingsTransactionRepositoryInterface // Added
import com.cashruler.data.repositories.ValidationResult
import com.cashruler.notifications.NotificationManager // Ajoute cet import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour la gestion des projets d'épargne
 */
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val savingsTransactionRepository: SavingsTransactionRepositoryInterface, // Added
    private val notificationManager: NotificationManager // Ajoute cette ligne
) : ViewModel() {

    // États UI
    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState = _uiState.asStateFlow()

    // Transactions for the current project
    private val _currentProjectTransactions = MutableStateFlow<List<SavingsTransaction>>(emptyList())
    val currentProjectTransactions = _currentProjectTransactions.asStateFlow()

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Projets d'épargne
    val activeProjects = savingsRepository.getActiveProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Total épargné
    val totalSavedAmount = savingsRepository.getTotalSavedAmount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Projets à échéance proche
    val upcomingDeadlines = savingsRepository.getUpcomingDeadlines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Projets en retard
    val overdueProjects = savingsRepository.getOverdueProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Projets terminés
    val completedProjects = savingsRepository.getCompletedProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Met à jour l'état du formulaire
     */
    fun updateFormState(update: (SavingsFormState) -> SavingsFormState) {
        _uiState.update { currentState ->
            val newFormState = update(currentState.formState)
            val validationErrors = validateFormState(newFormState)
            currentState.copy(
                formState = newFormState,
                validationErrors = validationErrors)
        }
    }

    /**
     * Ajoute un nouveau projet d'épargne
     */
    fun addProject() {
        viewModelScope.launch {
            _isLoading.value = true
            val project = createProjectFromForm()
            handleProject(project)
        }.invokeOnCompletion { _isLoading.value = false }
    }
    private fun isFormValid(): Boolean {
        return _uiState.value.validationErrors.isEmpty()
    }

    /**
     * Met à jour un projet existant
     */
    fun updateProject(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val project = createProjectFromForm().copy(id = id)
            handleProject(project, id)
        }.invokeOnCompletion { _isLoading.value = false }
    }

    /**
     * Validate the form state
     */
    private fun validateFormState(formState: SavingsFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (formState.title.isBlank()) {
            errors["title"] = "Le titre est requis"
        }

        if (formState.description.isBlank()) {
            errors["description"] = "La description est requise"
        }

        if (formState.targetAmount <= 0) {
            errors["targetAmount"] = "Le montant cible doit être supérieur à 0"
        }

        if (formState.periodicAmount != null && formState.periodicAmount <= 0) {
            errors["periodicAmount"] = "Le montant périodique doit être supérieur à 0"
        }

        if (formState.savingFrequency != null && formState.savingFrequency <= 0) {
            errors["savingFrequency"] = "La fréquence d'épargne doit être supérieure à 0"
        }

        return errors
    }

    private suspend fun handleProject(project: SavingsProject, id: Long? = null) {
        if (!isFormValid()) {
            _error.emit("Veuillez corriger les erreurs du formulaire")
            return
        }

        val projectToHandle = if (id != null) project.copy(id = id) else project

        try {
            if (id != null) {
                savingsRepository.updateProject(projectToHandle)
            } else {
                savingsRepository.addProject(projectToHandle)
            }
            _uiState.update { it.copy(formState = SavingsFormState(), isSuccess = true, validationErrors = emptyMap()) }
        } catch (e: Exception) {
            val action = if (id != null) "mettre à jour" else "ajouter"
            _error.emit("Erreur lors de l'action de $action le projet: ${e.message}")
        }
    }

            try {
    /**
     * Ajoute un montant à un projet
     */
    fun addAmount(projectId: Long, amount: Double, description: String = "Deposit") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transaction = SavingsTransaction(
                    projectId = projectId,
                    amount = amount, // Ensure positive for deposit logic in repo/DAO if needed, though type handles it
                    transactionDate = Date(),
                    type = SavingsTransaction.TYPE_DEPOSIT,
                    description = description,
                    createdAt = Date()
                )
                savingsTransactionRepository.addTransaction(transaction)

                // Update SavingsProject.currentAmount
                val newTotal = savingsTransactionRepository.getTotalAmountForProject(projectId)
                val projectToUpdate = savingsRepository.getProjectById(projectId).firstOrNull()
                if (projectToUpdate != null) {
                    val updatedProject = projectToUpdate.copy(currentAmount = newTotal)
                    savingsRepository.updateProject(updatedProject) // This updates the project in DB

                    // Check for goal achievement after updating currentAmount
                    if (updatedProject.currentAmount >= updatedProject.targetAmount && !updatedProject.isGoalAchievedNotified) {
                        notificationManager.showSavingsGoalAchieved(
                            notificationId = projectId.toInt(),
                            projectTitle = updatedProject.title,
                            targetAmount = updatedProject.targetAmount
                        )
                        savingsRepository.markGoalAchievedNotified(projectId)
                    }
                }
                 loadTransactionsForProject(projectId) // Refresh transactions list
                _isLoading.value = false
                _error.emit("Deposit successful.")
            } catch (e: Exception) {
                _isLoading.value = false
                _error.emit("Erreur lors de l'ajout du montant: ${e.message}")
            }
        }
    }

    /**
     * Retire un montant d'un projet
     */
    fun subtractAmount(projectId: Long, amount: Double, description: String = "Withdrawal") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transaction = SavingsTransaction(
                    projectId = projectId,
                    amount = amount, // Ensure positive, type handles direction
                    transactionDate = Date(),
                    type = SavingsTransaction.TYPE_WITHDRAWAL,
                    description = description,
                    createdAt = Date()
                )
                savingsTransactionRepository.addTransaction(transaction)

                // Update SavingsProject.currentAmount
                val newTotal = savingsTransactionRepository.getTotalAmountForProject(projectId)
                val projectToUpdate = savingsRepository.getProjectById(projectId).firstOrNull()
                if (projectToUpdate != null) {
                     val updatedProject = projectToUpdate.copy(currentAmount = newTotal)
                    savingsRepository.updateProject(updatedProject)
                }
                loadTransactionsForProject(projectId) // Refresh transactions list
                _isLoading.value = false
                _error.emit("Withdrawal successful.")
            } catch (e: Exception)
            {
                _isLoading.value = false
                _error.emit("Erreur lors du retrait du montant: ${e.message}")
            }
        }
    }

    /**
     * Active/Désactive un projet
     */
    fun setProjectActive(projectId: Long, isActive: Boolean) {
        viewModelScope.launch {
            try {
                savingsRepository.setProjectActive(projectId, isActive)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement d'état du projet: ${e.message}")
            }
        }
    }

    /**
     * Charge un projet pour édition
     */
    fun loadProject(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            savingsRepository.getProjectById(id)
                .filterNotNull()
                .collect { project ->
                    // Recalculate currentAmount based on transactions when loading project
                    val calculatedCurrentAmount = savingsTransactionRepository.getTotalAmountForProject(project.id)
                    val projectWithCorrectedAmount = if (project.currentAmount != calculatedCurrentAmount) {
                        project.copy(currentAmount = calculatedCurrentAmount)
                        // Optionally update the project in DB if discrepancy is found and should be corrected
                        // savingsRepository.updateProject(project.copy(currentAmount = calculatedCurrentAmount))
                    } else {
                        project
                    }

                    _uiState.update {
                        it.copy(
                            formState = SavingsFormState(
                                title = projectWithCorrectedAmount.title,
                                description = projectWithCorrectedAmount.description,
                                targetAmount = projectWithCorrectedAmount.targetAmount,
                                currentAmount = projectWithCorrectedAmount.currentAmount, // Use calculated amount
                                startDate = projectWithCorrectedAmount.startDate,
                                targetDate = projectWithCorrectedAmount.targetDate,
                                periodicAmount = projectWithCorrectedAmount.periodicAmount,
                                savingFrequency = projectWithCorrectedAmount.savingFrequency,
                                isActive = projectWithCorrectedAmount.isActive,
                                icon = projectWithCorrectedAmount.icon,
                                notes = projectWithCorrectedAmount.notes,
                                priority = projectWithCorrectedAmount.priority
                            )
                        )
                    }
                    loadTransactionsForProject(id) // Load transactions after project details are loaded
                    _isLoading.value = false
                }
        }
    }

    fun loadTransactionsForProject(projectId: Long) {
        viewModelScope.launch {
            savingsTransactionRepository.getTransactionsForProject(projectId)
                .catch { e ->
                    _error.emit("Failed to load transactions: ${e.message}")
                }
                .collect { transactions ->
                    _currentProjectTransactions.value = transactions
                }
        }
    }

    /**
     * Réinitialise le formulaire
     */
    fun resetForm() {
        _uiState.update { it.copy(
            formState = SavingsFormState(),
            isSuccess = false
        ) }
    }

    private fun createProjectFromForm(): SavingsProject {
        return with(_uiState.value.formState) {
            SavingsProject(
                title = title,
                description = description,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                startDate = startDate,
                targetDate = targetDate,
                periodicAmount = periodicAmount,
                savingFrequency = savingFrequency,
                isActive = isActive,
                icon = icon,
                notes = notes.takeIf { it.isNotBlank() },
                priority = priority
            )
        }
    }
}

/**
 * État UI pour l'écran d'épargne
 */
data class SavingsUiState(
    val formState: SavingsFormState = SavingsFormState(),
    val isSuccess: Boolean = false
)

/**
 * État du formulaire de projet d'épargne
 */
data class SavingsFormState(
    val title: String = "",
    val description: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val startDate: Date = Date(),
    val targetDate: Date? = null,
    val periodicAmount: Double? = null,
    val savingFrequency: Int? = null,
    val isActive: Boolean = true,
    val icon: String? = null,
    val notes: String = "",
    val priority: Int = 0
)
