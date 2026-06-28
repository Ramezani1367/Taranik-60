package com.tranik.app.data.repository

import com.tranik.app.data.model.formatDuration
import org.junit.Assert.*
import org.junit.Test

class LyricsRepositoryTest {

    @Test
    fun `parsePlainText splits lines correctly`() {
        val repo = LyricsRepository()
        val result = repo.parsePlainText("Line 1\nLine 2\n\nLine 3")
        assertEquals(3, result.size)
        assertEquals("Line 1", result[0].text)
        assertEquals("Line 2", result[1].text)
        assertEquals("Line 3", result[2].text)
    }

    @Test
    fun `parsePlainText ignores blank lines`() {
        val repo = LyricsRepository()
        val result = repo.parsePlainText("Hello\n\n\nWorld")
        assertEquals(2, result.size)
    }

    @Test
    fun `parsePlainText lines are not synced by default`() {
        val repo = LyricsRepository()
        val result = repo.parsePlainText("Hello")
        assertFalse(result[0].synced)
        assertEquals(-1L, result[0].timeMs)
    }

    @Test
    fun `readLrcFile returns empty for non-existent file`() {
        val repo = LyricsRepository()
        val result = repo.readLrcFile("/non/existent/path.lrc")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `readPlainTextFile returns empty for non-existent file`() {
        val repo = LyricsRepository()
        val result = repo.readPlainTextFile("/non/existent/path.txt")
        assertEquals("", result)
    }

    @Test
    fun `findLrcForTrack returns correct path`() {
        val repo = LyricsRepository()
        val result = repo.findLrcForTrack("/storage/Music/song.mp3")
        assertEquals("/storage/Music/song.lrc", result)
    }

    @Test
    fun `createLrcForTrack returns correct path`() {
        val repo = LyricsRepository()
        val result = repo.createLrcForTrack("/storage/Music/song.mp3")
        assertEquals("/storage/Music/song.lrc", result)
    }
}

class FormatDurationTest {

    @Test
    fun `zero milliseconds`() {
        assertEquals("0:00", formatDuration(0))
    }

    @Test
    fun `seconds only`() {
        assertEquals("0:45", formatDuration(45000))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("3:44", formatDuration(224000))
    }

    @Test
    fun `exactly one minute`() {
        assertEquals("1:00", formatDuration(60000))
    }

    @Test
    fun `over one hour`() {
        assertEquals("1:01:01", formatDuration(3661000))
    }

    @Test
    fun `exactly one hour`() {
        assertEquals("1:00:00", formatDuration(3600000))
    }

    @Test
    fun `negative returns zero`() {
        assertEquals("0:00", formatDuration(-1))
    }

    @Test
    fun `large duration`() {
        assertEquals("2:30:00", formatDuration(9000000))
    }
}
