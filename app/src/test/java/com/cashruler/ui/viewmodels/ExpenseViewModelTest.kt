package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.data.repositories.ValidationResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class ExpenseViewModelTest {

    private lateinit var viewModel: ExpenseViewModel
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val spendingLimitRepository: SpendingLimitRepository = mockk(relaxed = true)
    
    // Using UnconfinedTestDispatcher for immediate execution
    // For more complex scenarios with coroutine scheduling, StandardTestDispatcher might be preferred.
    private val testDispatcher = UnconfinedTestDispatcher() 

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default behaviors for StateFlows exposed directly by the ViewModel or its dependencies
        coEvery { expenseRepository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { expenseRepository.getAllCategories() } returns flowOf(listOf("Food", "Transport"))
        
        // Initialize ViewModel here after setting up Dispatchers and default mocks
        viewModel = ExpenseViewModel(expenseRepository, spendingLimitRepository)
    }

    @Test
    fun `initial uiState is correct`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        assertEquals(ExpenseFormState(), initialState.formState)
        assertFalse(initialState.isSuccess)
        assertTrue(initialState.validationErrors.isEmpty())
    }

    @Test
    fun `updateFormState updates formState and validationErrors for invalid amount`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0, description = "Test", category = "Food") }
        val uiState = viewModel.uiState.value
        assertEquals(0.0, uiState.formState.amount, 0.001)
        assertTrue(uiState.validationErrors.containsKey("amount"))
    }

    @Test
    fun `updateFormState updates formState and validationErrors for blank description`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 10.0, description = "", category = "Food") }
        val uiState = viewModel.uiState.value
        assertEquals("", uiState.formState.description)
        assertTrue(uiState.validationErrors.containsKey("description"))
    }
    
    @Test
    fun `updateFormState updates formState and validationErrors for blank category`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 10.0, description = "Test", category = "") }
        val uiState = viewModel.uiState.value
        assertEquals("", uiState.formState.category)
        assertTrue(uiState.validationErrors.containsKey("category"))
    }

    @Test
    fun `updateFormState updates formState and validationErrors for invalid recurringFrequency`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(isRecurring = true, recurringFrequency = 0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("recurringFrequency"))
        
        viewModel.updateFormState { it.copy(isRecurring = true, recurringFrequency = 400) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("recurringFrequency"))
    }

    @Test
    fun `updateFormState clears validationErrors for valid input`() = runTest(testDispatcher) {
        // First, create an invalid state
        viewModel.updateFormState { it.copy(amount = 0.0) }
        assertTrue(viewModel.uiState.value.validationErrors.isNotEmpty())

        // Then, update to a valid state
        viewModel.updateFormState { it.copy(amount = 50.0, description = "Valid Desc", category = "Food") }
        assertTrue(viewModel.uiState.value.validationErrors.isEmpty())
    }

    @Test
    fun `addExpense success case`() = runTest(testDispatcher) {
        val validExpenseForm = ExpenseFormState(amount = 100.0, description = "Dinner", category = "Food", date = Date(), spendingLimitId = 1L)
        viewModel.updateFormState { validExpenseForm }

        val expenseMatcher = slot<Expense>()
        coEvery { expenseRepository.validateExpense(capture(expenseMatcher)) } returns ValidationResult.Success
        coEvery { expenseRepository.addExpense(any()) } returns 1L // Simulate successful add returning an ID
        coEvery { spendingLimitRepository.addToSpentAmount(1L, 100.0) } just Runs

        viewModel.addExpense()

        viewModel.uiState.test {
            val emittedState = awaitItem() // Current state after addExpense call
            assertTrue(emittedState.isSuccess)
            assertEquals(ExpenseFormState(), emittedState.formState) // Form is reset
            coVerify { expenseRepository.addExpense(expenseMatcher.captured) }
            coVerify { spendingLimitRepository.addToSpentAmount(1L, 100.0) }
            assertEquals("Dinner", expenseMatcher.captured.description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addExpense form validation failure`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0) } // Invalid state

        viewModel.error.test {
            viewModel.addExpense()
            assertEquals("Veuillez corriger les erreurs du formulaire", awaitItem())
            coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `addExpense repository validation failure`() = runTest(testDispatcher) {
        val validExpenseForm = ExpenseFormState(amount = 100.0, description = "Dinner", category = "Food")
        viewModel.updateFormState { validExpenseForm }

        coEvery { expenseRepository.validateExpense(any()) } returns ValidationResult.Error(listOf("Repo error"))

        viewModel.error.test {
            viewModel.addExpense()
            assertEquals("Repo error", awaitItem())
            coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }
    
    @Test
    fun `updateExpense success case`() = runTest(testDispatcher) {
        val expenseId = 1L
        val updatedForm = ExpenseFormState(amount = 150.0, description = "Updated Dinner", category = "Food")
        viewModel.updateFormState { updatedForm }

        val expenseMatcher = slot<Expense>()
        coEvery { expenseRepository.validateExpense(capture(expenseMatcher)) } returns ValidationResult.Success
        coEvery { expenseRepository.updateExpense(any()) } just Runs
        // Assuming no change to spendingLimit on update for simplicity, or mock it if logic exists

        viewModel.updateExpense(expenseId)

        viewModel.uiState.test {
            val emittedState = awaitItem()
            assertTrue(emittedState.isSuccess)
            assertEquals(ExpenseFormState(), emittedState.formState) // Form reset
            coVerify { expenseRepository.updateExpense(expenseMatcher.captured) }
            assertEquals(expenseId, expenseMatcher.captured.id)
            assertEquals("Updated Dinner", expenseMatcher.captured.description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateExpense form validation failure`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0) } // Invalid state

        viewModel.error.test {
            viewModel.updateExpense(1L)
            assertEquals("Veuillez corriger les erreurs du formulaire", awaitItem())
            coVerify(exactly = 0) { expenseRepository.updateExpense(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `updateExpense repository validation failure`() = runTest(testDispatcher) {
        val validForm = ExpenseFormState(amount = 100.0, description = "Valid", category = "Cat")
        viewModel.updateFormState { validForm }
        coEvery { expenseRepository.validateExpense(any()) } returns ValidationResult.Error(listOf("Update repo error"))

        viewModel.error.test {
            viewModel.updateExpense(1L)
            assertEquals("Update repo error", awaitItem())
            coVerify(exactly = 0) { expenseRepository.updateExpense(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `loadExpense updates formState`() = runTest(testDispatcher) {
        val expenseId = 1L
        val mockDate = Date()
        val mockExpense = Expense(id = expenseId, amount = 200.0, description = "Loaded", category = "Misc", date = mockDate)
        coEvery { expenseRepository.getExpenseById(expenseId) } returns flowOf(mockExpense)

        viewModel.loadExpense(expenseId)
        
        val expectedFormState = ExpenseFormState(
            amount = 200.0,
            description = "Loaded",
            category = "Misc",
            date = mockDate,
            isRecurring = false, // Default from Expense model
            recurringFrequency = null, // Default
            notes = null, // Default
            attachmentUri = null, // Default
            spendingLimitId = null // Default
        )
        assertEquals(expectedFormState, viewModel.uiState.value.formState)
    }

    @Test
    fun `deleteExpense calls repository and updates spending limit`() = runTest(testDispatcher) {
        val expenseToDelete = Expense(id = 1L, amount = 50.0, description = "Delete", category = "Test", date = Date(), spendingLimitId = 2L)
        coEvery { expenseRepository.deleteExpense(expenseToDelete) } just Runs
        coEvery { spendingLimitRepository.addToSpentAmount(2L, -50.0) } just Runs

        viewModel.deleteExpense(expenseToDelete)

        coVerify { expenseRepository.deleteExpense(expenseToDelete) }
        coVerify { spendingLimitRepository.addToSpentAmount(2L, -50.0) }
        // Test error emission if delete fails (requires mocking repo to throw exception)
    }
    
    @Test
    fun `deleteExpense without spendingLimitId calls repository only`() = runTest(testDispatcher) {
        val expenseToDelete = Expense(id = 1L, amount = 50.0, description = "Delete", category = "Test", date = Date(), spendingLimitId = null)
        coEvery { expenseRepository.deleteExpense(expenseToDelete) } just Runs

        viewModel.deleteExpense(expenseToDelete)

        coVerify { expenseRepository.deleteExpense(expenseToDelete) }
        coVerify(exactly = 0) { spendingLimitRepository.addToSpentAmount(any(), any()) }
    }

    @Test
    fun `deleteExpense handles repository exception`() = runTest(testDispatcher) {
        val expenseToDelete = Expense(id = 1L, amount = 50.0, description = "Delete", category = "Test", date = Date())
        val exceptionMessage = "Database error"
        coEvery { expenseRepository.deleteExpense(expenseToDelete) } throws RuntimeException(exceptionMessage)

        viewModel.error.test {
            viewModel.deleteExpense(expenseToDelete)
            val emittedError = awaitItem()
            assertTrue(emittedError.contains(exceptionMessage))
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `addNewCategory success`() = runTest(testDispatcher) {
        val newCategoryName = "New Category"
        coEvery { expenseRepository.getAllCategories() } returns flowOf(listOf("Food")) // Initial categories
        coEvery { expenseRepository.addCategory(newCategoryName) } just Runs
        
        // Re-initialize viewModel to pick up the new getAllCategories mock for this specific test if needed,
        // or ensure the existing one is flexible enough (e.g. if it's a MutableStateFlow in the repo mock)
        // For this test, we assume the initial `coEvery` in `setUp` is sufficient if `allCategories`
        // in ViewModel is properly collecting from the repo's flow.

        viewModel.addNewCategory(newCategoryName)
        
        coVerify { expenseRepository.addCategory(newCategoryName) }
        // To verify UI update, you might need to mock `expenseRepository.getAllCategories()`
        // to emit a new list after `addCategory` is called, then test `viewModel.allCategories`.
        // This depends on how `allCategories` in ViewModel is implemented.
        // If `allCategories` is a direct `stateIn` from `expenseRepository.getAllCategories()`,
        // then mocking `expenseRepository.getAllCategories()` to return a new flow or emit to a
        // BehaviorSubject/StateFlow in the mock repo would be needed for this part of the test.
        // For simplicity, this test focuses on the call to `addCategory`.
    }

    @Test
    fun `addNewCategory blank name emits error`() = runTest(testDispatcher) {
        viewModel.error.test {
            viewModel.addNewCategory("  ")
            assertEquals("Le nom de la catégorie ne peut pas être vide.", awaitItem())
            coVerify(exactly = 0) { expenseRepository.addCategory(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addNewCategory existing category emits error`() = runTest(testDispatcher) {
        val existingCategory = "Food"
        coEvery { expenseRepository.getAllCategories() } returns flowOf(listOf("Food", "Transport"))
        // Reinitialize viewModel if the allCategories state was cached internally from a previous call in setUp
        // viewModel = ExpenseViewModel(expenseRepository, spendingLimitRepository) // Potentially needed

        viewModel.error.test {
            viewModel.addNewCategory(existingCategory)
            assertEquals("La catégorie '$existingCategory' existe déjà.", awaitItem())
            coVerify(exactly = 0) { expenseRepository.addCategory(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `resetForm resets uiState`() = runTest(testDispatcher) {
        // Setup some non-default state
        viewModel.updateFormState { it.copy(amount = 10.0, description = "Test", isSuccess = true) }
        assertNotEquals(ExpenseFormState(), viewModel.uiState.value.formState)
        assertTrue(viewModel.uiState.value.isSuccess)

        viewModel.resetForm()

        val finalState = viewModel.uiState.value
        assertEquals(ExpenseFormState(), finalState.formState)
        assertFalse(finalState.isSuccess)
        assertTrue(finalState.validationErrors.isEmpty())
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Clear all mocks if necessary, though usually not needed with fresh mocks per test class
        // clearAllMocks() 
    }
}
