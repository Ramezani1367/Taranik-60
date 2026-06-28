package com.tranik.app.data.repository

import com.tranik.app.data.model.LyricLine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor() {

    /**
     * خواندن لیریک از فایل LRC
     */
    fun readLrcFile(lrcPath: String): List<LyricLine> {
        val file = File(lrcPath)
        if (!file.exists()) return emptyList()

        val lines = file.readLines()
        val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        val result = mutableListOf<LyricLine>()

        for (line in lines) {
            val match = lrcRegex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val ms = match.groupValues[3].let {
                    if (it.length == 2) it + "0" else it
                }.toLong()
                val timeMs = minutes * 60000 + seconds * 1000 + ms
                val text = match.groupValues[4].trim()

                if (text.isNotBlank()) {
                    result.add(LyricLine(timeMs = timeMs, text = text, synced = true))
                }
            }
        }

        return result.sortedBy { it.timeMs }
    }

    /**
     * خواندن لیریک ساده (بدون زمان‌بندی) از فایل txt
     */
    fun readPlainTextFile(path: String): String {
        val file = File(path)
        return if (file.exists()) file.readText() else ""
    }

    /**
     * ذخیره لیریک ساده
     */
    fun savePlainText(path: String, text: String): Result<String> {
        return try {
            File(path).writeText(text)
            Result.success("لیریک ذخیره شد ✓")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ذخیره لیریک همگام‌سازی شده به فرمت LRC
     */
    fun saveLrcFile(path: String, lines: List<LyricLine>): Result<String> {
        return try {
            val content = lines
                .filter { it.synced && it.timeMs >= 0 }
                .joinToString("\n") { line ->
                    "[${line.lrcTimeTag}]${line.text}"
                }
            File(path).writeText(content)
            Result.success("فایل LRC ذخیره شد ✓")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * پیدا کردن فایل LRC متناظر با فایل صوتی
     */
    fun findLrcForTrack(trackPath: String): String? {
        val baseName = trackPath.substringBeforeLast(".")
        val lrcPath = "$baseName.lrc"
        return if (File(lrcPath).exists()) lrcPath else null
    }

    /**
     * ساخت فایل LRC جدید برای یه ترک
     */
    fun createLrcForTrack(trackPath: String): String {
        val baseName = trackPath.substringBeforeLast(".")
        return "$baseName.lrc"
    }

    /**
     * پارس کردن متن ساده به خطوط لیریک
     */
    fun parsePlainText(text: String): List<LyricLine> {
        return text.lines()
            .filter { it.isNotBlank() }
            .map { LyricLine(text = it.trim()) }
    }
}
