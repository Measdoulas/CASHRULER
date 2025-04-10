package com.cashruler.notifications.workers

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.cashruler.data.models.SpendingLimit
import com.cashruler.data.models.SpendingLimitFrequency
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.notifications.NotificationService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class LimitCheckWorkerTest {

    private lateinit var context: Context
    private lateinit var spendingLimitRepository: SpendingLimitRepository
    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        spendingLimitRepository = mockk()
        notificationService = mockk(relaxed = true)
    }

    @Test
    fun `doWork returns success and shows notification when limit is exceeded`() = runTest {
        // Given
        val limitId = 1L
        val limit = createSpendingLimit(limitId, currentAmount = 150.0, limitAmount = 100.0)
        
        coEvery { spendingLimitRepository.getLimitById(limitId) } returns limit
        coEvery { spendingLimitRepository.getLimitStatus(limit) } returns LimitStatus.EXCEEDED
        coEvery { spendingLimitRepository.isLimitActive(limit) } returns true

        val worker = TestListenableWorkerBuilder<LimitCheckWorker>(
            context = context,
            inputData = workDataOf("limitId" to limitId)
        ).setWorkerFactory(
            TestWorkerFactory(spendingLimitRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerify { 
            notificationService.showLimitAlert(limit)
            notificationService.scheduleLimitCheck(limitId)
        }
    }

    @Test
    fun `doWork returns success and does not show notification when limit is not exceeded`() = runTest {
        // Given
        val limitId = 1L
        val limit = createSpendingLimit(limitId, currentAmount = 50.0, limitAmount = 100.0)
        
        coEvery { spendingLimitRepository.getLimitById(limitId) } returns limit
        coEvery { spendingLimitRepository.getLimitStatus(limit) } returns LimitStatus.OK
        coEvery { spendingLimitRepository.isLimitActive(limit) } returns true

        val worker = TestListenableWorkerBuilder<LimitCheckWorker>(
            context = context,
            inputData = workDataOf("limitId" to limitId)
        ).setWorkerFactory(
            TestWorkerFactory(spendingLimitRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { notificationService.showLimitAlert(any()) }
        coVerify { notificationService.scheduleLimitCheck(limitId) }
    }

    @Test
    fun `doWork returns failure when limit is not found`() = runTest {
        // Given
        val limitId = 1L
        coEvery { spendingLimitRepository.getLimitById(limitId) } returns null

        val worker = TestListenableWorkerBuilder<LimitCheckWorker>(
            context = context,
            inputData = workDataOf("limitId" to limitId)
        ).setWorkerFactory(
            TestWorkerFactory(spendingLimitRepository, notificationService)
        ).build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify(exactly = 0) { 
            notificationService.showLimitAlert(any())
            notificationService.scheduleLimitCheck(any())
        }
    }

    private fun createSpendingLimit(
        id: Long,
        currentAmount: Double,
        limitAmount: Double
    ) = SpendingLimit(
        id = id,
        category = "Test",
        amount = limitAmount,
        currentAmount = currentAmount,
        startDate = Date(),
        frequency = SpendingLimitFrequency.MONTHLY
    )

    private class TestWorkerFactory(
        private val repository: SpendingLimitRepository,
        private val notificationService: NotificationService
    ) : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters
        ): androidx.work.ListenableWorker? {
            return when (workerClassName) {
                LimitCheckWorker::class.java.name ->
                    LimitCheckWorker(appContext, workerParameters, repository, notificationService)
                else -> null
            }
        }
    }

    enum class LimitStatus {
        OK,
        WARNING,
        EXCEEDED;

        fun requiresNotification() = this != OK
    }
}
