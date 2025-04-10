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
 * ViewModel pour le tableau de bord
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
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

    // Statistiques du mois en cours
    private val _monthlyStats = MutableStateFlow(MonthlyStats())
    val monthlyStats = _monthlyStats.asStateFlow()

    // Date de début et fin du mois en cours
    private val currentMonthDates = calculateCurrentMonthDates()

    // Projets d'épargne actifs
    val activeSavingsProjects = savingsRepository.getActiveProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Limites de dépenses dépassées
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

    // Prochains revenus récurrents
    val upcomingIncomes = incomeRepository.getUpcomingRecurringIncomes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        monthlyStats()
    }

    private fun monthlyStats() = viewModelScope.launch {
            try {
                // Revenus du mois
                val monthlyIncome = incomeRepository.getTotalIncomesBetweenDates(
                    currentMonthDates.first,
                    currentMonthDates.second
                ).first()

                // Revenus nets du mois
                val monthlyNetIncome = incomeRepository.getTotalNetIncomeBetweenDates(
                    currentMonthDates.first,
                    currentMonthDates.second
                ).first()

                // Dépenses du mois
                val monthlyExpenses = expenseRepository.getTotalExpensesBetweenDates(
                    currentMonthDates.first,
                    currentMonthDates.second
                ).first()

                // Total épargné
                val totalSaved = savingsRepository.getTotalSavedAmount().first()

                // Met à jour les statistiques
                _monthlyStats.update { stats ->
                    stats.copy(
                        income = monthlyIncome,
                        netIncome = monthlyNetIncome,
                        expenses = monthlyExpenses,
                        balance = monthlyNetIncome - monthlyExpenses,
                        totalSaved = totalSaved
                    )
                }            } catch (e: Exception) {
                _error.emit("Erreur lors du chargement des statistiques: ${e.message}")
            }
        }
    }

    /**
     * Calcule les dates de début et fin du mois en cours
     */
    private fun calculateCurrentMonthDates(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        // Début du mois
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        // Fin du mois
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

/**
 * Statistiques mensuelles pour le tableau de bord
 */
data class MonthlyStats(
    val income: Double = 0.0,
    val netIncome: Double = 0.0,
    val expenses: Double = 0.0,
    val balance: Double = 0.0,
    val totalSaved: Double = 0.0
) {
    /**
     * Calcule le taux d'épargne du mois
     */
    fun getSavingsRate(): Float = if (netIncome > 0) {
        ((netIncome - expenses) / netIncome * 100).toFloat()
    } else 0f

    /**
     * Calcule le taux de dépenses du mois
     */
    fun getExpenseRate(): Float = if (netIncome > 0) {
        (expenses / netIncome * 100).toFloat()
    } else 0f
}
