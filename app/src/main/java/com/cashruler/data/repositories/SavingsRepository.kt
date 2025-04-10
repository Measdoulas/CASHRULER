package com.cashruler.data.repositories

import com.cashruler.data.dao.SavingsDao
import com.cashruler.data.models.SavingsProject
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des projets d'épargne
 */
@Singleton
class SavingsRepository @Inject constructor(
    private val savingsDao: SavingsDao,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Récupère tous les projets d'épargne
     */
    fun getAllProjects() = savingsDao.getAllProjects()
        .flowOn(dispatcher)

    /**
     * Récupère tous les projets d'épargne actifs
     */
    fun getActiveProjects() = savingsDao.getActiveProjects()
        .flowOn(dispatcher)

    /**
     * Récupère un projet par son ID
     */
    fun getProjectById(projectId: Long) = savingsDao.getProjectById(projectId)
        .flowOn(dispatcher)

    /**
     * Ajoute un nouveau projet d'épargne
     */
    suspend fun addProject(project: SavingsProject) = withContext(dispatcher) {
        savingsDao.insert(project)
    }

    /**
     * Met à jour un projet existant
     */
    suspend fun updateProject(project: SavingsProject) = withContext(dispatcher) {
        savingsDao.update(project)
    }

    /**
     * Supprime un projet
     */
    suspend fun deleteProject(project: SavingsProject) = withContext(dispatcher) {
        savingsDao.delete(project)
    }

    /**
     * Met à jour le montant actuel d'un projet
     */
    suspend fun updateProjectAmount(
        projectId: Long,
        amount: Double,
        date: Date = Date()
    ) = withContext(dispatcher) {
        savingsDao.updateProjectAmount(projectId, amount, date)
    }

    /**
     * Ajoute un montant au projet
     */
    suspend fun addToProjectAmount(
        projectId: Long,
        amount: Double,
        date: Date = Date()
    ) = withContext(dispatcher) {
        savingsDao.addToProjectAmount(projectId, amount, date)
    }

    /**
     * Retire un montant du projet
     */
    suspend fun subtractFromProjectAmount(
        projectId: Long,
        amount: Double,
        date: Date = Date()
    ) = withContext(dispatcher) {
        savingsDao.subtractFromProjectAmount(projectId, amount, date)
    }

    /**
     * Change l'état actif/inactif d'un projet
     */
    suspend fun setProjectActive(
        projectId: Long,
        isActive: Boolean,
        date: Date = Date()
    ) = withContext(dispatcher) {
        savingsDao.setProjectActive(projectId, isActive, date)
    }

    /**
     * Récupère le total épargné sur tous les projets actifs
     */
    fun getTotalSavedAmount() = savingsDao.getTotalSavedAmount()
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère le total des objectifs pour tous les projets actifs
     */
    fun getTotalTargetAmount() = savingsDao.getTotalTargetAmount()
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère les projets dont la date cible approche
     */
    fun getUpcomingDeadlines(
        currentDate: Date = Date(),
        daysAhead: Int = 30
    ): Flow<List<SavingsProject>> {
        val futureDate = Date(currentDate.time + (daysAhead * 24 * 60 * 60 * 1000L))
        return savingsDao.getUpcomingDeadlines(currentDate, futureDate)
            .flowOn(dispatcher)
    }

    /**
     * Récupère les projets en retard
     */
    fun getOverdueProjects(currentDate: Date = Date()) = 
        savingsDao.getOverdueProjects(currentDate)
            .flowOn(dispatcher)

    /**
     * Récupère les projets terminés
     */
    fun getCompletedProjects() = savingsDao.getCompletedProjects()
        .flowOn(dispatcher)

    /**
     * Récupère les statistiques globales des projets
     */
    fun getGlobalStatistics() = savingsDao.getGlobalStatistics()
        .flowOn(dispatcher)

    /**
     * Valide les champs d'un projet d'épargne
     */
    fun validateProject(project: SavingsProject): ValidationResult {
        val errors = mutableListOf<String>()

        if (!project.isValidTitle()) {
            errors.add("Le titre ne peut pas être vide")
        }
        if (!project.isValidDescription()) {
            errors.add("La description ne peut pas être vide")
        }
        if (!project.isValidTargetAmount()) {
            errors.add("Le montant cible doit être supérieur à 0")
        }
        if (!project.isValidCurrentAmount()) {
            errors.add("Le montant actuel doit être entre 0 et le montant cible")
        }
        if (!project.isValidPeriodicSaving()) {
            errors.add("La configuration d'épargne périodique est invalide")
        }
        if (!project.isValidDates()) {
            errors.add("La date cible doit être postérieure à la date de début")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Vérifie si un projet a atteint son objectif
     */
    fun isProjectCompleted(project: SavingsProject): Boolean {
        return project.currentAmount >= project.targetAmount ||
               (project.targetDate != null && project.targetDate.before(Date()))
    }

    suspend fun getAllProjectsList(): List<SavingsProject> = withContext(dispatcher) {
        savingsDao.getAllProjects().first()
    }

    /**
     * Calcule la prochaine occurrence d'un projet d'épargne périodique
     */
    fun calculateNextOccurrence(project: SavingsProject): Date? {
        if (project.periodicAmount == null || project.savingFrequency == null) return null

        return Date(System.currentTimeMillis() + (project.savingFrequency * 24 * 60 * 60 * 1000))
    }
}
