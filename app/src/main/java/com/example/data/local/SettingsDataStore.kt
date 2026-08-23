package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cleaner_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_LARGE_FILE_THRESHOLD_MB = longPreferencesKey("large_file_threshold_mb")
        val KEY_OLD_FILE_THRESHOLD_DAYS = intPreferencesKey("old_file_threshold_days")
        val KEY_SIMILAR_PHOTO_HAMMING_THRESHOLD = intPreferencesKey("similar_photo_threshold")
        val KEY_RECYCLE_BIN_ENABLED = booleanPreferencesKey("recycle_bin_enabled")
        val KEY_LOW_END_MODE = booleanPreferencesKey("low_end_mode")
        val KEY_LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "LIGHT", "DARK", "SYSTEM"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "LIGHT" // Defaults to LIGHT theme
    }

    val largeFileThresholdMb: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LARGE_FILE_THRESHOLD_MB] ?: 25L // 25 MB
    }

    val oldFileThresholdDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_OLD_FILE_THRESHOLD_DAYS] ?: 60 // 60 days
    }

    val similarPhotoThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIMILAR_PHOTO_HAMMING_THRESHOLD] ?: 8 // Hamming distance <= 8
    }

    val recycleBinEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RECYCLE_BIN_ENABLED] ?: true
    }

    val lowEndMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOW_END_MODE] ?: true
    }

    val lastScanTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SCAN_TIMESTAMP] ?: 0L
    }

    suspend fun setLargeFileThresholdMb(mb: Long) {
        context.dataStore.edit { it[KEY_LARGE_FILE_THRESHOLD_MB] = mb }
    }

    suspend fun setOldFileThresholdDays(days: Int) {
        context.dataStore.edit { it[KEY_OLD_FILE_THRESHOLD_DAYS] = days }
    }

    suspend fun setSimilarPhotoThreshold(threshold: Int) {
        context.dataStore.edit { it[KEY_SIMILAR_PHOTO_HAMMING_THRESHOLD] = threshold }
    }

    suspend fun setRecycleBinEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RECYCLE_BIN_ENABLED] = enabled }
    }

    suspend fun setLowEndMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOW_END_MODE] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_SCAN_TIMESTAMP] = timestamp }
    }
}
