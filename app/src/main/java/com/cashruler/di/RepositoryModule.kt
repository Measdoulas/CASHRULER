package com.cashruler.di

import com.cashruler.data.repositories.ExpenseReminderRepository
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseReminderRepository(
        expenseReminderRepository: ExpenseReminderRepository
    ): ExpenseReminderRepositoryInterface

    @Binds
    @Singleton
    abstract fun bindSavingsTransactionRepository(
        savingsTransactionRepository: com.cashruler.data.repositories.SavingsTransactionRepository
    ): com.cashruler.data.repositories.SavingsTransactionRepositoryInterface
}
