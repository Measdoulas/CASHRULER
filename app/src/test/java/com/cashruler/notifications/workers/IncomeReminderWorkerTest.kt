package com.cashruler.notifications.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.cashruler.data.models.Income
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.notifications.NotificationManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class IncomeReminderWorkerTest {
    private lateinit var context: Context
    private lateinit var workerParameters: WorkerParameters
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var worker: IncomeReminderWorker

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParameters = mockk(relaxed = true)
        incomeRepository = mockk()
        notificationManager = mockk(relaxed = true)

        worker = TestListenableWorkerBuilder<IncomeReminderWorker>(context)
            .setWorkerFactory(TestWorkerFactory(incomeRepository, notificationManager))
            .build() as IncomeReminderWorker
    }

    @Test
    fun `when no upcoming incomes, returns success`() = runBlocking {
        // Given
        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(emptyList())

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notificationManager.showIncomeReminder(any(), any(), any(), any()) }
    }

    @Test
    fun `when income is due today, shows notification and updates next occurrence`() = runBlocking {
        // Given
        val today = Calendar.getInstance().time
        val income = Income(
            id = 1L,
            amount = 450000.0,
            description = "Salaire",
            type = "Salaire",
            date = today,
            isRecurring = true,
            recurringFrequency = 30,
            nextOccurrence = today
        )
        val nextOccurrence = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 30)
        }.time

        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(listOf(income))
        coEvery { incomeRepository.calculateNextOccurrence(any()) } returns nextOccurrence
        coEvery { incomeRepository.updateIncome(any()) } returns Unit

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { 
            notificationManager.showIncomeReminder(
                income.id.toInt(),
                income.description,
                match { it.contains("aujourd'hui") },
                income.amount
            )
        }
        verify {
            incomeRepository.updateIncome(match {
                it.id == income.id && it.nextOccurrence == nextOccurrence
            })
        }
    }

    @Test
    fun `when income is due tomorrow, shows notification without updating`() = runBlocking {
        // Given
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.time
        val income = Income(
            id = 1L,
            amount = 450000.0,
            description = "Salaire",
            type = "Salaire",
            date = tomorrow,
            isRecurring = true,
            recurringFrequency = 30,
            nextOccurrence = tomorrow
        )

        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(listOf(income))

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { 
            notificationManager.showIncomeReminder(
                income.id.toInt(),
                income.description,
                match { it.contains("demain") },
                income.amount
            )
        }
        verify(exactly = 0) { incomeRepository.updateIncome(any()) }
    }

    @Test
    fun `when income is due in 3 days, shows notification without updating`() = runBlocking {
        // Given
        val inThreeDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 3)
        }.time
        val income = Income(
            id = 1L,
            amount = 450000.0,
            description = "Salaire",
            type = "Salaire",
            date = inThreeDays,
            isRecurring = true,
            recurringFrequency = 30,
            nextOccurrence = inThreeDays
        )

        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(listOf(income))

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { 
            notificationManager.showIncomeReminder(
                income.id.toInt(),
                income.description,
                match { it.contains("3 jours") },
                income.amount
            )
        }
        verify(exactly = 0) { incomeRepository.updateIncome(any()) }
    }

    @Test
    fun `when income is due in 4 days, does not show notification`() = runBlocking {
        // Given
        val inFourDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 4)
        }.time
        val income = Income(
            id = 1L,
            amount = 450000.0,
            description = "Salaire",
            type = "Salaire",
            date = inFourDays,
            isRecurring = true,
            recurringFrequency = 30,
            nextOccurrence = inFourDays
        )

        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(listOf(income))

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notificationManager.showIncomeReminder(any(), any(), any(), any()) }
        verify(exactly = 0) { incomeRepository.updateIncome(any()) }
    }

    @Test
    fun `when repository throws exception, returns retry`() = runBlocking {
        // Given
        coEvery { incomeRepository.getUpcomingRecurringIncomes() } throws Exception("Network error")

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        verify(exactly = 0) { notificationManager.showIncomeReminder(any(), any(), any(), any()) }
    }
}

private class TestWorkerFactory(
    private val incomeRepository: IncomeRepository,
    private val notificationManager: NotificationManager
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            IncomeReminderWorker::class.java.name ->
                IncomeReminderWorker(
                    appContext,
                    workerParameters,
                    incomeRepository,
                    notificationManager
                )
            else -> null
        }
    }
}
