package com.cashruler.data.repositories

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
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
    private val spendingLimitRepository: SpendingLimitRepository,
    private val settingsRepository: SettingsRepository
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

    private companion object {
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

    private suspend fun importData(filePath: String): Unit = withContext(Dispatchers.IO) {
        val json = File(filePath).readText()
        val data = gson.fromJson(json, BackupData::class.java)

        if (data.version > BACKUP_VERSION) {
            throw IllegalStateException("Version de sauvegarde non supportée")
        }

        database.withTransaction {
            // Suppression des données existantes
            deleteAllData()

            // Import des nouvelles données
            expenseRepository.addExpenses(data.expenses)
            incomeRepository.addIncomes(data.incomes)
            savingsRepository.insertProjects(data.savingsProjects)
            spendingLimitRepository.insertLimits(data.spendingLimits)
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.clearAllTables()
        }
    }

    private suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
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

data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.WEEKLY
)

enum class BackupFrequency {
    DAILY, WEEKLY, MONTHLY
}

suspend fun BackupRepository.getSettings(): BackupSettings = withContext(Dispatchers.IO) {
    // Dummy implementation, replace with actual logic to retrieve settings
    BackupSettings(
        autoBackupEnabled = settingsRepository.getAutoBackupEnabled(),
        autoBackupFrequency = when (settingsRepository.getAutoBackupFrequency()) {
            0 -> BackupFrequency.DAILY
            1 -> BackupFrequency.WEEKLY
            2 -> BackupFrequency.MONTHLY
            else -> BackupFrequency.WEEKLY
        }
    )
}

suspend fun BackupRepository.getLastBackupDate(): Date? = withContext(Dispatchers.IO) {
    getBackupFiles().maxByOrNull { it.timestamp }?.date
}

suspend fun BackupRepository.getBackupFiles(): List<BackupRepository.BackupInfo> =
    withContext(Dispatchers.IO) {
        listBackups()
    }

suspend fun BackupRepository.createBackup(): File = withContext(Dispatchers.IO) {
    exportData()
    listBackups().maxByOrNull { it.timestamp }?.file ?: throw IllegalStateException("Backup failed")
}

suspend fun BackupRepository.restoreBackup(backupFileName: String) =
    withContext(Dispatchers.IO) {
        val backupFile =
            getBackupFiles().find { it.file.name == backupFileName }?.file ?: throw IllegalArgumentException(
                "Backup not found"
            )
        importData(backupFile.absolutePath)
    }

suspend fun BackupRepository.deleteBackup(backupFileName: String) =
    withContext(Dispatchers.IO) {
        val backupFile =
            getBackupFiles().find { it.file.name == backupFileName }?.file ?: throw IllegalArgumentException(
                "Backup not found"
            )
        if (!backupFile.delete()) {
            throw IOException("Failed to delete backup file")
        }
    }

suspend fun BackupRepository.exportBackup(backupFileName: String, destinationUri: Uri) =
    withContext(Dispatchers.IO) {
        val backupFile =
            getBackupFiles().find { it.file.name == backupFileName }?.file ?: throw IllegalArgumentException(
                "Backup not found"
            )

        val resolver: ContentResolver = context.contentResolver
        try {
            resolver.openOutputStream(destinationUri)?.use { outputStream ->
                backupFile.inputStream().copyTo(outputStream)
            } ?: throw IOException("Failed to open output stream for destination URI")
        } catch (e: IOException) {
            throw IOException("Error exporting backup: ${e.message}", e)
        }
    }

suspend fun BackupRepository.importBackup(sourceUri: Uri) = withContext(Dispatchers.IO) {
    val resolver: ContentResolver = context.contentResolver
    val tempFile = File.createTempFile("import_backup", ".json", context.cacheDir)

    try {
        resolver.openInputStream(sourceUri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open input stream for source URI")

        importData(tempFile.absolutePath)
    } catch (e: IOException) {
        throw IOException("Error importing backup: ${e.message}", e)
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}

suspend fun BackupRepository.setAutoBackup(
    enabled: Boolean,
    frequency: BackupFrequency
) = withContext(Dispatchers.IO) {
    settingsRepository.setAutoBackupEnabled(enabled)
    settingsRepository.setAutoBackupFrequency(
        when (frequency) {
            BackupFrequency.DAILY -> 0
            BackupFrequency.WEEKLY -> 1
            BackupFrequency.MONTHLY -> 2
        }
    )
}
}
