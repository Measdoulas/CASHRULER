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
        SpendingLimit::class,
        CategoryEntity::class, // Ajout de CategoryEntity
        IncomeTypeEntity::class // Ajout de IncomeTypeEntity
    ],
    version = 1, // La version devrait être incrémentée si on change le schéma pour la production
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsDao(): SavingsDao
    abstract fun spendingLimitDao(): SpendingLimitDao
    abstract fun categoryDao(): CategoryDao // Ajout du DAO pour CategoryEntity
    abstract fun incomeTypeDao(): IncomeTypeDao // Ajout du DAO pour IncomeTypeEntity

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
            // Migration des catégories par défaut vers la nouvelle table CategoryEntity
            Expense.DEFAULT_CATEGORIES.forEach { categoryName ->
                // Insère dans la table CategoryEntity
                db.categoryDao().insert(CategoryEntity(name = categoryName))

                // Conserve la logique existante pour SpendingLimit si nécessaire,
                // ou la modifier si SpendingLimit doit référencer CategoryEntity.id
                // Pour l'instant, on suppose que SpendingLimit.category reste une String.
                val existingLimit = db.spendingLimitDao().getLimitByCategoryAndPeriod(categoryName, SpendingLimit.PERIOD_MONTHLY).first()
                if (existingLimit == null) {
                    db.spendingLimitDao().insert(
                        SpendingLimit(
                            category = categoryName, // Reste une string ici
                            amount = 0.0, // Ou un montant par défaut pertinent
                            periodInDays = SpendingLimit.PERIOD_MONTHLY
                        )
                    )
                }
            }

            // Types de revenus par défaut
            Income.DEFAULT_TYPES.forEach { typeName ->
                db.incomeTypeDao().insert(IncomeTypeEntity(name = typeName))
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
