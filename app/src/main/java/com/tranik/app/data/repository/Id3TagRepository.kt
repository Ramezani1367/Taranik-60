package com.tranik.app.data.repository

import com.mpatric.mp3agic.Mp3File
import com.tranik.app.data.model.DirtyTag
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Id3TagRepository @Inject constructor() {

    fun readTags(filePath: String): Map<String, String> {
        return try {
            val mp3 = Mp3File(filePath)
            val tags = mutableMapOf<String, String>()

            if (mp3.hasId3v2Tag()) {
                val tag = mp3.id3v2Tag
                tags["title"] = tag.title ?: ""
                tags["artist"] = tag.artist ?: ""
                tags["album"] = tag.album ?: ""
                tags["albumArtist"] = tag.albumArtist ?: ""
                tags["year"] = tag.year ?: ""
                tags["trackNumber"] = tag.track ?: ""
                tags["genre"] = tag.genreDescription ?: ""
                tags["comment"] = tag.comment ?: ""
                tags["composer"] = tag.composer ?: ""
                tags["lyrics"] = tag.lyrics ?: ""
            } else if (mp3.hasId3v1Tag()) {
                val tag = mp3.id3v1Tag
                tags["title"] = tag.title ?: ""
                tags["artist"] = tag.artist ?: ""
                tags["album"] = tag.album ?: ""
                tags["year"] = tag.year ?: ""
                tags["trackNumber"] = tag.track ?: ""
                tags["comment"] = tag.comment ?: ""
            }
            tags
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun writeTags(filePath: String, tags: Map<String, String>): Result<String> {
        return try {
            val mp3 = Mp3File(filePath)

            if (!mp3.hasId3v2Tag()) {
                mp3.removeId3v1Tag()
                mp3.addId3v2Tag()
            }

            val tag = mp3.id3v2Tag
            tags["title"]?.let { tag.title = it }
            tags["artist"]?.let { tag.artist = it }
            tags["album"]?.let { tag.album = it }
            tags["albumArtist"]?.let { tag.albumArtist = it }
            tags["year"]?.let { tag.year = it }
            tags["trackNumber"]?.let { tag.track = it }
            tags["comment"]?.let { tag.comment = it }
            tags["composer"]?.let { tag.composer = it }
            tags["genre"]?.let {
                try { tag.genre = it.toInt() } catch (_: Exception) { tag.genreDescription = it }
            }

            val outPath = "$filePath.tmp"
            mp3.save(outPath)

            val original = File(filePath)
            val temp = File(outPath)
            original.delete()
            temp.renameTo(original)

            Result.success("تگ‌ها ذخیره شد ✓")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun detectDirtyTags(filePath: String): List<DirtyTag> {
        val tags = readTags(filePath)
        val dirtyTags = mutableListOf<DirtyTag>()

        val suspiciousPatterns = listOf(
            Regex("""\[.*\]""", RegexOption.IGNORE_CASE),
            Regex("""https?://""", RegexOption.IGNORE_CASE),
            Regex("""www\."""),
            Regex("""<[^>]+>"""),
            Regex(""".com|.net|.org""", RegexOption.IGNORE_CASE)
        )

        tags.forEach { (key, value) ->
            if (value.isNotBlank()) {
                suspiciousPatterns.forEach { pattern ->
                    if (pattern.containsMatchIn(value)) {
                        val cleaned = pattern.replace(value, "").trim()
                        if (cleaned.isNotBlank() && cleaned != value) {
                            dirtyTags.add(DirtyTag(field = fieldLabel(key), key = key, oldValue = value, newValue = cleaned))
                            return@forEach
                        }
                    }
                }
            }
        }
        return dirtyTags
    }

    private fun fieldLabel(key: String): String = when (key) {
        "title" -> "عنوان"
        "artist" -> "هنرمند"
        "album" -> "آلبوم"
        "albumArtist" -> "هنرمند آلبوم"
        "year" -> "سال"
        "genre" -> "ژانر"
        "composer" -> "آهنگساز"
        "comment" -> "نظر"
        else -> key
    }
}
