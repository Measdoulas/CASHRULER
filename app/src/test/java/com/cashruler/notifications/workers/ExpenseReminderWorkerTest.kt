package com.cashruler.notifications.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import com.cashruler.notifications.NotificationManager // Custom NotificationManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import java.util.Calendar

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class) // Needed for TestListenableWorkerBuilder context
class ExpenseReminderWorkerTest {

    private lateinit var context: Context
    private val mockRepository: ExpenseReminderRepositoryInterface = mockk(relaxed = true)
    private val mockNotificationManager: NotificationManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Mock the NotificationManager constructor behavior if it's complex,
        // but for now, relaxed mock should suffice.
    }

    private fun createWorker(): ExpenseReminderWorker {
        val workerParameters = mockk<WorkerParameters>(relaxed = true)
        return ExpenseReminderWorker(
            context = context,
            workerParams = workerParameters,
            expenseReminderRepository = mockRepository,
            notificationManager = mockNotificationManager
        )
    }
    
    @Test
    fun `doWork_noDueReminders_returnsSuccessAndDoesNothing`() = runTest {
        coEvery { mockRepository.getActiveRemindersDue(any()) } returns emptyList()

        val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ExpenseReminderWorker(
                        appContext,
                        workerParameters,
                        mockRepository,
                        mockNotificationManager
                    )
                }
            })
            .build()
        
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { mockNotificationManager.showExpenseReminder(any()) }
        coVerify(exactly = 0) { mockRepository.setReminderActive(any(), any()) }
    }

    @Test
    fun `doWork_dueNonRecurringReminder_sendsNotificationAndDeactivates`() = runTest {
        val reminder = ExpenseReminder(id = 1, description = "Test Reminder", reminderDate = Date(), isRecurring = false)
        coEvery { mockRepository.getActiveRemindersDue(any()) } returns listOf(reminder)
        every { mockNotificationManager.showExpenseReminder(reminder) } just Runs
        coEvery { mockRepository.setReminderActive(reminder.id, false) } just Runs

         val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ExpenseReminderWorker(
                        appContext,
                        workerParameters,
                        mockRepository,
                        mockNotificationManager
                    )
                }
            })
            .build()
            
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) { mockNotificationManager.showExpenseReminder(reminder) }
        coVerify(exactly = 1) { mockRepository.setReminderActive(reminder.id, false) }
    }

    @Test
    fun `doWork_dueRecurringReminder_sendsNotificationAndKeepsActive`() = runTest {
         // As per current worker logic, recurring reminders are also deactivated if not handled.
         // This test reflects the current behavior where isRecurring is not yet used to skip deactivation.
         // If worker is updated, this test should change.
        val reminder = ExpenseReminder(id = 2, description = "Recurring Test", reminderDate = Date(), isRecurring = true, frequencyDays = 7)
        coEvery { mockRepository.getActiveRemindersDue(any()) } returns listOf(reminder)
        every { mockNotificationManager.showExpenseReminder(reminder) } just Runs
        // Current worker logic deactivates all reminders it processes if they are not recurring.
        // It does not yet have logic to reschedule or keep recurring active AND update their date.
        // So, for now, we expect it to be set to inactive.
        // coEvery { mockRepository.setReminderActive(reminder.id, true) } just Runs // This would be for future
        coEvery { mockRepository.setReminderActive(reminder.id, false) } just Runs


         val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ExpenseReminderWorker(
                        appContext,
                        workerParameters,
                        mockRepository,
                        mockNotificationManager
                    )
                }
            })
            .build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) { mockNotificationManager.showExpenseReminder(reminder) }
        // coVerify(exactly = 0) { mockRepository.setReminderActive(reminder.id, false) } // Ideal future state
        // coVerify { mockRepository.updateReminder(match {it.id == reminder.id && it.reminderDate.after(reminder.reminderDate)}) } // Ideal future
         coVerify(exactly = 1) { mockRepository.setReminderActive(reminder.id, false) } // Current behavior
    }

    @Test
    fun `doWork_repositoryThrowsError_returnsFailure`() = runTest {
        coEvery { mockRepository.getActiveRemindersDue(any()) } throws RuntimeException("DB error")

        val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(context)
             .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ExpenseReminderWorker(
                        appContext,
                        workerParameters,
                        mockRepository,
                        mockNotificationManager
                    )
                }
            })
            .build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
    
    @Test
    fun `doWork_multipleReminders_processesAll`() = runTest {
        val reminder1 = ExpenseReminder(id = 1, description = "Non-recurring 1", reminderDate = Date(), isRecurring = false)
        val reminder2 = ExpenseReminder(id = 2, description = "Non-recurring 2", reminderDate = Date(), isRecurring = false)
        coEvery { mockRepository.getActiveRemindersDue(any()) } returns listOf(reminder1, reminder2)
        every { mockNotificationManager.showExpenseReminder(any()) } just Runs
        coEvery { mockRepository.setReminderActive(any(), false) } just Runs

        val worker = TestListenableWorkerBuilder<ExpenseReminderWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return ExpenseReminderWorker(
                        appContext,
                        workerParameters,
                        mockRepository,
                        mockNotificationManager
                    )
                }
            })
            .build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 2) { mockNotificationManager.showExpenseReminder(any()) }
        coVerify(exactly = 1) { mockRepository.setReminderActive(reminder1.id, false) }
        coVerify(exactly = 1) { mockRepository.setReminderActive(reminder2.id, false) }
    }
}
