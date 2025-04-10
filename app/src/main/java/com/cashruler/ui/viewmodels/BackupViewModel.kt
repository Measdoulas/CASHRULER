package com.cashruler.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.repositories.BackupRepository
import com.cashruler.data.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel pour la gestion des sauvegardes
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // État des sauvegardes
    private val _backupState = MutableStateFlow(BackupState())
    val backupState = _backupState.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Messages de succès
    private val _success = MutableSharedFlow<String>()
    val success = _success.asSharedFlow()

    init {
        loadBackupState()
    }

    /**
     * Charge l'état des sauvegardes
     */
    private fun loadBackupState() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                val lastBackup = backupRepository.getLastBackupDate()
                val backupFiles = backupRepository.getBackupFiles()

                _backupState.update { state ->
                    state.copy(
                        autoBackupEnabled = settings.autoBackupEnabled,
                        autoBackupFrequency = settings.autoBackupFrequency,
                        lastBackupDate = lastBackup,
                        availableBackups = backupFiles
                    )
                }
            } catch (e: Exception) {
                _error.emit("Erreur lors du chargement de l'état des sauvegardes: ${e.message}")
            }
        }
    }

    /**
     * Crée une nouvelle sauvegarde
     */
    fun createBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val backupFile = backupRepository.createBackup()
                _backupState.update { it.copy(
                    lastBackupDate = Date(),
                    availableBackups = backupRepository.getBackupFiles()
                ) }
                _success.emit("Sauvegarde créée avec succès: ${backupFile.name}")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la création de la sauvegarde: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restaure une sauvegarde
     */
    fun restoreBackup(backupFileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                backupRepository.restoreBackup(backupFileName)
                _success.emit("Sauvegarde restaurée avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la restauration: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Supprime une sauvegarde
     */
    fun deleteBackup(backupFileName: String) {
        viewModelScope.launch {
            try {
                backupRepository.deleteBackup(backupFileName)
                _backupState.update { it.copy(
                    availableBackups = backupRepository.getBackupFiles()
                ) }
                _success.emit("Sauvegarde supprimée avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la suppression: ${e.message}")
            }
        }
    }

    /**
     * Exporte une sauvegarde vers un URI externe
     */
    fun exportBackup(backupFileName: String, destinationUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                backupRepository.exportBackup(backupFileName, destinationUri)
                _success.emit("Sauvegarde exportée avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'exportation: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Importe une sauvegarde depuis un URI externe
     */
    fun importBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                backupRepository.importBackup(sourceUri)
                _backupState.update { it.copy(
                    availableBackups = backupRepository.getBackupFiles()
                ) }
                _success.emit("Sauvegarde importée avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'importation: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Configure la sauvegarde automatique
     */
    fun configureAutoBackup(enabled: Boolean, frequency: BackupFrequency) {
        viewModelScope.launch {
            try {
                settingsRepository.setAutoBackup(enabled, frequency)
                _backupState.update { it.copy(
                    autoBackupEnabled = enabled,
                    autoBackupFrequency = frequency
                ) }
                _success.emit("Configuration de la sauvegarde automatique mise à jour")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la configuration: ${e.message}")
            }
        }
    }
}

/**
 * État des sauvegardes
 */
data class BackupState(
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val lastBackupDate: Date? = null,
    val availableBackups: List<BackupFile> = emptyList()
)

/**
 * Informations sur un fichier de sauvegarde
 */
data class BackupFile(
    val name: String,
    val date: Date,
    val size: Long,
    val version: String
)
