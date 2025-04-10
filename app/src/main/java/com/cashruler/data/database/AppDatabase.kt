package com.cashruler.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cashruler.data.dao.*
import com.cashruler.data.models.*
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de données principale de l'application
 */
@Database(
    entities = [
        Expense::class,
        Income::class,
        SavingsProject::class,
        SpendingLimit::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsDao(): SavingsDao
    abstract fun spendingLimitDao(): SpendingLimitDao

    companion object {
        private const val DATABASE_NAME = "cashruler.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialisation des données par défaut
                        CoroutineScope(Dispatchers.IO).launch {
                            initializeDefaultData(INSTANCE!!)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun initializeDefaultData(db: AppDatabase) {
            // Catégories de dépenses par défaut
            Expense.DEFAULT_CATEGORIES.forEach { category ->
                db.spendingLimitDao().insert(
                    SpendingLimit(
                        category = category,
                        amount = 0.0,
                        periodInDays = SpendingLimit.PERIOD_MONTHLY
                    )
                )
            }
        }
    }
}

/**
 * Convertisseurs pour les types complexes
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDoubleMap(value: Map<String, Double>?): String? {
        if (value == null) return null
        return value.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toDoubleMap(value: String?): Map<String, Double>? {
        if (value == null) return null
        return value.split(";").associate {
            val (key, value) = it.split(":")
            key to value.toDouble()
        }
    }
}
