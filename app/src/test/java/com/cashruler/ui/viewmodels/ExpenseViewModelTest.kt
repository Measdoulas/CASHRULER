package com.cashruler.ui.viewmodels

import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        repository = mockk()
        viewModel = ExpenseViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        assertIs<ExpenseUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun `loadExpenses updates state to Success with expenses`() = runTest {
        // Given
        val expenses = listOf(createExpense(1), createExpense(2))
        coEvery { repository.getAllExpenses() } returns flowOf(expenses)

        // When
        viewModel.loadExpenses()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertIs<ExpenseUiState.Success>(state)
        assertEquals(expenses, state.expenses)
    }

    @Test
    fun `addExpense calls repository and shows success message`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { repository.addExpense(expense) } returns Unit
        var successCalled = false
        var errorCalled = false

        // When
        viewModel.addExpense(
            expense = expense,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.addExpense(expense) }
        assertEquals(true, successCalled)
        assertEquals(false, errorCalled)
    }

    @Test
    fun `addExpense shows error when repository throws exception`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { repository.addExpense(expense) } throws Exception("Error")
        var successCalled = false
        var errorCalled = false

        // When
        viewModel.addExpense(
            expense = expense,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.addExpense(expense) }
        assertEquals(false, successCalled)
        assertEquals(true, errorCalled)
    }

    @Test
    fun `updateExpense calls repository and shows success message`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { repository.updateExpense(expense) } returns Unit
        var successCalled = false
        var errorCalled = false

        // When
        viewModel.updateExpense(
            expense = expense,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.updateExpense(expense) }
        assertEquals(true, successCalled)
        assertEquals(false, errorCalled)
    }

    @Test
    fun `deleteExpense calls repository and shows success message`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { repository.deleteExpense(expense) } returns Unit
        var successCalled = false
        var errorCalled = false

        // When
        viewModel.deleteExpense(
            expense = expense,
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.deleteExpense(expense) }
        assertEquals(true, successCalled)
        assertEquals(false, errorCalled)
    }

    @Test
    fun `getExpenseById returns selected expense`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { repository.getExpenseById(1) } returns flowOf(expense)

        // When
        viewModel.selectExpense(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(expense, viewModel.selectedExpense.value)
    }

    private fun createExpense(
        id: Long,
        amount: Double = 100.0,
        title: String = "Test Expense",
        category: String = "Food",
        date: Date = Date()
    ) = Expense(
        id = id,
        amount = amount,
        title = title,
        category = category,
        date = date
    )
}
