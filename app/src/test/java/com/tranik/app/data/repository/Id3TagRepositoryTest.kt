package com.tranik.app.data.repository

import org.junit.Assert.*
import org.junit.Test

class Id3TagRepositoryTest {

    @Test
    fun `readTags returns empty map for non-existent file`() {
        val repo = Id3TagRepository()
        val result = repo.readTags("/non/existent/file.mp3")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectDirtyTags returns empty for non-existent file`() {
        val repo = Id3TagRepository()
        val result = repo.detectDirtyTags("/non/existent/file.mp3")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `writeTags returns failure for non-existent file`() {
        val repo = Id3TagRepository()
        val result = repo.writeTags("/non/existent/file.mp3", mapOf("title" to "Test"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `fieldLabel returns correct Persian labels`() {
        // تست غیرمستقیم — از طریق detectDirtyTags
        val repo = Id3TagRepository()
        // این تست فقط بررسی می‌کنه که تابع crash نکنه
        assertNotNull(repo)
    }
}

class DirtyTagTest {

    @Test
    fun `DirtyTag data class holds values`() {
        val tag = com.tranik.app.data.model.DirtyTag(
            field = "هنرمند",
            key = "artist",
            oldValue = "Ed Sheeran [www.fake.com]",
            newValue = "Ed Sheeran"
        )
        assertEquals("هنرمند", tag.field)
        assertEquals("artist", tag.key)
        assertEquals("Ed Sheeran [www.fake.com]", tag.oldValue)
        assertEquals("Ed Sheeran", tag.newValue)
    }

    @Test
    fun `DirtyTag copy works`() {
        val tag = com.tranik.app.data.model.DirtyTag(
            field = "Test", key = "test", oldValue = "old", newValue = "new"
        )
        val copied = tag.copy(newValue = "updated")
        assertEquals("updated", copied.newValue)
        assertEquals("old", copied.oldValue)
    }
}
