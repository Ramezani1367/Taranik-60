package com.tranik.app.data.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tranik.app.data.model.Folder
import com.tranik.app.data.model.Track
import com.tranik.app.data.model.formatDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * خواندن تمام فایل‌های صوتی از MediaStore
     */
    fun getAllTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durationCol)
                val filePath = cursor.getString(dataCol) ?: continue

                // فقط فایل‌هایی که حداقل ۳۰ ثانیه هستن (فیلتر رینگتون و صداهای کوتاه)
                if (duration < 30000) continue

                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                val folderPath = filePath.substringBeforeLast("/")

                tracks.add(
                    Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: filePath.substringAfterLast("/"),
                        artist = cursor.getString(artistCol) ?: "نامشخص",
                        album = cursor.getString(albumCol) ?: "نامشخص",
                        albumArtist = cursor.getString(albumArtistCol) ?: "",
                        year = cursor.getString(yearCol) ?: "",
                        trackNumber = cursor.getInt(trackCol),
                        genre = cursor.getString(genreCol) ?: "",
                        durationMs = duration,
                        contentUri = contentUri,
                        filePath = filePath,
                        folderPath = folderPath,
                        size = cursor.getLong(sizeCol),
                        albumId = cursor.getLong(albumIdCol),
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModifiedCol)
                    )
                )
            }
        }

        return tracks
    }

    /**
     * گرفتن فایل‌های یک فولدر خاص
     */
    fun getTracksByFolder(folderPath: String): List<Track> {
        return getAllTracks().filter { it.folderPath == folderPath }
    }

    /**
     * اسکن تمام فولدرهایی که فایل صوتی دارن
     */
    fun getAllFolders(): List<Folder> {
        val tracks = getAllTracks()
        return tracks
            .groupBy { it.folderPath }
            .map { (path, tracksInFolder) ->
                Folder(
                    path = path,
                    name = path.substringAfterLast("/"),
                    trackCount = tracksInFolder.size,
                    lastModified = tracksInFolder.maxOfOrNull { it.dateModified } ?: 0
                )
            }
            .sortedByDescending { it.trackCount }
    }

    /**
     * جستجو در آهنگ‌ها
     */
    fun searchTracks(query: String): List<Track> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return getAllTracks().filter { track ->
            track.title.lowercase().contains(q) ||
            track.artist.lowercase().contains(q) ||
            track.album.lowercase().contains(q)
        }
    }

    /**
     * آدرس کاور آلبوم
     */
    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }
}
