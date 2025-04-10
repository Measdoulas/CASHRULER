package com.cashruler.data.repositories

import com.cashruler.data.dao.SpendingLimitDao
import com.cashruler.data.models.SpendingLimit
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des limites de dépenses
 */
@Singleton
class SpendingLimitRepository @Inject constructor(
    private val spendingLimitDao: SpendingLimitDao,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Récupère toutes les limites de dépenses
     */
    fun getAllLimits() = spendingLimitDao.getAllLimits()
        .flowOn(dispatcher)

    /**
     * Récupère toutes les limites de dépenses actives
     */
    fun getActiveLimits() = spendingLimitDao.getActiveLimits()
        .flowOn(dispatcher)

    /**
     * Récupère une limite par son ID
     */
    fun getLimitById(limitId: Long) = spendingLimitDao.getLimitById(limitId)
        .flowOn(dispatcher)

    /**
     * Ajoute une nouvelle limite de dépenses
     */
    suspend fun addLimit(limit: SpendingLimit) = withContext(dispatcher) {
        spendingLimitDao.insert(limit)
    }

    /**
     * Met à jour une limite existante
     */
    suspend fun updateLimit(limit: SpendingLimit) = withContext(dispatcher) {
        spendingLimitDao.update(limit)
    }

    /**
     * Supprime une limite
     */
    suspend fun deleteLimit(limit: SpendingLimit) = withContext(dispatcher) {
        spendingLimitDao.delete(limit)
    }

    /**
     * Met à jour le montant dépensé d'une limite
     */
    suspend fun updateSpentAmount(
        limitId: Long,
        amount: Double,
        date: Date = Date()
    ) = withContext(dispatcher) {
        spendingLimitDao.updateSpentAmount(limitId, amount, date)
    }

    /**
     * Ajoute un montant aux dépenses d'une limite
     */
    suspend fun addToSpentAmount(
        limitId: Long,
        amount: Double,
        date: Date = Date()
    ) = withContext(dispatcher) {
        spendingLimitDao.addToSpentAmount(limitId, amount, date)
    }

    /**
     * Change l'état actif/inactif d'une limite
     */
    suspend fun setLimitActive(
        limitId: Long,
        isActive: Boolean,
        date: Date = Date()
    ) = withContext(dispatcher) {
        spendingLimitDao.setLimitActive(limitId, isActive, date)
    }

    /**
     * Récupère les limites par catégorie
     */
    fun getLimitsByCategory(category: String) = spendingLimitDao.getLimitsByCategory(category)
        .flowOn(dispatcher)

    /**
     * Récupère les limites dépassées
     */
    fun getExceededLimits() = spendingLimitDao.getExceededLimits()
        .flowOn(dispatcher)

    /**
     * Récupère les limites approchant leur seuil d'alerte
     */
    fun getLimitsNearThreshold() = spendingLimitDao.getLimitsNearThreshold()
        .flowOn(dispatcher)

    /**
     * Réinitialise les montants dépensés des limites dont la période est terminée
     */
    suspend fun resetExpiredPeriods(
        newPeriodStart: Date = Date(),
        date: Date = Date()
    ) = withContext(dispatcher) {
        spendingLimitDao.resetExpiredPeriods(newPeriodStart, date)
    }

    /**
     * Récupère les statistiques globales des limites
     */
    fun getGlobalStatistics() = spendingLimitDao.getGlobalStatistics()
        .flowOn(dispatcher)

    /**
     * Valide les champs d'une limite de dépenses
     */
    fun validateLimit(limit: SpendingLimit): ValidationResult {
        val errors = mutableListOf<String>()

        if (!limit.isValidCategory()) {
            errors.add("La catégorie ne peut pas être vide")
        }
        if (!limit.isValidAmount()) {
            errors.add("Le montant limite doit être supérieur à 0")
        }
        if (!limit.isValidPeriod()) {
            errors.add("La période doit être supérieure à 0")
        }
        if (!limit.isValidWarningThreshold()) {
            errors.add("Le seuil d'alerte doit être entre 0 et 100")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Vérifie si une limite est dépassée
     */
    fun isLimitExceeded(limit: SpendingLimit): Boolean {
        return limit.currentSpent > limit.amount
    }

    /**
     * Vérifie si une limite approche du seuil d'alerte
     */
    fun isNearWarningThreshold(limit: SpendingLimit): Boolean {
        return limit.enableNotifications &&
               limit.getUsagePercentage() >= limit.warningThreshold &&
               limit.currentSpent <= limit.amount
    }

    suspend fun getAllLimitsList(): List<SpendingLimit> = withContext(dispatcher) {
        spendingLimitDao.getAllLimits().first()
    }

}
