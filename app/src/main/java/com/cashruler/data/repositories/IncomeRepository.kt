package com.cashruler.data.repositories

import com.cashruler.data.dao.IncomeDao
import com.cashruler.data.models.Income
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des revenus
 */
@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Récupère tous les revenus
     */
    fun getAllIncomes() = incomeDao.getAllIncomes()
        .flowOn(dispatcher)

    /**
     * Récupère un revenu par son ID
     */
    fun getIncomeById(id: Long) = incomeDao.getIncomeById(id)
        .flowOn(dispatcher)

    /**
     * Récupère les revenus sur une période donnée
     */
    fun getIncomesBetweenDates(
        startDate: Date,
        endDate: Date
    ) = incomeDao.getIncomesBetweenDates(startDate, endDate)
        .flowOn(dispatcher)

    /**
     * Récupère les revenus par type
     */
    fun getIncomesByType(type: String) = incomeDao.getIncomesByType(type)
        .flowOn(dispatcher)

    /**
     * Récupère le total des revenus sur une période
     */
    fun getTotalIncomesBetweenDates(
        startDate: Date,
        endDate: Date
    ) = incomeDao.getTotalIncomesBetweenDates(startDate, endDate)
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère le total des revenus nets (après impôts) sur une période
     */
    fun getTotalNetIncomeBetweenDates(
        startDate: Date,
        endDate: Date
    ) = incomeDao.getTotalNetIncomeBetweenDates(startDate, endDate)
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère le total des revenus par type sur une période
     */
    fun getTotalIncomesByType(
        startDate: Date,
        endDate: Date
    ) = incomeDao.getTotalIncomesByType(startDate, endDate)
        .flowOn(dispatcher)

    /**
     * Récupère les revenus récurrents
     */
    fun getRecurringIncomes() = incomeDao.getRecurringIncomes()
        .flowOn(dispatcher)

    /**
     * Récupère les revenus imposables
     */
    fun getTaxableIncomes() = incomeDao.getTaxableIncomes()
        .flowOn(dispatcher)

    /**
     * Calcule le total des impôts sur une période
     */
    fun getTotalTaxesBetweenDates(
        startDate: Date,
        endDate: Date
    ) = incomeDao.getTotalTaxesBetweenDates(startDate, endDate)
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère les revenus avec la prochaine occurrence prévue
     */
    fun getUpcomingRecurringIncomes(currentDate: Date = Date()) = 
        incomeDao.getUpcomingRecurringIncomes(currentDate)
            .flowOn(dispatcher)

    /**
     * Récupère tous les types distincts utilisés
     */
    fun getAllTypes() = incomeDao.getAllTypes()
        .flowOn(dispatcher)

    /**
     * Ajoute un nouveau revenu
     */
    suspend fun addIncome(income: Income) = withContext(dispatcher) {
        incomeDao.insert(income)
    }

    /**
     * Met à jour un revenu existant
     */
    suspend fun updateIncome(income: Income) = withContext(dispatcher) {
        incomeDao.update(income)
    }

    /**
     * Supprime un revenu
     */
    suspend fun deleteIncome(income: Income) = withContext(dispatcher) {
        incomeDao.delete(income)
    }

    /**
     * Supprime tous les revenus
     */
    suspend fun deleteAllIncomes() = withContext(dispatcher) {
        incomeDao.deleteAll()
    }

    /**
     * Valide les champs d'un revenu
     */
    fun validateIncome(income: Income): ValidationResult {
        val errors = mutableListOf<String>()

        if (!income.isValidAmount()) {
            errors.add("Le montant doit être supérieur à 0")
        }
        if (!income.isValidDescription()) {
            errors.add("La description ne peut pas être vide")
        }
        if (!income.isValidType()) {
            errors.add("Le type ne peut pas être vide")
        }
        if (!income.isValidRecurrence()) {
            errors.add("La configuration de récurrence est invalide")
        }
        if (!income.isValidTaxation()) {
            errors.add("La configuration fiscale est invalide")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Calcule la prochaine occurrence d'un revenu récurrent
     */
    fun calculateNextOccurrence(income: Income): Date? {
        if (!income.isRecurring || income.recurringFrequency == null) {
            return null
        }

        return Date(income.date.time + (income.recurringFrequency * 24 * 60 * 60 * 1000L))
    }
}
