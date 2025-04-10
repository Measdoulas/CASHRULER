package com.cashruler.ui.viewmodels

import com.cashruler.data.models.*
import com.cashruler.data.repositories.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var savingsRepository: SavingsRepository
    private lateinit var spendingLimitRepository: SpendingLimitRepository
    private lateinit var viewModel: StatisticsViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        expenseRepository = mockk()
        incomeRepository = mockk()
        savingsRepository = mockk()
        spendingLimitRepository = mockk()

        viewModel = StatisticsViewModel(
            expenseRepository = expenseRepository,
            incomeRepository = incomeRepository,
            savingsRepository = savingsRepository,
            spendingLimitRepository = spendingLimitRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        assertIs<StatisticsUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `loadStatistics updates state to Success with calculated statistics`() = runTest {
        // Given
        val expenses = listOf(
            createExpense(1, amount = 100.0, category = "Food"),
            createExpense(2, amount = 200.0, category = "Food"),
            createExpense(3, amount = 150.0, category = "Transport")
        )
        
        val incomes = listOf(
            createIncome(1, amount = 1000.0, type = "Salary"),
            createIncome(2, amount = 500.0, type = "Freelance")
        )

        val savingsProjects = listOf(
            createSavingsProject(1, currentAmount = 500.0, targetAmount = 1000.0),
            createSavingsProject(2, currentAmount = 200.0, targetAmount = 500.0)
        )

        val limits = listOf(
            createSpendingLimit(1, category = "Food", amount = 400.0),
            createSpendingLimit(2, category = "Transport", amount = 200.0)
        )

        coEvery { expenseRepository.getAllExpenses() } returns flowOf(expenses)
        coEvery { incomeRepository.getAllIncomes() } returns flowOf(incomes)
        coEvery { savingsRepository.getAllProjects() } returns flowOf(savingsProjects)
        coEvery { spendingLimitRepository.getAllLimits() } returns flowOf(limits)

        // When
        viewModel.loadStatistics()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertIs<StatisticsUiState.Success>(state)
        
        // Vérifier les statistiques des dépenses
        assertEquals(450.0, state.expenseStats.totalAmount)
        assertEquals(2, state.expenseStats.categoryBreakdown.size)
        assertEquals(300.0, state.expenseStats.categoryBreakdown.find { it.category == "Food" }?.amount)
        
        // Vérifier les statistiques des revenus
        assertEquals(1500.0, state.incomeStats.totalAmount)
        assertEquals(2, state.incomeStats.typeBreakdown.size)
        assertEquals(1000.0, state.incomeStats.typeBreakdown.find { it.type == "Salary" }?.amount)
        
        // Vérifier les statistiques d'épargne
        assertEquals(700.0, state.savingsStats.totalProgress.currentAmount)
        assertEquals(1500.0, state.savingsStats.totalProgress.targetAmount)
        assertEquals(2, state.savingsStats.projectStats.activeProjectsCount)
    }

    @Test
    fun `setPeriod updates selected period and reloads statistics`() = runTest {
        // Given
        coEvery { expenseRepository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { incomeRepository.getAllIncomes() } returns flowOf(emptyList())
        coEvery { savingsRepository.getAllProjects() } returns flowOf(emptyList())
        coEvery { spendingLimitRepository.getAllLimits() } returns flowOf(emptyList())

        // When
        viewModel.setPeriod(StatisticsPeriod.THIS_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(StatisticsPeriod.THIS_MONTH, viewModel.selectedPeriod.value)
        coVerify { 
            expenseRepository.getAllExpenses()
            incomeRepository.getAllIncomes()
            savingsRepository.getAllProjects()
            spendingLimitRepository.getAllLimits()
        }
    }

    private fun createExpense(
        id: Long,
        amount: Double,
        category: String,
        date: Date = Date()
    ) = Expense(id = id, amount = amount, category = category, title = "Test", date = date)

    private fun createIncome(
        id: Long,
        amount: Double,
        type: String,
        date: Date = Date()
    ) = Income(id = id, amount = amount, type = type, name = "Test", date = date)

    private fun createSavingsProject(
        id: Long,
        currentAmount: Double,
        targetAmount: Double,
        isActive: Boolean = true
    ) = SavingsProject(
        id = id,
        title = "Test",
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = Date(),
        deadline = if (isActive) Date(System.currentTimeMillis() + 86400000) else Date(0),
        frequency = SavingsFrequency.MONTHLY
    )

    private fun createSpendingLimit(
        id: Long,
        category: String,
        amount: Double
    ) = SpendingLimit(
        id = id,
        category = category,
        amount = amount,
        startDate = Date(),
        frequency = SpendingLimitFrequency.MONTHLY
    )
}
