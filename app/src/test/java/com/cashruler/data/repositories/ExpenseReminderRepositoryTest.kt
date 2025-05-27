package com.cashruler.data.repositories

import com.cashruler.data.dao.ExpenseReminderDao
import com.cashruler.data.models.ExpenseReminder
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class ExpenseReminderRepositoryTest {

    private lateinit var repository: ExpenseReminderRepository
    private val mockDao: ExpenseReminderDao = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // For repository methods that might switch context internally
        repository = ExpenseReminderRepository(mockDao, testDispatcher) // Provide test dispatcher
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `addReminder calls dao insert`() = runTest(testDispatcher) {
        val reminder = ExpenseReminder(description = "Test", reminderDate = Date())
        coEvery { mockDao.insert(reminder) } returns 1L

        val resultId = repository.addReminder(reminder)

        coVerify { mockDao.insert(reminder) }
        assertEquals(1L, resultId)
    }

    @Test
    fun `updateReminder calls dao update and sets updatedAt`() = runTest(testDispatcher) {
        val originalDate = Date(System.currentTimeMillis() - 100000) // An older date
        val reminder = ExpenseReminder(id = 1, description = "Update Test", reminderDate = Date(), updatedAt = originalDate)
        
        // We need to capture the argument to verify updatedAt
        val reminderSlot = slot<ExpenseReminder>()
        coEvery { mockDao.update(capture(reminderSlot)) } just Runs

        repository.updateReminder(reminder)

        coVerify { mockDao.update(reminderSlot.captured) }
        assertTrue("updatedAt should be greater than originalDate", reminderSlot.captured.updatedAt.time > originalDate.time)
        assertEquals(reminder.id, reminderSlot.captured.id)
    }

    @Test
    fun `deleteReminder calls dao delete`() = runTest(testDispatcher) {
        val reminder = ExpenseReminder(id = 1, description = "Delete Test", reminderDate = Date())
        coEvery { mockDao.delete(reminder) } just Runs

        repository.deleteReminder(reminder)

        coVerify { mockDao.delete(reminder) }
    }

    @Test
    fun `getReminderById calls dao getById`() = runTest(testDispatcher) {
        val reminderId = 1L
        val expectedReminder = ExpenseReminder(id = reminderId, description = "Get Test", reminderDate = Date())
        coEvery { mockDao.getById(reminderId) } returns expectedReminder

        val result = repository.getReminderById(reminderId)

        coVerify { mockDao.getById(reminderId) }
        assertEquals(expectedReminder, result)
    }

    @Test
    fun `getActiveReminders returns flow from dao`() = runTest(testDispatcher) {
        val reminders = listOf(ExpenseReminder(id = 1, description = "Active 1", reminderDate = Date()))
        coEvery { mockDao.getActiveReminders() } returns flowOf(reminders)

        val resultFlow = repository.getActiveReminders()
        
        assertEquals(reminders, resultFlow.first())
        coVerify { mockDao.getActiveReminders() } 
    }

    @Test
    fun `getActiveRemindersDue calls dao getActiveRemindersDue with date time`() = runTest(testDispatcher) {
        val testDate = Date()
        val expectedReminders = listOf(ExpenseReminder(id = 1, description = "Due Reminder", reminderDate = testDate))
        coEvery { mockDao.getActiveRemindersDue(testDate.time) } returns expectedReminders

        val result = repository.getActiveRemindersDue(testDate)

        coVerify { mockDao.getActiveRemindersDue(testDate.time) }
        assertEquals(expectedReminders, result)
    }
    
    @Test
    fun `setReminderActive calls dao setReminderActive`() = runTest(testDispatcher) {
        val reminderId = 1L
        val isActive = false
        // DAO's setReminderActive has a default for updatedAt, so we don't need to capture for that here
        // unless we want to verify it's called with a specific timestamp from repo (which it isn't currently)
        coEvery { mockDao.setReminderActive(reminderId, isActive, any()) } just Runs 

        repository.setReminderActive(reminderId, isActive)
        
        // Verify it's called with the correct id and isActive state.
        // The third argument (updatedAt) in DAO is defaulted if not passed by repo,
        // or set by DAO itself. Current repo implementation relies on DAO's default.
        coVerify { mockDao.setReminderActive(reminderId, isActive, any()) }
    }
}
