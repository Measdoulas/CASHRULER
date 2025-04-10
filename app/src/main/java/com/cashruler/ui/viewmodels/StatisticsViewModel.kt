package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.repositories.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour les statistiques et analyses financières
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val savingsRepository: SavingsRepository,
    private val spendingLimitRepository: SpendingLimitRepository
) : ViewModel() {

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Période d'analyse sélectionnée
    private val _selectedPeriod = MutableStateFlow(AnalysisPeriod.MONTH)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    // Dates de la période sélectionnée
    private val _periodDates = MutableStateFlow(calculatePeriodDates(AnalysisPeriod.MONTH))
    val periodDates = _periodDates.asStateFlow()

    // Statistiques globales
    private val _stats = MutableStateFlow(GlobalStatistics())
    val stats = _stats.asStateFlow()

    // Répartition des dépenses par catégorie
    private val _expensesByCategory = MutableStateFlow<Map<String, Double>>(emptyMap())
    val expensesByCategory = _expensesByCategory.asStateFlow()

    // Répartition des revenus par type
    private val _incomesByType = MutableStateFlow<Map<String, Double>>(emptyMap())
    val incomesByType = _incomesByType.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * Change la période d'analyse
     */
    fun setPeriod(period: AnalysisPeriod) {
        _selectedPeriod.value = period
        _periodDates.value = calculatePeriodDates(period)
        loadStatistics()
    }

    /**
     * Charge les statistiques pour la période sélectionnée
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dates = _periodDates.value
                
                // Statistiques des revenus
                val totalIncome = incomeRepository.getTotalIncomesBetweenDates(
                    dates.first,
                    dates.second
                ).first()
                
                val totalNetIncome = incomeRepository.getTotalNetIncomeBetweenDates(
                    dates.first,
                    dates.second
                ).first()
                
                val totalTaxes = incomeRepository.getTotalTaxesBetweenDates(
                    dates.first,
                    dates.second
                ).first()

                // Répartition des revenus par type
                val incomeTypes = incomeRepository.getTotalIncomesByType(
                    dates.first,
                    dates.second
                ).first()
                _incomesByType.value = incomeTypes

                // Statistiques des dépenses
                val totalExpenses = expenseRepository.getTotalExpensesBetweenDates(
                    dates.first,
                    dates.second
                ).first()

                // Répartition des dépenses par catégorie
                val expenseCategories = expenseRepository.getTotalExpensesByCategory(
                    dates.first,
                    dates.second
                ).first()
                _expensesByCategory.value = expenseCategories

                // Statistiques d'épargne
                val savingsStats = savingsRepository.getGlobalStatistics().first()

                // Statistiques des limites de dépenses
                val limitStats = spendingLimitRepository.getGlobalStatistics().first()

                // Met à jour les statistiques globales
                _stats.update { 
                    GlobalStatistics(
                        totalIncome = totalIncome,
                        totalNetIncome = totalNetIncome,
                        totalTaxes = totalTaxes,
                        totalExpenses = totalExpenses,
                        balance = totalNetIncome - totalExpenses,
                        savingsAmount = savingsStats.totalSaved,
                        savingsTarget = savingsStats.totalTarget,
                        activeSavingsProjects = savingsStats.activeProjects,
                        activeLimits = limitStats.activeLimits,
                        exceededLimits = limitStats.exceededLimits
                    )
                }

            } catch (e: Exception) {
                _error.emit("Erreur lors du chargement des statistiques: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calcule les dates de début et fin pour une période donnée
     */
    private fun calculatePeriodDates(period: AnalysisPeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        when (period) {
            AnalysisPeriod.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            }
            AnalysisPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            AnalysisPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
            }
            AnalysisPeriod.ALL_TIME -> {
                calendar.set(2000, 0, 1) // Date arbitraire dans le passé
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    /**
     * Rafraîchit les statistiques
     */
    fun refresh() {
        loadStatistics()
    }
}

/**
 * Périodes d'analyse disponibles
 */
enum class AnalysisPeriod {
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

/**
 * Statistiques globales
 */
data class GlobalStatistics(
    val totalIncome: Double = 0.0,
    val totalNetIncome: Double = 0.0,
    val totalTaxes: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val savingsAmount: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val activeSavingsProjects: Int = 0,
    val activeLimits: Int = 0,
    val exceededLimits: Int = 0
) {
    /**
     * Calcule le taux d'épargne
     */
    fun getSavingsRate(): Float = if (totalNetIncome > 0) {
        ((totalNetIncome - totalExpenses) / totalNetIncome * 100).toFloat()
    } else 0f

    /**
     * Calcule le taux de dépenses
     */
    fun getExpenseRate(): Float = if (totalNetIncome > 0) {
        (totalExpenses / totalNetIncome * 100).toFloat()
    } else 0f

    /**
     * Calcule le taux d'imposition moyen
     */
    fun getTaxRate(): Float = if (totalIncome > 0) {
        (totalTaxes / totalIncome * 100).toFloat()
    } else 0f

    /**
     * Calcule le progrès vers l'objectif d'épargne
     */
    fun getSavingsProgress(): Float = if (savingsTarget > 0) {
        (savingsAmount / savingsTarget * 100).toFloat()
    } else 0f
}
