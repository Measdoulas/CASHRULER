package com.cashruler.notifications.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.cashruler.data.repositories.SpendingLimitRepositoryInterface
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DailyLimitResetWorkerTest {

    private lateinit var context: Context
    private val mockSpendingLimitRepository: SpendingLimitRepositoryInterface = mockk()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createWorker(): DailyLimitResetWorker {
        // Create a WorkerFactory that provides the mocked repository
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return if (workerClassName == DailyLimitResetWorker::class.java.name) {
                    DailyLimitResetWorker(appContext, workerParameters, mockSpendingLimitRepository)
                } else {
                    null
                }
            }
        }

        return TestListenableWorkerBuilder<DailyLimitResetWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Test
    fun `doWork_callsResetExpiredPeriods_returnsSuccess`() = runTest {
        coEvery { mockSpendingLimitRepository.resetExpiredPeriods(any(), any()) } just Runs

        val worker = createWorker()
        val result = worker.doWork()

        coVerify { mockSpendingLimitRepository.resetExpiredPeriods(any<Date>(), any<Date>()) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork_repositoryThrowsException_returnsFailure`() = runTest {
        val exceptionMessage = "Repository error"
        coEvery { mockSpendingLimitRepository.resetExpiredPeriods(any(), any()) } throws RuntimeException(exceptionMessage)

        val worker = createWorker()
        val result = worker.doWork()
        
        assertEquals(ListenableWorker.Result.failure(), result)
    }
    
    @Test
    fun `doWork_repositoryThrowsSpecificException_returnsRetry`() = runTest {
        // Example for a specific, potentially retryable exception
        // For now, let's assume any exception leads to failure as per worker's current logic
        // If retry logic were implemented in the worker for specific exceptions:
        // coEvery { mockSpendingLimitRepository.resetExpiredPeriods(any(), any()) } throws java.io.IOException("Network error")
        // val worker = createWorker()
        // val result = worker.doWork()
        // assertEquals(ListenableWorker.Result.retry(), result)
        
        // Current worker behavior: any exception -> failure
        val exceptionMessage = "Simulated error for retry logic (currently failure)"
        coEvery { mockSpendingLimitRepository.resetExpiredPeriods(any(), any()) } throws RuntimeException(exceptionMessage)

        val worker = createWorker()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result) // Current worker returns failure
    }
}
