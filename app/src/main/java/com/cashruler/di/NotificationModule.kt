package com.cashruler.di

import android.content.Context
import androidx.work.WorkManager
import com.cashruler.notifications.NotificationManager
import com.cashruler.notifications.NotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): NotificationService {
        return NotificationService(context, workManager)
    }

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        notificationService: NotificationService,
        settingsRepository: com.cashruler.data.repositories.SettingsRepository
    ): NotificationManager {
        return NotificationManager(context, notificationService, settingsRepository)
    }

    // Configuration personnalisée de WorkManager
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): androidx.work.Configuration {
        return androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationBindingsModule {

    @dagger.Binds
    abstract fun bindNotificationManager(
        notificationManager: NotificationManager
    ): NotificationManager
}
