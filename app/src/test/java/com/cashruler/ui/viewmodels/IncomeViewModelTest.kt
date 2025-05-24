package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.models.Income
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.data.repositories.ValidationResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date

@ExperimentalCoroutinesApi
class IncomeViewModelTest {

    private lateinit var viewModel: IncomeViewModel
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher() 

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mock flows exposed by the ViewModel or collected during init
        coEvery { incomeRepository.getAllIncomes() } returns flowOf(emptyList())
        coEvery { incomeRepository.getAllTypes() } returns flowOf(listOf("Salary", "Freelance"))
        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(emptyList())
        
        viewModel = IncomeViewModel(incomeRepository)
        // Advance dispatcher after ViewModel init if init block launches coroutines for collection
        // For this ViewModel, it seems like these are StateIn/SharedIn, so immediate collection might not start
        // unless there's a subscriber. `advanceUntilIdle` is safe.
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `initial uiState is correct`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        assertEquals(IncomeFormState(), initialState.formState)
        assertFalse(initialState.isSuccess)
        assertTrue(initialState.validationErrors.isEmpty())
    }

    @Test
    fun `updateFormState updates formState and validationErrors for invalid amount`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0, description = "Test", type = "Salary") }
        val uiState = viewModel.uiState.value
        assertEquals(0.0, uiState.formState.amount, 0.001)
        assertTrue(uiState.validationErrors.containsKey("amount"))
    }

    @Test
    fun `updateFormState updates formState and validationErrors for blank description`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 100.0, description = "  ", type = "Salary") }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("description"))
    }
    
    @Test
    fun `updateFormState updates formState and validationErrors for blank type`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 100.0, description = "Test", type = "") }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("type"))
    }

    @Test
    fun `updateFormState updates formState and validationErrors for invalid recurringFrequency`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(isRecurring = true, recurringFrequency = 0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("recurringFrequency"))
        
        viewModel.updateFormState { it.copy(isRecurring = true, recurringFrequency = 400) } // Assuming max 365
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("recurringFrequency"))
    }
    
    @Test
    fun `updateFormState updates formState and validationErrors for invalid taxRate`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(isTaxable = true, taxRate = -1.0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("taxRate"))

        viewModel.updateFormState { it.copy(isTaxable = true, taxRate = 101.0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("taxRate"))
    }

    @Test
    fun `updateFormState clears validationErrors for valid input`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0) } // Invalid state
        assertTrue(viewModel.uiState.value.validationErrors.isNotEmpty())

        viewModel.updateFormState { it.copy(amount = 500.0, description = "Valid Salary", type = "Salary") }
        assertTrue(viewModel.uiState.value.validationErrors.isEmpty())
    }

    @Test
    fun `addIncome success non-recurring`() = runTest(testDispatcher) {
        val validForm = IncomeFormState(amount = 1000.0, description = "Project X", type = "Freelance", isTaxable = true, taxRate = 10.0)
        viewModel.updateFormState { validForm }

        val incomeMatcher = slot<Income>()
        coEvery { incomeRepository.validateIncome(capture(incomeMatcher)) } returns ValidationResult.Success
        coEvery { incomeRepository.addIncome(any()) } returns 1L // Simulate successful add

        viewModel.addIncome()
        testDispatcher.scheduler.advanceUntilIdle()


        viewModel.uiState.test {
            val emittedState = awaitItem()
            assertTrue(emittedState.isSuccess)
            assertEquals(IncomeFormState(), emittedState.formState) // Form is reset
            coVerify { incomeRepository.addIncome(incomeMatcher.captured) }
            assertEquals("Project X", incomeMatcher.captured.description)
            assertEquals(1000.0 * (1 - 10.0/100.0), incomeMatcher.captured.netAmount, 0.01)
            assertNull(incomeMatcher.captured.nextOccurrenceDate) // Non-recurring
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `addIncome success recurring`() = runTest(testDispatcher) {
        val nextOccurrence = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.time
        val validForm = IncomeFormState(
            amount = 2000.0, description = "Salary", type = "Employment", 
            isRecurring = true, recurringFrequency = 30
        )
        viewModel.updateFormState { validForm }

        val incomeMatcher = slot<Income>()
        coEvery { incomeRepository.validateIncome(capture(incomeMatcher)) } returns ValidationResult.Success
        coEvery { incomeRepository.calculateNextOccurrence(any(), any()) } returns nextOccurrence
        coEvery { incomeRepository.addIncome(any()) } returns 1L

        viewModel.addIncome()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val emittedState = awaitItem()
            assertTrue(emittedState.isSuccess)
            coVerify { incomeRepository.addIncome(incomeMatcher.captured) }
            assertEquals(nextOccurrence, incomeMatcher.captured.nextOccurrenceDate)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `addIncome form validation failure`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 0.0) } // Invalid state

        viewModel.error.test {
            viewModel.addIncome()
            // No need to advance dispatcher if error is emitted synchronously or viewModelScope uses Unconfined
            assertEquals("Veuillez corriger les erreurs du formulaire", awaitItem())
            coVerify(exactly = 0) { incomeRepository.addIncome(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `addIncome repository validation failure`() = runTest(testDispatcher) {
        val validForm = IncomeFormState(amount = 100.0, description = "Valid", type = "Gift")
        viewModel.updateFormState { validForm }
        coEvery { incomeRepository.validateIncome(any()) } returns ValidationResult.Error(listOf("Repo error"))

        viewModel.error.test {
            viewModel.addIncome()
            assertEquals("Repo error", awaitItem())
            coVerify(exactly = 0) { incomeRepository.addIncome(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }
    
    @Test
    fun `updateIncome success`() = runTest(testDispatcher) {
        val incomeId = 1L
        val updatedForm = IncomeFormState(amount = 1200.0, description = "Updated Project", type = "Freelance")
        viewModel.updateFormState { updatedForm }

        val incomeMatcher = slot<Income>()
        coEvery { incomeRepository.validateIncome(capture(incomeMatcher)) } returns ValidationResult.Success
        coEvery { incomeRepository.updateIncome(any()) } just Runs

        viewModel.updateIncome(incomeId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val emittedState = awaitItem()
            assertTrue(emittedState.isSuccess)
            assertEquals(IncomeFormState(), emittedState.formState) // Form reset
            coVerify { incomeRepository.updateIncome(incomeMatcher.captured) }
            assertEquals(incomeId, incomeMatcher.captured.id)
            assertEquals("Updated Project", incomeMatcher.captured.description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadIncome updates formState`() = runTest(testDispatcher) {
        val incomeId = 1L
        val mockDate = Date()
        val mockIncome = Income(
            id = incomeId, amount = 2500.0, description = "Consulting", type = "Work", 
            date = mockDate, isTaxable = true, taxRate = 15.0
        )
        coEvery { incomeRepository.getIncomeById(incomeId) } returns flowOf(mockIncome)

        viewModel.loadIncome(incomeId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val expectedFormState = IncomeFormState(
            amount = 2500.0, description = "Consulting", type = "Work", date = mockDate,
            isRecurring = false, recurringFrequency = null, 
            isTaxable = true, taxRate = 15.0, notes = null, attachmentUri = null
        )
        assertEquals(expectedFormState, viewModel.uiState.value.formState)
    }

    @Test
    fun `deleteIncome calls repository`() = runTest(testDispatcher) {
        val incomeToDelete = Income(id = 1L, amount = 100.0, description = "Old", type = "Misc", date = Date())
        coEvery { incomeRepository.deleteIncome(incomeToDelete) } just Runs

        viewModel.deleteIncome(incomeToDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { incomeRepository.deleteIncome(incomeToDelete) }
    }

    @Test
    fun `deleteIncome handles repository exception`() = runTest(testDispatcher) {
        val incomeToDelete = Income(id = 1L, amount = 100.0, description = "Fail Delete", type = "Test", date = Date())
        val exceptionMessage = "DB delete error"
        coEvery { incomeRepository.deleteIncome(incomeToDelete) } throws RuntimeException(exceptionMessage)

        viewModel.error.test {
            viewModel.deleteIncome(incomeToDelete)
            testDispatcher.scheduler.advanceUntilIdle()
            val emittedError = awaitItem()
            assertTrue(emittedError.contains(exceptionMessage))
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `resetForm resets uiState`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(amount = 123.0, description = "Some Desc", isSuccess = true) }
        assertNotEquals(IncomeFormState(), viewModel.uiState.value.formState)
        assertTrue(viewModel.uiState.value.isSuccess)

        viewModel.resetForm()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertEquals(IncomeFormState(), finalState.formState)
        assertFalse(finalState.isSuccess)
        assertTrue(finalState.validationErrors.isEmpty())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // clearAllMocks() // Usually not needed if mocks are recreated per test or test class
    }
}
