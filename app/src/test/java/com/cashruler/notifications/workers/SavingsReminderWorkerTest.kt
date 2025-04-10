package com.cashruler.notifications.workers

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.cashruler.data.models.SavingsFrequency
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.repositories.SavingsRepository
import com.cashruler.notifications.NotificationService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class SavingsReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var savingsRepository: SavingsRepository
    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        savingsRepository = mockk()
        notificationService = mockk(relaxed = true)
    }

    @Test
    fun `doWork returns success and shows notification for active project needing contribution`() = runTest {
        // Given
        val projectId = 1L
        val project = createSavingsProject(
            id = projectId,
            currentAmount = 500.0,
            targetAmount = 1000.0,
            isActive = true,
            frequency = SavingsFrequency.MONTHLY
        )
        
        coEvery { savingsRepository.getProjectById(projectId) } returns project

        val worker = TestListenableWorkerBuilder<SavingsReminderWorker>(
            context = context,
            inputData = workDataOf("projectId" to projectId)
        ).setWorkerFactory(
            TestWorkerFactory(savingsRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerify { 
            notificationService.showSavingsReminder(project)
            notificationService.scheduleSavingsReminder(projectId, 30) // Monthly frequency
        }
    }

    @Test
    fun `doWork returns success and does not show notification for completed project`() = runTest {
        // Given
        val projectId = 1L
        val project = createSavingsProject(
            id = projectId,
            currentAmount = 1000.0,
            targetAmount = 1000.0,
            isActive = true
        )
        
        coEvery { savingsRepository.getProjectById(projectId) } returns project

        val worker = TestListenableWorkerBuilder<SavingsReminderWorker>(
            context = context,
            inputData = workDataOf("projectId" to projectId)
        ).setWorkerFactory(
            TestWorkerFactory(savingsRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { 
            notificationService.showSavingsReminder(any())
            notificationService.scheduleSavingsReminder(any(), any())
        }
    }

    @Test
    fun `doWork returns success and does not show notification for inactive project`() = runTest {
        // Given
        val projectId = 1L
        val project = createSavingsProject(
            id = projectId,
            currentAmount = 500.0,
            targetAmount = 1000.0,
            isActive = false
        )
        
        coEvery { savingsRepository.getProjectById(projectId) } returns project

        val worker = TestListenableWorkerBuilder<SavingsReminderWorker>(
            context = context,
            inputData = workDataOf("projectId" to projectId)
        ).setWorkerFactory(
            TestWorkerFactory(savingsRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { 
            notificationService.showSavingsReminder(any())
            notificationService.scheduleSavingsReminder(any(), any())
        }
    }

    @Test
    fun `doWork returns failure when project is not found`() = runTest {
        // Given
        val projectId = 1L
        coEvery { savingsRepository.getProjectById(projectId) } returns null

        val worker = TestListenableWorkerBuilder<SavingsReminderWorker>(
            context = context,
            inputData = workDataOf("projectId" to projectId)
        ).setWorkerFactory(
            TestWorkerFactory(savingsRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify(exactly = 0) { 
            notificationService.showSavingsReminder(any())
            notificationService.scheduleSavingsReminder(any(), any())
        }
    }

    private fun createSavingsProject(
        id: Long,
        currentAmount: Double,
        targetAmount: Double,
        isActive: Boolean,
        frequency: SavingsFrequency = SavingsFrequency.MONTHLY
    ) = SavingsProject(
        id = id,
        title = "Test Project",
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = Date(),
        deadline = if (isActive) Date(System.currentTimeMillis() + 86400000) else Date(0),
        frequency = frequency
    )

    private class TestWorkerFactory(
        private val repository: SavingsRepository,
        private val notificationService: NotificationService
    ) : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters
        ): androidx.work.ListenableWorker? {
            return when (workerClassName) {
                SavingsReminderWorker::class.java.name ->
                    SavingsReminderWorker(appContext, workerParameters, repository, notificationService)
                else -> null
            }
        }
    }
}
