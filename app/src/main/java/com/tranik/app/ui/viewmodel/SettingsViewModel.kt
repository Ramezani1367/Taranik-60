package com.tranik.app.ui.viewmodel

import android.content.Context
import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.repository.AppSettings
import com.tranik.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val theme: String = "dark",
    val language: String = "fa",
    val autoSync: Boolean = true,
    val downloadCovers: Boolean = true,
    val cacheSize: String = "محاسبه...",
    val versionName: String = "1.2.0",
    val isLoaded: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        // لود تنظیمات ذخیره‌شده
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        theme = settings.theme,
                        language = settings.language,
                        autoSync = settings.autoSync,
                        downloadCovers = settings.downloadCovers,
                        isLoaded = true
                    )
                }
            }
        }
    }

    fun setTheme(theme: String) {
        _state.update { it.copy(theme = theme) }
        viewModelScope.launch { settingsRepository.saveTheme(theme) }
    }

    fun setLanguage(lang: String) {
        _state.update { it.copy(language = lang) }
        viewModelScope.launch { settingsRepository.saveLanguage(lang) }
    }

    fun setAutoSync(enabled: Boolean) {
        _state.update { it.copy(autoSync = enabled) }
        viewModelScope.launch { settingsRepository.saveAutoSync(enabled) }
    }

    fun setDownloadCovers(enabled: Boolean) {
        _state.update { it.copy(downloadCovers = enabled) }
        viewModelScope.launch { settingsRepository.saveDownloadCovers(enabled) }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                cacheDir.walkTopDown().filter { it.isFile }.forEach { it.delete() }
                _state.update { it.copy(cacheSize = "0 KB") }
            } catch (e: Exception) {
                _state.update { it.copy(cacheSize = "خطا") }
            }
        }
    }

    fun calculateCacheSize(context: Context) {
        viewModelScope.launch {
            try {
                val size = context.cacheDir.walkTopDown()
                    .filter { it.isFile }.sumOf { it.length() }
                _state.update { it.copy(cacheSize = formatCacheSize(size)) }
            } catch (e: Exception) {
                _state.update { it.copy(cacheSize = "نامشخص") }
            }
        }
    }

    private fun formatCacheSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
