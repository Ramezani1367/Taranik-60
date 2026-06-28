package com.tranik.app.ui.viewmodel

import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.LyricLine
import com.tranik.app.data.model.Track
import com.tranik.app.data.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsState(
    val track: Track? = null,
    val lyricsText: String = "",
    val isLrcFormat: Boolean = false,
    val isSaving: Boolean = false,
    val saveResult: String? = null,
    val error: String? = null
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(LyricsState())
    val state = _state.asStateFlow()

    fun loadTrack(track: Track) {
        _state.update { it.copy(track = track, saveResult = null, error = null) }

        viewModelScope.launch {
            // پیدا کردن فایل لیریک متناظر
            val lrcPath = lyricsRepository.findLrcForTrack(track.filePath)
            if (lrcPath != null) {
                // لیریک همگام‌سازی شده LRC
                val lines = lyricsRepository.readLrcFile(lrcPath)
                val text = lines.joinToString("\n") { line ->
                    if (line.synced) "[${line.lrcTimeTag}]${line.text}" else line.text
                }
                _state.update { it.copy(lyricsText = text, isLrcFormat = true) }
            } else {
                // فایل txt با همون نام
                val txtPath = track.filePath.substringBeforeLast(".") + ".txt"
                val text = lyricsRepository.readPlainTextFile(txtPath)
                _state.update { it.copy(lyricsText = text, isLrcFormat = false) }
            }
        }
    }

    fun updateLyricsText(text: String) {
        _state.update { it.copy(lyricsText = text) }
    }

    fun setLrcFormat(isLrc: Boolean) {
        _state.update { it.copy(isLrcFormat = isLrc) }
    }

    fun clearLyrics() {
        _state.update { it.copy(lyricsText = "") }
    }

    fun saveLyrics() {
        val track = _state.value.track ?: return
        val text = _state.value.lyricsText

        _state.update { it.copy(isSaving = true, saveResult = null, error = null) }

        viewModelScope.launch {
            val result = if (_state.value.isLrcFormat) {
                val lrcPath = lyricsRepository.createLrcForTrack(track.filePath)
                val lines = lyricsRepository.parsePlainText(text)
                lyricsRepository.saveLrcFile(lrcPath, lines.map { it.copy(timeMs = -1) })
            } else {
                val txtPath = track.filePath.substringBeforeLast(".") + ".txt"
                lyricsRepository.savePlainText(txtPath, text)
            }

            _state.update {
                it.copy(
                    isSaving = false,
                    saveResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun getLinesForSync(): List<LyricLine> {
        return lyricsRepository.parsePlainText(_state.value.lyricsText)
    }

    fun clearSaveResult() {
        _state.update { it.copy(saveResult = null, error = null) }
    }
}
