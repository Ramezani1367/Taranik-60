package com.tranik.app.ui.viewmodel

import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.LyricLine
import com.tranik.app.data.repository.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncState(
    val lines: List<LyricLine> = emptyList(),
    val currentIndex: Int = 0,
    val progressText: String = "",
    val canUndo: Boolean = false,
    val isComplete: Boolean = false,
    val saveResult: String? = null,
    val error: String? = null
) {
    val syncedCount: Int get() = lines.count { it.synced }
    val totalCount: Int get() = lines.size
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(SyncState())
    val state = _state.asStateFlow()

    private val undoStack = mutableListOf<List<LyricLine>>()

    fun loadLyrics(rawText: String) {
        val lines = lyricsRepository.parsePlainText(rawText)
        undoStack.clear()
        _state.update {
            it.copy(
                lines = lines,
                currentIndex = 0,
                progressText = "0 / ${lines.size} ثبت شد",
                canUndo = false,
                isComplete = false
            )
        }
    }

    fun stampLine(currentPositionMs: Long) {
        val s = _state.value
        if (s.currentIndex >= s.lines.size) return

        // ذخیره وضعیت قبلی برای undo
        undoStack.add(s.lines)
        if (undoStack.size > 50) undoStack.removeFirst()

        val newLines = s.lines.toMutableList()
        newLines[s.currentIndex] = newLines[s.currentIndex].withTime(currentPositionMs)

        val nextIndex = s.currentIndex + 1
        val isComplete = nextIndex >= newLines.size

        _state.update {
            it.copy(
                lines = newLines,
                currentIndex = nextIndex,
                progressText = "${newLines.count { l -> l.synced }} / ${newLines.size} ثبت شد",
                canUndo = undoStack.isNotEmpty(),
                isComplete = isComplete
            )
        }
    }

    fun undoLastStamp() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        val currentIndex = _state.value.currentIndex - 1

        _state.update {
            it.copy(
                lines = previous,
                currentIndex = maxOf(0, currentIndex),
                progressText = "${previous.count { l -> l.synced }} / ${previous.size} ثبت شد",
                canUndo = undoStack.isNotEmpty(),
                isComplete = false
            )
        }
    }

    fun resetAllStamps() {
        val resetLines = _state.value.lines.map { line ->
            line.copy(timeMs = -1, synced = false)
        }
        undoStack.clear()

        _state.update {
            it.copy(
                lines = resetLines,
                currentIndex = 0,
                progressText = "0 / ${resetLines.size} ثبت شد",
                canUndo = false,
                isComplete = false
            )
        }
    }

    fun adjustTime(index: Int, deltaMs: Long) {
        val lines = _state.value.lines.toMutableList()
        if (index < 0 || index >= lines.size) return
        val line = lines[index]
        if (!line.synced) return

        lines[index] = line.copy(timeMs = maxOf(0, line.timeMs + deltaMs))
        _state.update { it.copy(lines = lines) }
    }

    fun exportLrc(trackPath: String) {
        viewModelScope.launch {
            val lrcPath = lyricsRepository.createLrcForTrack(trackPath)
            val result = lyricsRepository.saveLrcFile(lrcPath, _state.value.lines)
            _state.update {
                it.copy(
                    saveResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearSaveResult() {
        _state.update { it.copy(saveResult = null, error = null) }
    }
}
