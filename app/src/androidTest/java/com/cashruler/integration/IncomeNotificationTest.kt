package com.cashruler.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.cashruler.data.models.Income
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.notifications.NotificationManager
import com.cashruler.notifications.workers.IncomeReminderWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IncomeNotificationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var incomeRepository: IncomeRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Configure le WorkManager pour les tests
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun testIncomeReminderWorkflow() = runBlocking {
        // Crée un revenu récurrent qui arrive bientôt
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.time

        val testIncome = Income(
            id = 1L,
            amount = 450000.0,
            description = "Salaire test",
            type = "Salaire",
            date = today.time,
            isRecurring = true,
            recurringFrequency = 30,
            nextOccurrence = tomorrow
        )

        // Configure le repository pour retourner notre revenu test
        coEvery { incomeRepository.getUpcomingRecurringIncomes() } returns flowOf(listOf(testIncome))
        coEvery { incomeRepository.calculateNextOccurrence(any()) } returns Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 31)
        }.time

        // Crée et exécute le worker
        val worker = TestListenableWorkerBuilder<IncomeReminderWorker>(context)
            .setWorkerFactory(createTestWorkerFactory())
            .build()

        // Exécute le worker
        val result = worker.doWork()

        // Vérifie que le worker a réussi
        assertEquals(ListenableWorker.Result.SUCCESS, result)

        // Vérifie que la notification a été envoyée
        verify { 
            notificationManager.showIncomeReminder(
                testIncome.id.toInt(),
                testIncome.description,
                match { it.contains("demain") },
                testIncome.amount
            )
        }
    }

    @Test
    fun testWorkerScheduling() {
        // Programme le worker
        IncomeReminderWorker.schedule(context)

        // Vérifie que le worker est programmé
        val workInfos = workManager
            .getWorkInfosForUniqueWork(IncomeReminderWorker.WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, workInfos[0].state)
    }

    private fun createTestWorkerFactory() = object : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters
        ) = IncomeReminderWorker(
            appContext,
            workerParameters,
            incomeRepository,
            notificationManager
        )
    }
}
