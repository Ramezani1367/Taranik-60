package com.tranik.app.ui.viewmodel

import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.DirtyTag
import com.tranik.app.data.model.Track
import com.tranik.app.data.repository.Id3TagRepository
import com.tranik.app.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagEditorState(
    val track: Track? = null,
    val tagFields: Map<String, String> = emptyMap(),
    val dirtyTags: List<DirtyTag> = emptyList(),
    val isSaving: Boolean = false,
    val saveResult: String? = null,
    val error: String? = null
)

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    private val id3TagRepository: Id3TagRepository,
    private val trackRepository: TrackRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(TagEditorState())
    val state = _state.asStateFlow()

    fun loadTrack(track: Track) {
        _state.update { it.copy(track = track, isSaving = false, saveResult = null, error = null) }

        viewModelScope.launch {
            // خواندن تگ‌های واقعی از فایل
            val tags = id3TagRepository.readTags(track.filePath)
            val dirtyTags = id3TagRepository.detectDirtyTags(track.filePath)

            _state.update {
                it.copy(
                    tagFields = tags,
                    dirtyTags = dirtyTags
                )
            }
        }
    }

    fun updateField(key: String, value: String) {
        _state.update {
            it.copy(tagFields = it.tagFields.toMutableMap().apply { this[key] = value })
        }
    }

    fun cleanTag(key: String) {
        val dirty = _state.value.dirtyTags.find { it.key == key }
        if (dirty != null) {
            updateField(key, dirty.newValue)
            _state.update {
                it.copy(dirtyTags = it.dirtyTags.filter { d -> d.key != key })
            }
        }
    }

    fun ignoreTag(key: String) {
        _state.update {
            it.copy(dirtyTags = it.dirtyTags.filter { d -> d.key != key })
        }
    }

    fun saveID3() {
        val track = _state.value.track ?: return
        val tags = _state.value.tagFields

        _state.update { it.copy(isSaving = true, saveResult = null, error = null) }

        viewModelScope.launch {
            val result = id3TagRepository.writeTags(track.filePath, tags)
            _state.update {
                it.copy(
                    isSaving = false,
                    saveResult = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }

            // بروزرسانی کش
            if (result.isSuccess) {
                trackRepository.invalidateCache()
            }
        }
    }

    fun clearSaveResult() {
        _state.update { it.copy(saveResult = null, error = null) }
    }
}
