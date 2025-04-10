package com.cashruler.data.repositories

import android.content.Context
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val savingsRepository: SavingsRepository,
    private val spendingLimitRepository: SpendingLimitRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val backupFolder get() = File(context.filesDir, "backups")

    data class BackupData(
        val expenses: List<Expense>,
        val incomes: List<Income>,
        val savingsProjects: List<SavingsProject>,
        val spendingLimits: List<SpendingLimit>,
        val version: Int = BACKUP_VERSION,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        const val BACKUP_VERSION = 1
    }

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val data = BackupData(
            expenses = expenseRepository.getAllExpensesList(),
            incomes = incomeRepository.getAllIncomesList(),
            savingsProjects = savingsRepository.getAllProjectsList(),
            spendingLimits = spendingLimitRepository.getAllLimitsList()
        )

        val json = gson.toJson(data)
        val backupFile = createBackupFile()
        FileWriter(backupFile).use { it.write(json) }
        backupFile.absolutePath
    }

    suspend fun importData(filePath: String): Unit = withContext(Dispatchers.IO) {
        val json = File(filePath).readText()
        val data = gson.fromJson(json, BackupData::class.java)

        if (data.version > BACKUP_VERSION) {
            throw IllegalStateException("Version de sauvegarde non supportée")
        }

        database.withTransaction {
            // Suppression des données existantes
            deleteAllData()

            // Import des nouvelles données
            expenseRepository.insertExpenses(data.expenses)
            incomeRepository.insertIncomes(data.incomes)
            savingsRepository.insertProjects(data.savingsProjects)
            spendingLimitRepository.insertLimits(data.spendingLimits)
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.clearAllTables()
        }
    }

    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        if (!backupFolder.exists()) return@withContext emptyList()

        backupFolder.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { file ->
                try {
                    val data = gson.fromJson(file.readText(), BackupData::class.java)
                    BackupInfo(
                        file = file,
                        timestamp = data.timestamp,
                        version = data.version
                    )
                } catch (e: Exception) {
                    null
                }
            }
            ?.filterNotNull()
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    private fun createBackupFile(): File {
        if (!backupFolder.exists()) {
            backupFolder.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        
        return File(backupFolder, "backup_${timestamp}.json")
    }

    data class BackupInfo(
        val file: File,
        val timestamp: Long,
        val version: Int
    ) {
        val date: Date get() = Date(timestamp)
    }
}
