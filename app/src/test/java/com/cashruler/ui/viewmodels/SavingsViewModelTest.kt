package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SavingsTransaction
import com.cashruler.data.repositories.SavingsRepositoryInterface // Changed to interface
import com.cashruler.data.repositories.SavingsTransactionRepositoryInterface // Added
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow // For creating flows in tests
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class SavingsViewModelTest {

    private lateinit var viewModel: SavingsViewModel
    private val savingsRepository: SavingsRepositoryInterface = mockk(relaxed = true) // Changed to interface
    private val savingsTransactionRepository: SavingsTransactionRepositoryInterface = mockk(relaxed = true) // Added
    private val notificationManager: com.cashruler.notifications.NotificationManager = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher() 

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mock flows collected in ViewModel's init or exposed as StateFlows
        coEvery { savingsRepository.getActiveProjects() } returns flowOf(emptyList())
        coEvery { savingsRepository.getCompletedProjects() } returns flowOf(emptyList())
        coEvery { savingsRepository.getTotalSavedAmount() } returns flowOf(0.0)
        
        viewModel = SavingsViewModel(savingsRepository, savingsTransactionRepository, notificationManager) // Updated constructor
        testDispatcher.scheduler.advanceUntilIdle() // Ensure init collections are processed
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks() // Clear mocks after each test
    }

    @Test
    fun `initial uiState, activeProjects, completedProjects, and totalSavedAmount are correct`() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.value
        assertEquals(SavingsFormState(), initialState.projectFormState)
        assertFalse(initialState.isSuccess)
        assertTrue(initialState.validationErrors.isEmpty())

        viewModel.activeProjects.test {
            assertEquals(emptyList<SavingsProject>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.completedProjects.test {
            assertEquals(emptyList<SavingsProject>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.totalSavedAmount.test {
            assertEquals(0.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateFormState updates projectFormState and validationErrors for blank title`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(title = "") }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("title"))
    }

    @Test
    fun `updateFormState updates projectFormState and validationErrors for invalid targetAmount`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(targetAmount = 0.0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("targetAmount"))
    }
    
    @Test
    fun `updateFormState updates projectFormState and validationErrors for invalid periodicAmount`() = runTest(testDispatcher) {
        // periodicAmount <= 0 when savingFrequency is set
        viewModel.updateFormState { it.copy(periodicAmount = 0.0, savingFrequency = 7) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("periodicAmount"))
    }

    @Test
    fun `updateFormState updates projectFormState and validationErrors for invalid savingFrequency`() = runTest(testDispatcher) {
        // savingFrequency <= 0 when periodicAmount is set and > 0
        viewModel.updateFormState { it.copy(periodicAmount = 100.0, savingFrequency = 0) }
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("savingFrequency"))
    }

    @Test
    fun `updateFormState clears validationErrors for valid input`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(title = "") } // Invalid state
        assertTrue(viewModel.uiState.value.validationErrors.isNotEmpty())

        viewModel.updateFormState { it.copy(title = "Vacation", targetAmount = 1000.0) }
        assertTrue(viewModel.uiState.value.validationErrors.isEmpty())
    }

    @Test
    fun `addProject success case`() = runTest(testDispatcher) {
        val validForm = SavingsFormState(title = "New Laptop", targetAmount = 1500.0, currentAmount = 50.0)
        viewModel.updateFormState { validForm }

        val projectMatcher = slot<SavingsProject>()
        coEvery { savingsRepository.addProject(capture(projectMatcher)) } returns 1L // Simulate successful add

        viewModel.addProject()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val emittedState = awaitItem() // Get current state after add
            assertTrue(emittedState.isSuccess)
            assertEquals(SavingsFormState(), emittedState.projectFormState) // Form is reset
            coVerify { savingsRepository.addProject(projectMatcher.captured) }
            assertEquals("New Laptop", projectMatcher.captured.title)
            assertEquals(1500.0, projectMatcher.captured.targetAmount, 0.01)
            // currentAmount in form is not directly used for new project's currentAmount in ViewModel logic
            assertEquals(0.0, projectMatcher.captured.currentAmount, 0.01) 
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addProject form validation failure`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(title = "") } // Invalid state

        viewModel.error.test {
            viewModel.addProject()
            assertEquals("Veuillez corriger les erreurs du formulaire.", awaitItem())
            coVerify(exactly = 0) { savingsRepository.addProject(any()) }
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSuccess)
    }
    
    @Test
    fun `updateProject success case`() = runTest(testDispatcher) {
        val projectId = 1L
        val updatedForm = SavingsFormState(title = "Updated Laptop", targetAmount = 1600.0)
        viewModel.updateFormState { updatedForm }

        val projectMatcher = slot<SavingsProject>()
        coEvery { savingsRepository.updateProject(capture(projectMatcher)) } just Runs

        viewModel.updateProject(projectId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val emittedState = awaitItem()
            assertTrue(emittedState.isSuccess)
            assertEquals(SavingsFormState(), emittedState.projectFormState) // Form reset
            coVerify { savingsRepository.updateProject(projectMatcher.captured) }
            assertEquals(projectId, projectMatcher.captured.id)
            assertEquals("Updated Laptop", projectMatcher.captured.title)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `loadProject updates projectFormState and loads transactions`() = runTest(testDispatcher) {
        val projectId = 1L
        val mockDate = Date()
        val initialCurrentAmount = 100.0
        val totalFromTransactions = 120.0 // Simulate transactions sum
        val mockProject = SavingsProject(
            id = projectId, title = "Test Project", description = "Desc",
            targetAmount = 1000.0, currentAmount = initialCurrentAmount, // This will be updated
            startDate = mockDate, isGoalAchievedNotified = false
        )
        val mockTransactions = listOf(SavingsTransaction(projectId = projectId, amount = 120.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))

        coEvery { savingsRepository.getProjectById(projectId) } returns flowOf(mockProject)
        coEvery { savingsTransactionRepository.getTotalAmountForProject(projectId) } returns totalFromTransactions
        coEvery { savingsTransactionRepository.getTransactionsForProject(projectId) } returns flowOf(mockTransactions)

        viewModel.loadProject(projectId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Test Project", state.formState.title)
            assertEquals(totalFromTransactions, state.formState.currentAmount, 0.0) // currentAmount updated from transactions
            cancelAndConsumeRemainingEvents()
        }
        viewModel.currentProjectTransactions.test {
            assertEquals(mockTransactions, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        coVerify { savingsRepository.getProjectById(projectId) }
        coVerify { savingsTransactionRepository.getTotalAmountForProject(projectId) }
        coVerify { savingsTransactionRepository.getTransactionsForProject(projectId) } // Verifies loadTransactionsForProject was indirectly called
    }

    @Test
    fun `addAmount creates DEPOSIT transaction, updates project currentAmount, and checks notification`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToAdd = 50.0
        val description = "Weekly deposit"
        val initialProject = SavingsProject(id = projectId, title = "Saving for Goal", targetAmount = 200.0, currentAmount = 100.0, isGoalAchievedNotified = false)
        val expectedNewTotal = 150.0 // 100 (initial) + 50 (added)

        coEvery { savingsRepository.getProjectById(projectId) } returns flowOf(initialProject)
        coEvery { savingsTransactionRepository.addTransaction(any()) } returns 1L // Mock transaction ID
        coEvery { savingsTransactionRepository.getTotalAmountForProject(projectId) } returns expectedNewTotal
        coEvery { savingsRepository.updateProject(any()) } just Runs 
        // No notification expected yet as 150 < 200

        viewModel.addAmount(projectId, amountToAdd, description)
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionSlot = slot<SavingsTransaction>()
        coVerify { savingsTransactionRepository.addTransaction(capture(transactionSlot)) }
        assertEquals(projectId, transactionSlot.captured.projectId)
        assertEquals(amountToAdd, transactionSlot.captured.amount, 0.0)
        assertEquals(SavingsTransaction.TYPE_DEPOSIT, transactionSlot.captured.type)
        assertEquals(description, transactionSlot.captured.description)

        coVerify { savingsTransactionRepository.getTotalAmountForProject(projectId) }
        
        val projectSlot = slot<SavingsProject>()
        coVerify { savingsRepository.updateProject(capture(projectSlot)) }
        assertEquals(expectedNewTotal, projectSlot.captured.currentAmount, 0.0)
        
        verify(exactly = 0) { notificationManager.showSavingsGoalAchieved(any(), any(), any()) }
        coVerify(exactly = 0) { savingsRepository.markGoalAchievedNotified(any()) }
    }
    
    @Test
    fun `addAmount_goalAchieved_sendsNotificationAndMarksNotified_afterTransaction`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToAdd = 100.0
        val description = "Final deposit"
        val initialProject = SavingsProject(id = projectId, title = "Saving for Goal", targetAmount = 150.0, currentAmount = 50.0, isGoalAchievedNotified = false)
        val expectedNewTotal = 150.0 // 50 (initial) + 100 (added)

        coEvery { savingsRepository.getProjectById(projectId) } returns flowOf(initialProject) // Before amount update
        coEvery { savingsTransactionRepository.addTransaction(any()) } returns 1L
        coEvery { savingsTransactionRepository.getTotalAmountForProject(projectId) } returns expectedNewTotal 
        
        // Mock the project state *after* currentAmount is updated for notification check
        val updatedProjectForNotificationCheck = initialProject.copy(currentAmount = expectedNewTotal)
        // We need to make getProjectById return this specific state when it's called *inside* addAmount after total is calculated
        // This is a bit tricky with sequential calls. A simpler way is to ensure updateProject is called with correct currentAmount.
        // The notification logic relies on the project state *after* the update.
        
        coEvery { savingsRepository.updateProject(match { it.currentAmount == expectedNewTotal }) } just Runs
        coEvery { savingsRepository.markGoalAchievedNotified(projectId) } just Runs
        every { notificationManager.showSavingsGoalAchieved(projectId.toInt(), initialProject.title, initialProject.targetAmount) } just Runs


        viewModel.addAmount(projectId, amountToAdd, description)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsTransactionRepository.addTransaction(any()) }
        coVerify { savingsRepository.updateProject(match { it.currentAmount == expectedNewTotal }) }
        coVerify { notificationManager.showSavingsGoalAchieved(projectId.toInt(), initialProject.title, initialProject.targetAmount) }
        coVerify { savingsRepository.markGoalAchievedNotified(projectId) }
    }


    @Test
    fun `subtractAmount creates WITHDRAWAL transaction and updates project currentAmount`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToSubtract = 30.0
        val description = "Emergency withdrawal"
        val initialProject = SavingsProject(id = projectId, title = "My Savings", targetAmount = 200.0, currentAmount = 100.0)
        val expectedNewTotal = 70.0 // 100 (initial) - 30 (subtracted)

        coEvery { savingsRepository.getProjectById(projectId) } returns flowOf(initialProject)
        coEvery { savingsTransactionRepository.addTransaction(any()) } returns 2L // Mock transaction ID
        coEvery { savingsTransactionRepository.getTotalAmountForProject(projectId) } returns expectedNewTotal
        coEvery { savingsRepository.updateProject(any()) } just Runs

        viewModel.subtractAmount(projectId, amountToSubtract, description)
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionSlot = slot<SavingsTransaction>()
        coVerify { savingsTransactionRepository.addTransaction(capture(transactionSlot)) }
        assertEquals(projectId, transactionSlot.captured.projectId)
        assertEquals(amountToSubtract, transactionSlot.captured.amount, 0.0)
        assertEquals(SavingsTransaction.TYPE_WITHDRAWAL, transactionSlot.captured.type)
        assertEquals(description, transactionSlot.captured.description)

        coVerify { savingsTransactionRepository.getTotalAmountForProject(projectId) }

        val projectSlot = slot<SavingsProject>()
        coVerify { savingsRepository.updateProject(capture(projectSlot)) }
        assertEquals(expectedNewTotal, projectSlot.captured.currentAmount, 0.0)
    }

    @Test
    fun `loadTransactionsForProject updates currentProjectTransactions StateFlow`() = runTest(testDispatcher) {
        val projectId = 1L
        val mockTransactions = listOf(
            SavingsTransaction(id = 1, projectId = projectId, amount = 100.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "Deposit 1"),
            SavingsTransaction(id = 2, projectId = projectId, amount = 20.0, type = SavingsTransaction.TYPE_WITHDRAWAL, transactionDate = Date(), description = "Withdrawal 1")
        )
        coEvery { savingsTransactionRepository.getTransactionsForProject(projectId) } returns flowOf(mockTransactions)

        viewModel.loadTransactionsForProject(projectId) // Directly call to test its specific effect
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.currentProjectTransactions.test {
            assertEquals(mockTransactions, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
    
    @Test
    fun `loadTransactionsForProject_repositoryError_emitsErrorEvent`() = runTest(testDispatcher) {
        val projectId = 1L
        val errorMessage = "Failed to load transactions"
        coEvery { savingsTransactionRepository.getTransactionsForProject(projectId) } returns flow { throw RuntimeException(errorMessage) }

        viewModel.loadTransactionsForProject(projectId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.error.test {
            val event = awaitItem()
            assertTrue(event.contains(errorMessage))
            cancelAndConsumeRemainingEvents()
        }
    }
    
    @Test
    fun `deleteProject calls repository`() = runTest(testDispatcher) {
        val projectToDelete = SavingsProject(id = 1L, title = "Delete Me", targetAmount = 100.0, currentAmount = 0.0, startDate = Date()) // Added missing fields
        coEvery { savingsRepository.deleteProject(projectToDelete) } just Runs
        // If deleting project also deletes transactions, you might want to mock/verify that too:
        // coEvery { savingsTransactionRepository.deleteTransactionsForProject(projectToDelete.id) } just Runs 

        viewModel.deleteProject(projectToDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsRepository.deleteProject(projectToDelete) }
        // coVerify { savingsTransactionRepository.deleteTransactionsForProject(projectToDelete.id) }
    }

    @Test
    fun `setProjectActive calls repository`() = runTest(testDispatcher) {
        val projectId = 1L
        val isActive = false
        coEvery { savingsRepository.setProjectActive(projectId, isActive) } just Runs

        viewModel.setProjectActive(projectId, isActive)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsRepository.setProjectActive(projectId, isActive) }
    }

    @Test
    fun `resetForm resets uiState`() = runTest(testDispatcher) {
        viewModel.updateFormState { it.copy(title = "Temporary", isSuccess = true) }
        assertNotEquals(SavingsFormState(), viewModel.uiState.value.projectFormState)
        assertTrue(viewModel.uiState.value.isSuccess)

        viewModel.resetForm()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertEquals(SavingsFormState(), finalState.projectFormState)
        assertFalse(finalState.isSuccess)
        assertTrue(finalState.validationErrors.isEmpty())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks() // Ensure mocks are cleared, already in @After
    }
}
