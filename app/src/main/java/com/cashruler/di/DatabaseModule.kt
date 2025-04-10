package com.cashruler.di

import android.content.Context
import com.cashruler.data.dao.*
import com.cashruler.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt pour l'injection des dépendances liées à la base de données
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Fournit l'instance de la base de données
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    /**
     * Fournit le DAO pour les dépenses
     */
    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    /**
     * Fournit le DAO pour les revenus
     */
    @Provides
    @Singleton
    fun provideIncomeDao(database: AppDatabase): IncomeDao {
        return database.incomeDao()
    }

    /**
     * Fournit le DAO pour les projets d'épargne
     */
    @Provides
    @Singleton
    fun provideSavingsDao(database: AppDatabase): SavingsDao {
        return database.savingsDao()
    }

    /**
     * Fournit le DAO pour les limites de dépenses
     */
    @Provides
    @Singleton
    fun provideSpendingLimitDao(database: AppDatabase): SpendingLimitDao {
        return database.spendingLimitDao()
    }

    /**
     * Fournit le scope de l'application pour les coroutines
     */
    @Provides
    @Singleton
    fun provideCoroutineScope(): kotlinx.coroutines.CoroutineScope {
        return kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    }

    /**
     * Fournit le dispatcher IO pour les opérations de base de données
     */
    @Provides
    @Singleton
    fun provideIODispatcher(): kotlinx.coroutines.CoroutineDispatcher {
        return kotlinx.coroutines.Dispatchers.IO
    }
}

/**
 * Qualifieur pour le dispatcher IO
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IODispatcher

/**
 * Qualifieur pour le scope application
 */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
