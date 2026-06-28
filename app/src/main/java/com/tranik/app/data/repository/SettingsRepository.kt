package com.tranik.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val theme: String = "dark",
    val language: String = "fa",
    val autoSync: Boolean = true,
    val downloadCovers: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val scanFolders: Set<String> = emptySet()
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taranik_settings")

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val DOWNLOAD_COVERS = booleanPreferencesKey("download_covers")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SCAN_FOLDERS = stringSetPreferencesKey("scan_folders")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME] ?: "dark",
                language = prefs[Keys.LANGUAGE] ?: "fa",
                autoSync = prefs[Keys.AUTO_SYNC] ?: true,
                downloadCovers = prefs[Keys.DOWNLOAD_COVERS] ?: true,
                playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
                scanFolders = prefs[Keys.SCAN_FOLDERS] ?: emptySet()
            )
        }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun saveAutoSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    suspend fun saveDownloadCovers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOWNLOAD_COVERS] = enabled }
    }

    suspend fun savePlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun saveScanFolders(folders: Set<String>) {
        context.dataStore.edit { it[Keys.SCAN_FOLDERS] = folders }
    }
}
