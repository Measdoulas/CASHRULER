package com.cashruler.notifications

import android.content.Context
import com.cashruler.data.models.SavingsFrequency
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SpendingLimit
import com.cashruler.data.repositories.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.*

class NotificationManagerTest {
    
    private lateinit var context: Context
    private lateinit var notificationService: NotificationService
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        settingsRepository = mockk()
        notificationManager = NotificationManager(context, notificationService, settingsRepository)
    }

    @Test
    fun `setupLimitAlerts shows notification when limit is exceeded and notifications are enabled`() = runTest {
        // Given
        val limit = createSpendingLimit(1, currentAmount = 150.0, limitAmount = 100.0)
        coEvery { settingsRepository.getLimitsNotificationsEnabled() } returns true
        coEvery { settingsRepository.getLimitsWarningThreshold() } returns 80f
        coEvery { settingsRepository.getAutoResetLimits() } returns true

        // When
        notificationManager.setupLimitAlerts(limit)

        // Then
        coVerify { 
            notificationService.showLimitAlert(limit)
            notificationService.scheduleLimitCheck(limit.id)
        }
    }

    @Test
    fun `setupLimitAlerts does not show notification when notifications are disabled`() = runTest {
        // Given
        val limit = createSpendingLimit(1, currentAmount = 150.0, limitAmount = 100.0)
        coEvery { settingsRepository.getLimitsNotificationsEnabled() } returns false

        // When
        notificationManager.setupLimitAlerts(limit)

        // Then
        coVerify(exactly = 0) { 
            notificationService.showLimitAlert(any())
            notificationService.scheduleLimitCheck(any())
        }
    }

    @Test
    fun `setupSavingsReminders schedules reminder for active project when notifications are enabled`() = runTest {
        // Given
        val project = createSavingsProject(1, isActive = true)
        coEvery { settingsRepository.getSavingsNotificationsEnabled() } returns true

        // When
        notificationManager.setupSavingsReminders(project)

        // Then
        coVerify { 
            notificationService.scheduleSavingsReminder(project.id, 30) // Monthly frequency
        }
    }

    @Test
    fun `setupSavingsReminders does not schedule reminder for inactive project`() = runTest {
        // Given
        val project = createSavingsProject(1, isActive = false)
        coEvery { settingsRepository.getSavingsNotificationsEnabled() } returns true

        // When
        notificationManager.setupSavingsReminders(project)

        // Then
        coVerify(exactly = 0) { 
            notificationService.scheduleSavingsReminder(any(), any())
        }
    }

    @Test
    fun `updateLimitNotifications updates notifications when settings change`() = runTest {
        // Given
        val limit = createSpendingLimit(1)
        coEvery { settingsRepository.getLimitsNotificationsEnabled() } returns true
        coEvery { settingsRepository.getLimitsWarningThreshold() } returns 80f
        coEvery { settingsRepository.getAutoResetLimits() } returns true

        // When
        notificationManager.updateLimitNotifications(limit)

        // Then
        coVerify { 
            settingsRepository.getLimitsNotificationsEnabled()
            notificationService.scheduleLimitCheck(limit.id)
        }
    }

    @Test
    fun `cancel notifications removes scheduled checks`() = runTest {
        // Given
        val limitId = 1L
        val projectId = 2L

        // When
        notificationManager.cancelLimitNotifications(limitId)
        notificationManager.cancelSavingsNotifications(projectId)

        // Then
        verify { 
            notificationService.cancelLimitCheck(limitId)
            notificationService.cancelSavingsReminder(projectId)
        }
    }

    private fun createSpendingLimit(
        id: Long,
        currentAmount: Double = 0.0,
        limitAmount: Double = 100.0
    ) = SpendingLimit(
        id = id,
        category = "Test",
        amount = limitAmount,
        currentAmount = currentAmount,
        startDate = Date(),
        frequency = com.cashruler.data.models.SpendingLimitFrequency.MONTHLY
    )

    private fun createSavingsProject(
        id: Long,
        isActive: Boolean = true
    ) = SavingsProject(
        id = id,
        title = "Test Project",
        targetAmount = 1000.0,
        currentAmount = 0.0,
        startDate = Date(),
        deadline = if (isActive) Date(System.currentTimeMillis() + 86400000) else Date(0),
        frequency = SavingsFrequency.MONTHLY
    )
}
