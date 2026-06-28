package com.tranik.app.ui.viewmodel

import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.Folder
import com.tranik.app.data.model.Track
import com.tranik.app.data.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val folders: List<Folder> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val filteredTracks: List<Track> = emptyList(),
    val activeFolderPath: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val trackRepository: TrackRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    init {
        loadFoldersAndTracks()
    }

    private fun loadFoldersAndTracks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val folders = trackRepository.getAllFolders()
                val firstFolder = folders.firstOrNull()
                val tracks = if (firstFolder != null) {
                    trackRepository.getTracksByFolder(firstFolder.path)
                } else emptyList()

                _state.update {
                    it.copy(
                        folders = folders,
                        tracks = tracks,
                        filteredTracks = tracks,
                        activeFolderPath = firstFolder?.path ?: "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectFolder(folderPath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val tracks = trackRepository.getTracksByFolder(folderPath)
                _state.update {
                    it.copy(
                        activeFolderPath = folderPath,
                        tracks = tracks,
                        filteredTracks = applySearch(it.searchQuery, tracks),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun search(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredTracks = applySearch(query, it.tracks)
            )
        }
    }

    private fun applySearch(query: String, tracks: List<Track>): List<Track> {
        if (query.isBlank()) return tracks
        val q = query.lowercase()
        return tracks.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    fun refresh() {
        trackRepository.invalidateCache()
        loadFoldersAndTracks()
    }
}
