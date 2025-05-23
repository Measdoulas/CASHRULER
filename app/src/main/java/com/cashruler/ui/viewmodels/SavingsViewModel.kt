package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.repositories.SavingsRepository
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
    private val notificationManager: NotificationManager // Ajoute cette ligne
) : ViewModel() {

    // États UI
    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState = _uiState.asStateFlow()

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
    fun addAmount(projectId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                savingsRepository.addToProjectAmount(projectId, amount)
                // Après avoir ajouté le montant, vérifie si l'objectif est atteint
                val project = savingsRepository.getProjectById(projectId).firstOrNull()
                if (project != null && project.currentAmount >= project.targetAmount) {
                    // Vérifie aussi qu'on ne notifie pas plusieurs fois pour un objectif déjà signalé comme atteint.
                    // Pour cela, il faudrait un champ "isGoalAchievedNotified" dans SavingsProject.
                    // Pour l'instant, on notifie si l'objectif est atteint.
                    // Une amélioration serait de ne notifier qu'une seule fois.
                    notificationManager.showSavingsGoalAchieved(
                        notificationId = projectId.toInt(), // Utilise projectId comme base pour l'ID de notif
                        projectTitle = project.title,
                        targetAmount = project.targetAmount
                    )
                    // Optionnel: Désactiver les rappels pour ce projet si l'objectif est atteint
                    // notificationService.cancelSavingsReminder(projectId) // Nécessiterait d'injecter NotificationService
                }
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'ajout du montant: ${e.message}")
            }
        }
    }

    /**
     * Retire un montant d'un projet
     */
    fun subtractAmount(projectId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                savingsRepository.subtractFromProjectAmount(projectId, amount)
            } catch (e: Exception) {
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
            savingsRepository.getProjectById(id)
                .filterNotNull()
                .collect { project ->
                    _uiState.update { it.copy(formState = SavingsFormState(
                        title = project.title,
                        description = project.description,
                        targetAmount = project.targetAmount,
                        currentAmount = project.currentAmount,
                        startDate = project.startDate,
                        targetDate = project.targetDate,
                        periodicAmount = project.periodicAmount,
                        savingFrequency = project.savingFrequency,
                        isActive = project.isActive,
                        icon = project.icon,
                        notes = project.notes,
                        priority = project.priority
                    )) }
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
