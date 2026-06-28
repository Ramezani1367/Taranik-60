package com.tranik.app.data.repository

import com.tranik.app.data.model.Folder
import com.tranik.app.data.model.Track
import com.tranik.app.data.source.MediaStoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource
) {

    private var cachedTracks: List<Track>? = null
    private var cachedFolders: List<Folder>? = null

    suspend fun getAllTracks(): List<Track> {
        cachedTracks = mediaStoreDataSource.getAllTracks()
        return cachedTracks!!
    }

    suspend fun getTracksByFolder(folderPath: String): List<Track> {
        val tracks = cachedTracks ?: getAllTracks()
        return tracks.filter { it.folderPath == folderPath }
    }

    suspend fun getAllFolders(): List<Folder> {
        cachedFolders = mediaStoreDataSource.getAllFolders()
        return cachedFolders!!
    }

    suspend fun searchTracks(query: String): List<Track> {
        val tracks = cachedTracks ?: getAllTracks()
        if (query.isBlank()) return tracks
        val q = query.lowercase()
        return tracks.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    suspend fun getTrackById(id: Long): Track? {
        val tracks = cachedTracks ?: getAllTracks()
        return tracks.find { it.id == id }
    }

    fun getAlbumArtUri(albumId: Long): android.net.Uri {
        return mediaStoreDataSource.getAlbumArtUri(albumId)
    }

    fun invalidateCache() {
        cachedTracks = null
        cachedFolders = null
    }
}
