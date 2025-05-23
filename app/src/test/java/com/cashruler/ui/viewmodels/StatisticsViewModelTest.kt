package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.repositories.* // Import all from here
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class StatisticsViewModelTest {

    private lateinit var viewModel: StatisticsViewModel
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val savingsRepository: SavingsRepository = mockk(relaxed = true)
    private val spendingLimitRepository: SpendingLimitRepository = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    // Helper function to setup mocks for a given period's expected values
    private fun setupRepositoryMocks(
        totalIncome: Double, netIncome: Double, taxes: Double, incomeByType: Map<String, Double>,
        totalExpenses: Double, expensesByCategory: Map<String, Double>,
        savingsGlobalStats: SavingsRepository.GlobalStatistics, // Adjusted type
        limitGlobalStats: SpendingLimitRepository.GlobalStatistics // Adjusted type
    ) {
        coEvery { incomeRepository.getTotalIncomesBetweenDates(any(), any()) } returns flowOf(totalIncome)
        coEvery { incomeRepository.getTotalNetIncomeBetweenDates(any(), any()) } returns flowOf(netIncome)
        coEvery { incomeRepository.getTotalTaxesBetweenDates(any(), any()) } returns flowOf(taxes)
        coEvery { incomeRepository.getTotalIncomesByType(any(), any()) } returns flowOf(incomeByType)
        coEvery { expenseRepository.getTotalExpensesBetweenDates(any(), any()) } returns flowOf(totalExpenses)
        coEvery { expenseRepository.getTotalExpensesByCategory(any(), any()) } returns flowOf(expensesByCategory)
        coEvery { savingsRepository.getGlobalStatistics() } returns flowOf(savingsGlobalStats)
        coEvery { spendingLimitRepository.getGlobalStatistics() } returns flowOf(limitGlobalStats)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default mocks for AnalysisPeriod.MONTH (initial load)
        setupRepositoryMocks(
            totalIncome = 1000.0, netIncome = 800.0, taxes = 200.0, incomeByType = mapOf("Salary" to 1000.0),
            totalExpenses = 500.0, expensesByCategory = mapOf("Food" to 300.0, "Transport" to 200.0),
            savingsGlobalStats = SavingsRepository.GlobalStatistics(totalSaved = 100.0, totalTarget = 500.0, activeProjects = 1),
            limitGlobalStats = SpendingLimitRepository.GlobalStatistics(activeLimits = 2, exceededLimits = 0)
        )
        viewModel = StatisticsViewModel(expenseRepository, incomeRepository, savingsRepository, spendingLimitRepository)
        // Advance dispatcher after ViewModel init to allow init block to complete
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `initial state and data load on init are correct`() = runTest(testDispatcher) {
        assertFalse(viewModel.isLoading.value) // Should be false after init's loadStatistics
        assertEquals(AnalysisPeriod.MONTH, viewModel.selectedPeriod.value)

        viewModel.stats.test {
            val currentStats = awaitItem()
            assertEquals(1000.0, currentStats.totalIncome, 0.01)
            assertEquals(800.0, currentStats.totalNetIncome, 0.01)
            assertEquals(200.0, currentStats.totalTaxes, 0.01)
            assertEquals(500.0, currentStats.totalExpenses, 0.01)
            assertEquals(300.0, currentStats.balance, 0.01) // 800 - 500
            assertEquals(100.0, currentStats.savingsAmount, 0.01)
            assertEquals(500.0, currentStats.savingsTarget, 0.01)
            assertEquals(1, currentStats.activeSavingsProjects)
            assertEquals(2, currentStats.activeLimits)
            assertEquals(0, currentStats.exceededLimits)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(mapOf("Food" to 300.0, "Transport" to 200.0), viewModel.expensesByCategory.value)
        assertEquals(mapOf("Salary" to 1000.0), viewModel.incomesByType.value)
    }

    @Test
    fun `setPeriod updates period and reloads statistics with new data`() = runTest(testDispatcher) {
        // New values for AnalysisPeriod.YEAR
        val yearTotalIncome = 12000.0
        val yearNetIncome = 9600.0
        val yearTaxes = 2400.0
        val yearIncomeByType = mapOf("Salary" to 12000.0)
        val yearTotalExpenses = 6000.0
        val yearExpensesByCategory = mapOf("Rent" to 3000.0, "Food" to 3000.0)
        // Savings and Limit stats might be independent of period in this ViewModel's loadStatistics,
        // but we can change them to ensure they are re-fetched if needed.
        val yearSavingsStats = SavingsRepository.GlobalStatistics(totalSaved = 1200.0, totalTarget = 6000.0, activeProjects = 2)
        val yearLimitStats = SpendingLimitRepository.GlobalStatistics(activeLimits = 3, exceededLimits = 1)

        // Re-mock for YEAR period BEFORE calling setPeriod
        setupRepositoryMocks(
            totalIncome = yearTotalIncome, netIncome = yearNetIncome, taxes = yearTaxes, incomeByType = yearIncomeByType,
            totalExpenses = yearTotalExpenses, expensesByCategory = yearExpensesByCategory,
            savingsGlobalStats = yearSavingsStats, limitGlobalStats = yearLimitStats
        )

        viewModel.setPeriod(AnalysisPeriod.YEAR)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines launched by setPeriod complete

        assertFalse(viewModel.isLoading.value)
        assertEquals(AnalysisPeriod.YEAR, viewModel.selectedPeriod.value)

        viewModel.stats.test {
            val currentStats = awaitItem() // Get the latest emitted value
            assertEquals(yearTotalIncome, currentStats.totalIncome, 0.01)
            assertEquals(yearNetIncome, currentStats.totalNetIncome, 0.01)
            assertEquals(yearTotalExpenses, currentStats.totalExpenses, 0.01)
            assertEquals(yearNetIncome - yearTotalExpenses, currentStats.balance, 0.01)
            assertEquals(yearSavingsStats.totalSaved, currentStats.savingsAmount, 0.01)
             assertEquals(yearLimitStats.activeLimits, currentStats.activeLimits)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(yearExpensesByCategory, viewModel.expensesByCategory.value)
        assertEquals(yearIncomeByType, viewModel.incomesByType.value)

        // Verify that repository methods were called again.
        // Check at least one to confirm re-fetching. Exact date matching is complex, `any()` is used.
        coVerify(atLeast = 2) { incomeRepository.getTotalIncomesBetweenDates(any(), any()) } // Initial + setPeriod
    }

    @Test
    fun `loadStatistics handles repository error and updates error flow`() = runTest(testDispatcher) {
        val errorMessage = "Database connection failed"
        // Mock one of the repo calls to throw an exception
        coEvery { incomeRepository.getTotalIncomesBetweenDates(any(), any()) } throws RuntimeException(errorMessage)

        // Re-setup default mocks for other successful calls if they are not relaxed or if specific behavior is needed
        // For this test, relaxed mocks will return defaults, but the failing one is explicit.
        // We need to ensure other parts of loadStatistics don't fail before the targeted one.
        // This is usually fine if the failing mock is early in the loadStatistics sequence.

        viewModel.error.test {
            viewModel.refresh() // Trigger loadStatistics
            testDispatcher.scheduler.advanceUntilIdle()

            val emittedError = awaitItem()
            assertTrue(emittedError.contains(errorMessage))
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.isLoading.value) // isLoading should be reset
    }
    
    @Test
    fun `isLoading is true during data loading and false after`() = runTest(testDispatcher) {
        // Test initial load (covered by setUp and advanceUntilIdle)
        assertFalse("isLoading should be false after initial load", viewModel.isLoading.value)

        // Test loading during setPeriod
        // Re-mock for a new period to ensure data fetching happens
        setupRepositoryMocks(
            totalIncome = 2000.0, netIncome = 1600.0, taxes = 400.0, incomeByType = mapOf("Freelance" to 2000.0),
            totalExpenses = 1000.0, expensesByCategory = mapOf("Utilities" to 500.0, "Groceries" to 500.0),
            savingsGlobalStats = SavingsRepository.GlobalStatistics(totalSaved = 200.0, totalTarget = 1000.0, activeProjects = 1),
            limitGlobalStats = SpendingLimitRepository.GlobalStatistics(activeLimits = 1, exceededLimits = 0)
        )
        
        // viewModel.isLoading should be false before calling setPeriod
        assertFalse("isLoading should be false before setPeriod", viewModel.isLoading.value)

        // Call setPeriod without advancing the dispatcher immediately
        viewModel.setPeriod(AnalysisPeriod.WEEK)
        
        // viewModel.isLoading should now be true as loadStatistics starts
        assertTrue("isLoading should be true immediately after setPeriod call", viewModel.isLoading.value)
        
        // Advance the dispatcher to allow loadStatistics to complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        // viewModel.isLoading should be false again after loadStatistics completes
        assertFalse("isLoading should be false after setPeriod loadStatistics completes", viewModel.isLoading.value)
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // clearAllMocks() // Consider if necessary, usually not for fresh mocks per test class
    }
}
```
