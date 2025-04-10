package com.cashruler.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AppPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var db: AppDatabase
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        expenseRepository = ExpenseRepository(db.expenseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertExpensePerformance() = runTest {
        val expense = Expense(
            id = 0,
            title = "Test",
            amount = 100.0,
            category = "Test",
            date = Date()
        )

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                expenseRepository.addExpense(expense)
            }
        }
    }

    @Test
    fun queryExpensesPerformance() = runTest {
        // Préparer des données de test
        runWithTimingDisabled {
            repeat(100) {
                expenseRepository.addExpense(
                    Expense(
                        id = 0,
                        title = "Test $it",
                        amount = Random.nextDouble(100.0, 1000.0),
                        category = "Category ${it % 5}",
                        date = Date(System.currentTimeMillis() - it * 86400000L)
                    )
                )
            }
        }

        // Mesurer les performances des requêtes
        benchmarkRule.measureRepeated {
            expenseRepository.getAllExpensesList()
        }
    }

    @Test
    fun calculateMonthlyTotalsPerformance() = runTest {
        // Préparer beaucoup de données
        runWithTimingDisabled {
            repeat(1000) {
                expenseRepository.addExpense(
                    Expense(
                        id = 0,
                        title = "Test $it",
                        amount = Random.nextDouble(100.0, 1000.0),
                        category = "Category ${it % 5}",
                        date = Date(System.currentTimeMillis() - it * 86400000L)
                    )
                )
            }
        }

        // Mesurer le calcul des totaux mensuels
        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                val startDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val endDate = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.time
            }

            // La mesure réelle
            expenseRepository.getExpensesBetweenDates(
                startDate = runWithTimingDisabled { Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time },
                endDate = runWithTimingDisabled { Date() }
            )
        }
    }

    @Test
    fun searchExpensesPerformance() = runTest {
        // Préparer des données de test avec des titres variés
        runWithTimingDisabled {
            val words = listOf("Courses", "Restaurant", "Transport", "Loisirs", "Shopping")
            repeat(500) {
                expenseRepository.addExpense(
                    Expense(
                        id = 0,
                        title = "${words.random()} ${Random.nextInt(100)}",
                        amount = Random.nextDouble(100.0, 1000.0),
                        category = "Category ${it % 5}",
                        date = Date(System.currentTimeMillis() - it * 86400000L)
                    )
                )
            }
        }

        // Mesurer les performances de la recherche
        benchmarkRule.measureRepeated {
            expenseRepository.searchExpenses("Courses")
        }
    }

    @Test
    fun bulkInsertPerformance() = runTest {
        val expenses = List(1000) {
            Expense(
                id = 0,
                title = "Bulk Test $it",
                amount = Random.nextDouble(100.0, 1000.0),
                category = "Category ${it % 5}",
                date = Date(System.currentTimeMillis() - it * 86400000L)
            )
        }

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                expenseRepository.insertExpenses(expenses)
            }
        }
    }
}
