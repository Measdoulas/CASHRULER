package com.cashruler.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashruler.data.repositories.SettingsRepository
import com.cashruler.data.repositories.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel pour la gestion des paramètres de l'application
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Messages d'erreur
    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Messages de succès
    private val _success = MutableSharedFlow<String>()
    val success = _success.asSharedFlow()

    // État des paramètres
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    /**
     * Met à jour les paramètres de l'application
     */
    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSettings(settings)
                _success.emit("Paramètres mis à jour avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la mise à jour des paramètres: ${e.message}")
            }
        }
    }

    /**
     * Active/Désactive le thème sombre
     */
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setDarkMode(enabled)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement de thème: ${e.message}")
            }
        }
    }

    /**
     * Change la langue de l'application
     */
    fun setLanguage(locale: Locale) {
        viewModelScope.launch {
            try {
                settingsRepository.setLanguage(locale)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement de langue: ${e.message}")
            }
        }
    }

    /**
     * Active/Désactive les notifications
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationsEnabled(enabled)
            } catch (e: Exception) {
                _error.emit("Erreur lors de la mise à jour des notifications: ${e.message}")
            }
        }
    }

    /**
     * Configure la devise par défaut
     */
    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setCurrency(currencyCode)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement de devise: ${e.message}")
            }
        }
    }

    /**
     * Configure le format de date
     */
    fun setDateFormat(format: DateFormat) {
        viewModelScope.launch {
            try {
                settingsRepository.setDateFormat(format)
            } catch (e: Exception) {
                _error.emit("Erreur lors du changement de format de date: ${e.message}")
            }
        }
    }

    /**
     * Configure la sauvegarde automatique
     */
    fun setAutoBackup(enabled: Boolean, frequency: BackupFrequency) {
        viewModelScope.launch {
            try {
                settingsRepository.setAutoBackup(enabled, frequency)
            } catch (e: Exception) {
                _error.emit("Erreur lors de la configuration de la sauvegarde: ${e.message}")
            }
        }
    }

    /**
     * Exporte les données
     */
    fun exportData(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                backupRepository.exportData(path)
                _success.emit("Données exportées avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'exportation: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Importe les données
     */
    fun importData(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                backupRepository.importData(path)
                _success.emit("Données importées avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de l'importation: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Réinitialise les paramètres par défaut
     */
    fun resetToDefault() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefault()
                _success.emit("Paramètres réinitialisés avec succès")
            } catch (e: Exception) {
                _error.emit("Erreur lors de la réinitialisation: ${e.message}")
            }
        }
    }
}

/**
 * Paramètres de l'application
 */
data class AppSettings(
    val darkMode: Boolean = false,
    val language: Locale = Locale.getDefault(),
    val notificationsEnabled: Boolean = true,
    val currency: String = Currency.getInstance(Locale.getDefault()).currencyCode,
    val dateFormat: DateFormat = DateFormat.DEFAULT,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.WEEKLY
)

/**
 * Formats de date disponibles
 */
enum class DateFormat {
    DEFAULT,       // Format système
    DD_MM_YYYY,    // 31/12/2024
    MM_DD_YYYY,    // 12/31/2024
    YYYY_MM_DD     // 2024-12-31
}

/**
 * Fréquences de sauvegarde automatique
 */
enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}
