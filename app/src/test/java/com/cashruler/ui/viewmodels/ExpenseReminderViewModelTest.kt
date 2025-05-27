package com.cashruler.ui.viewmodels

import app.cash.turbine.test
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class ExpenseReminderViewModelTest {

    private lateinit var viewModel: ExpenseReminderViewModel
    private val mockRepository: ExpenseReminderRepositoryInterface = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ExpenseReminderViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadActiveReminders_updatesStateWithReminders`() = runTest(testDispatcher) {
        val reminders = listOf(
            ExpenseReminder(id = 1, description = "Reminder 1", reminderDate = Date()),
            ExpenseReminder(id = 2, description = "Reminder 2", reminderDate = Date())
        )
        coEvery { mockRepository.getActiveReminders() } returns flowOf(reminders)

        viewModel.loadActiveReminders()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem() // Initial state if any, or first emitted state
            // If loadActiveReminders is called in init, the first item might already have reminders.
            // If not, we might need to skip the initial default state.
            // For this test, assuming loadActiveReminders is explicitly called and emits.
            val finalState = if (state.reminders.isEmpty() && reminders.isNotEmpty()) awaitItem() else state
            assertEquals(reminders, finalState.reminders)
            assertFalse(finalState.isLoading)
        }
    }
    
    @Test
    fun `loadActiveReminders_repositoryError_updatesStateWithError`() = runTest(testDispatcher) {
        val errorMessage = "Failed to load"
        coEvery { mockRepository.getActiveReminders() } returns flow { throw RuntimeException(errorMessage) }

        viewModel.loadActiveReminders()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.errorMessage?.contains(errorMessage) == true)
            assertFalse(state.isLoading)
        }
         viewModel.errorEvents.test {
            assertTrue(awaitItem().contains(errorMessage))
        }
    }


    @Test
    fun `addReminder_validInput_callsRepositoryAndClearsForm`() = runTest(testDispatcher) {
        val formState = ExpenseReminderFormState(description = "Valid Reminder", amount = "100.0", reminderDate = Date())
        viewModel.updateFormState(formState)
        
        coEvery { mockRepository.addReminder(any()) } returns 1L // Assume returns ID

        viewModel.addReminder()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.addReminder(any()) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ExpenseReminderFormState(), state.formState) // Form reset
            assertFalse(state.isLoading)
        }
        viewModel.errorEvents.test {
            assertEquals("Reminder added successfully.", awaitItem())
        }
    }

    @Test
    fun `addReminder_invalidInput_setsErrorMessageAndDoesNotCallRepository`() = runTest(testDispatcher) {
        val formState = ExpenseReminderFormState(description = "", amount = "100.0") // Invalid: empty description
        viewModel.updateFormState(formState)

        viewModel.addReminder()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.addReminder(any()) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.validationErrors.containsKey("description"))
        }
        viewModel.errorEvents.test {
            assertEquals("Please correct the form errors.", awaitItem())
        }
    }

    @Test
    fun `updateReminder_validInput_callsRepository`() = runTest(testDispatcher) {
        val reminderId = 1L
        val formState = ExpenseReminderFormState(id = reminderId, description = "Updated Reminder", amount = "150.0", reminderDate = Date())
        viewModel.updateFormState(formState)
        
        coEvery { mockRepository.updateReminder(any()) } just Runs

        viewModel.updateReminder(reminderId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.updateReminder(match { it.id == reminderId && it.description == "Updated Reminder" }) }
         viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ExpenseReminderFormState(), state.formState) // Form reset
            assertFalse(state.isLoading)
        }
        viewModel.errorEvents.test {
            assertEquals("Reminder updated successfully.", awaitItem())
        }
    }
    
    @Test
    fun `updateReminder_invalidInput_doesNotCallRepository`() = runTest(testDispatcher) {
        val reminderId = 1L
        val formState = ExpenseReminderFormState(id = reminderId, description = "", amount = "150.0") // Invalid
        viewModel.updateFormState(formState)

        viewModel.updateReminder(reminderId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.updateReminder(any()) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.validationErrors.containsKey("description"))
        }
         viewModel.errorEvents.test {
            assertEquals("Please correct the form errors.", awaitItem())
        }
    }

    @Test
    fun `deleteReminder_callsRepository`() = runTest(testDispatcher) {
        val reminder = ExpenseReminder(id = 1, description = "Delete Me", reminderDate = Date())
        coEvery { mockRepository.deleteReminder(reminder) } just Runs

        viewModel.deleteReminder(reminder)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.deleteReminder(reminder) }
         viewModel.errorEvents.test {
            assertEquals("Reminder deleted successfully.", awaitItem())
        }
    }

    @Test
    fun `loadReminderForEditing_populatesFormState`() = runTest(testDispatcher) {
        val reminderId = 1L
        val reminder = ExpenseReminder(id = reminderId, description = "Edit Me", amount = 50.0, reminderDate = Date(), notes = "Test notes")
        coEvery { mockRepository.getReminderById(reminderId) } returns reminder

        viewModel.loadReminderForEditing(reminderId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(reminder.description, state.formState.description)
            assertEquals(reminder.amount.toString(), state.formState.amount)
            assertEquals(reminder.notes, state.formState.notes)
            assertFalse(state.isLoading)
        }
    }
    
    @Test
    fun `loadReminderForEditing_reminderNotFound_setsError`() = runTest(testDispatcher) {
        val reminderId = 1L
        coEvery { mockRepository.getReminderById(reminderId) } returns null

        viewModel.loadReminderForEditing(reminderId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Reminder not found.", state.errorMessage)
            assertFalse(state.isLoading)
        }
        viewModel.errorEvents.test {
            assertEquals("Reminder not found.", awaitItem())
        }
    }

    @Test
    fun `setReminderActive_callsRepository`() = runTest(testDispatcher) {
        val reminderId = 1L
        val isActive = false
        coEvery { mockRepository.setReminderActive(reminderId, isActive) } just Runs

        viewModel.setReminderActive(reminderId, isActive)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.setReminderActive(reminderId, isActive) }
        viewModel.errorEvents.test {
            assertEquals("Reminder status updated.", awaitItem())
        }
    }
    
    @Test
    fun `updateFormState_validatesAndUpdatesState`() = runTest(testDispatcher) {
        val initialFormState = ExpenseReminderFormState(description = "Initial")
        viewModel.updateFormState(initialFormState) // Set initial valid state

        viewModel.uiState.test {
            assertEquals(initialFormState, awaitItem().formState) // Check initial update

            val invalidFormState = initialFormState.copy(description = "") // Make it invalid
            viewModel.updateFormState(invalidFormState)
            
            val updatedState = awaitItem()
            assertEquals(invalidFormState, updatedState.formState)
            assertTrue(updatedState.validationErrors.containsKey("description"))

            val validFormState = invalidFormState.copy(description = "Valid Again")
            viewModel.updateFormState(validFormState)
            val finalState = awaitItem()
            assertEquals(validFormState, finalState.formState)
            assertTrue(finalState.validationErrors.isEmpty())
        }
    }
}
