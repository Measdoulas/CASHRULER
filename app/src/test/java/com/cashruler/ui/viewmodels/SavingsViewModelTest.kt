package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.repositories.SavingsRepository
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
class SavingsViewModelTest {

    private lateinit var viewModel: SavingsViewModel
    private val savingsRepository: SavingsRepository = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher() 

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mock flows collected in ViewModel's init or exposed as StateFlows
        coEvery { savingsRepository.getActiveProjects() } returns flowOf(emptyList())
        coEvery { savingsRepository.getCompletedProjects() } returns flowOf(emptyList())
        coEvery { savingsRepository.getTotalSavedAmount() } returns flowOf(0.0)
        // Mock for getGlobalStatistics used by totalTargetAmount (if applicable)
        // Assuming totalTargetAmount is derived from activeProjects for now.
        // If SavingsRepository.GlobalStatistics is used for totalTarget, mock it:
        // coEvery { savingsRepository.getGlobalStatistics() } returns flowOf(SavingsRepository.GlobalStatistics(totalTarget = 0.0))


        viewModel = SavingsViewModel(savingsRepository)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure init collections are processed
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
    fun `loadProject updates projectFormState`() = runTest(testDispatcher) {
        val projectId = 1L
        val mockDate = Date()
        val mockProject = SavingsProject(
            id = projectId, title = "Test Project", description = "Desc",
            targetAmount = 1000.0, currentAmount = 100.0, startDate = mockDate,
            targetDate = null, periodicAmount = null, savingFrequency = null,
            isActive = true, icon = null, notes = "Notes", priority = 1
        )
        coEvery { savingsRepository.getProjectById(projectId) } returns flowOf(mockProject)

        viewModel.loadProject(projectId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val expectedFormState = SavingsFormState(
            title = "Test Project", description = "Desc", targetAmount = 1000.0,
            currentAmount = 100.0, startDate = mockDate, targetDate = null,
            periodicAmount = null, savingFrequency = null, isActive = true,
            icon = null, notes = "Notes", priority = 1
        )
        assertEquals(expectedFormState, viewModel.uiState.value.projectFormState)
    }

    @Test
    fun `addAmount calls repository`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToAdd = 50.0
        coEvery { savingsRepository.addToProjectAmount(projectId, amountToAdd) } just Runs

        viewModel.addAmount(projectId, amountToAdd)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsRepository.addToProjectAmount(projectId, amountToAdd) }
    }

    @Test
    fun `addAmount handles repository exception`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToAdd = 50.0
        val errorMsg = "Failed to add amount"
        coEvery { savingsRepository.addToProjectAmount(projectId, amountToAdd) } throws RuntimeException(errorMsg)

        viewModel.error.test {
            viewModel.addAmount(projectId, amountToAdd)
            testDispatcher.scheduler.advanceUntilIdle()
            val emittedError = awaitItem()
            assertTrue(emittedError.contains(errorMsg))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `subtractAmount calls repository`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToSubtract = 30.0
        coEvery { savingsRepository.subtractFromProjectAmount(projectId, amountToSubtract) } just Runs

        viewModel.subtractAmount(projectId, amountToSubtract)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsRepository.subtractFromProjectAmount(projectId, amountToSubtract) }
    }
    
    @Test
    fun `subtractAmount handles repository exception`() = runTest(testDispatcher) {
        val projectId = 1L
        val amountToSubtract = 30.0
        val errorMsg = "Failed to subtract amount"
        coEvery { savingsRepository.subtractFromProjectAmount(projectId, amountToSubtract) } throws RuntimeException(errorMsg)

        viewModel.error.test {
            viewModel.subtractAmount(projectId, amountToSubtract)
            testDispatcher.scheduler.advanceUntilIdle()
            val emittedError = awaitItem()
            assertTrue(emittedError.contains(errorMsg))
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `deleteProject calls repository`() = runTest(testDispatcher) {
        val projectToDelete = SavingsProject(id = 1L, title = "Delete Me", targetAmount = 100.0)
        coEvery { savingsRepository.deleteProject(projectToDelete) } just Runs

        viewModel.deleteProject(projectToDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { savingsRepository.deleteProject(projectToDelete) }
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
    }
}
