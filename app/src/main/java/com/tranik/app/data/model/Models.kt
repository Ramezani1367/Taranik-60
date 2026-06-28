package com.tranik.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ==================== Track ====================
@Parcelize
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "نامشخص",
    val albumArtist: String = "",
    val year: String = "",
    val trackNumber: Int = 0,
    val genre: String = "",
    val durationMs: Long = 0,
    val contentUri: String = "",
    val filePath: String = "",
    val folderPath: String = "",
    val size: Long = 0,
    val hasLyric: Boolean = false,
    val tagClean: Boolean = true,
    val albumId: Long = -1,
    val dateAdded: Long = 0,
    val dateModified: Long = 0
) : Parcelable {
    val duration: String
        get() = formatDuration(durationMs)
    val displayArtist: String
        get() = if (artist.isNotBlank() && artist != "<unknown>") artist else "نامشخص"
    val displayAlbum: String
        get() = if (album.isNotBlank() && album != "<unknown>") album else "نامشخص"
    val displayName: String
        get() = if (title.isNotBlank()) title else filePath.substringAfterLast("/")
    companion object {
        fun empty() = Track(id = -1, title = "", artist = "", durationMs = 0)
    }
}

// ==================== Folder ====================
@Parcelize
data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int = 0,
    val lastModified: Long = 0
) : Parcelable {
    val id: String
        get() = path.hashCode().toString()
}

// ==================== LyricLine ====================
@Parcelize
data class LyricLine(
    val timeMs: Long = -1,
    val text: String,
    val synced: Boolean = false,
    val isCurrent: Boolean = false,
    val translation: String? = null
) : Parcelable {
    val time: String
        get() = if (timeMs >= 0) formatDuration(timeMs) else "--:--"
    val lrcTimeTag: String
        get() {
            if (timeMs < 0) return ""
            val minutes = timeMs / 60000
            val seconds = (timeMs % 60000) / 1000
            val hundredths = (timeMs % 1000) / 10
            return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
        }
    fun withTime(newTimeMs: Long) = copy(timeMs = newTimeMs, synced = true)
    fun withTranslation(t: String) = copy(translation = t)
}

// ==================== DirtyTag ====================
data class DirtyTag(
    val field: String,
    val key: String,
    val oldValue: String,
    val newValue: String
)

// ==================== Helpers ====================
fun formatDuration(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
