package com.cashruler.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val LIMITS_NOTIFICATIONS = booleanPreferencesKey("limits_notifications")
        val SAVINGS_NOTIFICATIONS = booleanPreferencesKey("savings_notifications")
        val AUTO_RESET_LIMITS = booleanPreferencesKey("auto_reset_limits")
        val LIMITS_WARNING_THRESHOLD = floatPreferencesKey("limits_warning_threshold")
    }

    private val dataStore = context.dataStore

    suspend fun getDarkThemeEnabled(): Boolean {
        return getValue(PreferencesKeys.DARK_THEME, false)
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        setValue(PreferencesKeys.DARK_THEME, enabled)
    }

    suspend fun getHideAmounts(): Boolean {
        return getValue(PreferencesKeys.HIDE_AMOUNTS, false)
    }

    suspend fun setHideAmounts(hide: Boolean) {
        setValue(PreferencesKeys.HIDE_AMOUNTS, hide)
    }

    suspend fun getLimitsNotificationsEnabled(): Boolean {
        return getValue(PreferencesKeys.LIMITS_NOTIFICATIONS, true)
    }

    suspend fun setLimitsNotificationsEnabled(enabled: Boolean) {
        setValue(PreferencesKeys.LIMITS_NOTIFICATIONS, enabled)
    }

    suspend fun getSavingsNotificationsEnabled(): Boolean {
        return getValue(PreferencesKeys.SAVINGS_NOTIFICATIONS, true)
    }

    suspend fun setSavingsNotificationsEnabled(enabled: Boolean) {
        setValue(PreferencesKeys.SAVINGS_NOTIFICATIONS, enabled)
    }

    suspend fun getAutoResetLimits(): Boolean {
        return getValue(PreferencesKeys.AUTO_RESET_LIMITS, true)
    }

    suspend fun setAutoResetLimits(enabled: Boolean) {
        setValue(PreferencesKeys.AUTO_RESET_LIMITS, enabled)
    }

    suspend fun getLimitsWarningThreshold(): Float {
        return getValue(PreferencesKeys.LIMITS_WARNING_THRESHOLD, 80f)
    }

    suspend fun setLimitsWarningThreshold(threshold: Float) {
        setValue(PreferencesKeys.LIMITS_WARNING_THRESHOLD, threshold)
    }

    fun observeDarkTheme(): Flow<Boolean> = observeValue(PreferencesKeys.DARK_THEME, false)
    fun observeHideAmounts(): Flow<Boolean> = observeValue(PreferencesKeys.HIDE_AMOUNTS, false)
    fun observeLimitsNotifications(): Flow<Boolean> = observeValue(PreferencesKeys.LIMITS_NOTIFICATIONS, true)
    fun observeSavingsNotifications(): Flow<Boolean> = observeValue(PreferencesKeys.SAVINGS_NOTIFICATIONS, true)
    fun observeAutoResetLimits(): Flow<Boolean> = observeValue(PreferencesKeys.AUTO_RESET_LIMITS, true)
    fun observeLimitsWarningThreshold(): Flow<Float> = observeValue(PreferencesKeys.LIMITS_WARNING_THRESHOLD, 80f)

    private suspend fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): T {
        return try {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { preferences ->
                    preferences[key] ?: defaultValue
                }
                .first()
        } catch (e: Exception) {
            defaultValue
        }
    }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    private fun <T> observeValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }
}
