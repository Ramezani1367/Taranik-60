package com.tranik.app.data.model

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    @Test
    fun `Track duration formatted correctly for short duration`() {
        val track = Track(id = 1, title = "Test", artist = "Artist", durationMs = 224000)
        assertEquals("3:44", track.duration)
    }

    @Test
    fun `Track duration formatted correctly for long duration`() {
        val track = Track(id = 1, title = "Test", artist = "Artist", durationMs = 3661000)
        assertEquals("1:01:01", track.duration)
    }

    @Test
    fun `Track duration zero`() {
        val track = Track(id = 1, title = "Test", artist = "Artist", durationMs = 0)
        assertEquals("0:00", track.duration)
    }

    @Test
    fun `Track displayArtist returns name when valid`() {
        val track = Track(id = 1, title = "Test", artist = "Ed Sheeran", durationMs = 0)
        assertEquals("Ed Sheeran", track.displayArtist)
    }

    @Test
    fun `Track displayArtist returns نامشخص for unknown`() {
        val track = Track(id = 1, title = "Test", artist = "<unknown>", durationMs = 0)
        assertEquals("نامشخص", track.displayArtist)
    }

    @Test
    fun `Track displayAlbum returns نامشخص for unknown`() {
        val track = Track(id = 1, title = "Test", artist = "X", album = "<unknown>", durationMs = 0)
        assertEquals("نامشخص", track.displayAlbum)
    }

    @Test
    fun `Track displayName returns title when not blank`() {
        val track = Track(id = 1, title = "My Song", artist = "X", durationMs = 0)
        assertEquals("My Song", track.displayName)
    }

    @Test
    fun `LyricLine time formatted correctly`() {
        val line = LyricLine(timeMs = 66000, text = "Hello")
        assertEquals("1:06", line.time)
    }

    @Test
    fun `LyricLine time shows dashes when not synced`() {
        val line = LyricLine(text = "Hello")
        assertEquals("--:--", line.time)
    }

    @Test
    fun `LyricLine lrcTimeTag formatted correctly`() {
        val line = LyricLine(timeMs = 66123, text = "Hello")
        assertEquals("01:06.12", line.lrcTimeTag)
    }

    @Test
    fun `LyricLine withTime marks as synced`() {
        val line = LyricLine(text = "Hello").withTime(5000)
        assertTrue(line.synced)
        assertEquals(5000, line.timeMs)
    }

    @Test
    fun `LyricLine withTranslation adds translation`() {
        val line = LyricLine(text = "Hello").withTranslation("سلام")
        assertEquals("سلام", line.translation)
    }

    @Test
    fun `Folder id is derived from path hash`() {
        val folder = Folder(path = "/storage/Music", name = "Music")
        assertNotNull(folder.id)
        assertTrue(folder.id.isNotBlank())
    }

    @Test
    fun `formatDuration handles negative input`() {
        assertEquals("0:00", formatDuration(-1))
    }

    @Test
    fun `formatDuration handles hours`() {
        assertEquals("1:23:45", formatDuration(5025000))
    }

    @Test
    fun `DirtyTag holds correct data`() {
        val tag = DirtyTag(field = "هنرمند", key = "artist", oldValue = "Ed [www.fake.com]", newValue = "Ed")
        assertEquals("هنرمند", tag.field)
        assertEquals("artist", tag.key)
        assertEquals("Ed [www.fake.com]", tag.oldValue)
        assertEquals("Ed", tag.newValue)
    }

    @Test
    fun `Track empty factory`() {
        val track = Track.empty()
        assertEquals(-1, track.id)
        assertEquals("", track.title)
    }

    @Test
    fun `Track companion object is accessible`() {
        val track = Track.empty()
        assertNotNull(track)
    }
}
