package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.data.repositories.SavingsRepository
import com.cashruler.data.repositories.SpendingLimitRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.* // Pour Date

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val expenseRepository: ExpenseRepository = mockk()
    private val incomeRepository: IncomeRepository = mockk()
    private val savingsRepository: SavingsRepository = mockk()
    private val spendingLimitRepository: SpendingLimitRepository = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock les comportements par défaut pour l'initialisation du ViewModel
        coEvery { savingsRepository.getTotalSavedAmount() } returns flowOf(0.0)
        coEvery { incomeRepository.getTotalIncomesBetweenDates(any(), any()) } returns flowOf(0.0)
        coEvery { incomeRepository.getTotalNetIncomeBetweenDates(any(), any()) } returns flowOf(0.0)
        coEvery { expenseRepository.getTotalExpensesBetweenDates(any(), any()) } returns flowOf(0.0)
        coEvery { savingsRepository.getActiveProjects() } returns flowOf(emptyList())
        coEvery { spendingLimitRepository.getExceededLimits() } returns flowOf(emptyList())
        coEvery { spendingLimitRepository.getLimitsNearThreshold() } returns flowOf(emptyList())
        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(emptyList())
    }

    @Test
    fun `monthlyStats calculates current and previous month data correctly`() = runTest(testDispatcher) {
        // Arrange
        val currentMonthIncome = 1000.0
        val currentMonthNetIncome = 900.0
        val currentMonthExpenses = 500.0
        val previousMonthIncome = 800.0
        val previousMonthExpenses = 400.0
        val totalSaved = 2000.0

        // Initialiser le ViewModel ici pour utiliser les mocks de base pour les dates
        // car currentMonthDates et previousMonthDates sont initialisés dans le ViewModel
        // avant que nous puissions les mocker directement si le ViewModel est créé dans le @Before général.
        // Pour ce test spécifique, nous allons recréer le viewModel après avoir défini les mocks spécifiques.

        val tempViewModelForDates = DashboardViewModel( // ViewModel temporaire pour obtenir les dates calculées
            expenseRepository,
            incomeRepository,
            savingsRepository,
            spendingLimitRepository
        )
        val currentDates = tempViewModelForDates.currentMonthDates
        val previousDates = tempViewModelForDates.previousMonthDates // Utilise la propriété internal

        // Mock pour le mois courant
        coEvery { incomeRepository.getTotalIncomesBetweenDates(currentDates.first, currentDates.second) } returns flowOf(currentMonthIncome)
        coEvery { incomeRepository.getTotalNetIncomeBetweenDates(currentDates.first, currentDates.second) } returns flowOf(currentMonthNetIncome)
        coEvery { expenseRepository.getTotalExpensesBetweenDates(currentDates.first, currentDates.second) } returns flowOf(currentMonthExpenses)

        // Mock pour le mois précédent
        coEvery { incomeRepository.getTotalIncomesBetweenDates(previousDates.first, previousDates.second) } returns flowOf(previousMonthIncome)
        coEvery { expenseRepository.getTotalExpensesBetweenDates(previousDates.first, previousDates.second) } returns flowOf(previousMonthExpenses)
        
        coEvery { savingsRepository.getTotalSavedAmount() } returns flowOf(totalSaved)

        // Recréer le ViewModel avec les mocks configurés pour ce test.
        // Ceci assure que le bloc init utilise les mocks que nous venons de configurer.
        viewModel = DashboardViewModel(
            expenseRepository,
            incomeRepository,
            savingsRepository,
            spendingLimitRepository
        )

        // Act & Assert
        viewModel.monthlyStats.test {
            val emittedStats = awaitItem() 

            assertEquals(currentMonthIncome, emittedStats.income, 0.01)
            assertEquals(currentMonthNetIncome, emittedStats.netIncome, 0.01)
            assertEquals(currentMonthExpenses, emittedStats.expenses, 0.01)
            assertEquals(currentMonthNetIncome - currentMonthExpenses, emittedStats.balance, 0.01)
            assertEquals(previousMonthIncome, emittedStats.previousMonthIncome, 0.01)
            assertEquals(previousMonthExpenses, emittedStats.previousMonthExpenses, 0.01)
            assertEquals(totalSaved, emittedStats.totalSaved, 0.01)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
