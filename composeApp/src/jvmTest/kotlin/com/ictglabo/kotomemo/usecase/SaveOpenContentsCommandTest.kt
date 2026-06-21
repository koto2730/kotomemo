package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.adapter.repository.FileContentsRepository
import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.entity.LineEnding
import com.ictglabo.kotomemo.framework.file.DefaultFileManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveOpenContentsCommandTest {

    private val repo = FileContentsRepository(DefaultFileManager())
    private lateinit var tmp: Path

    @AfterTest
    fun cleanup() {
        if (::tmp.isInitialized) tmp.deleteIfExists()
    }

    @Test
    fun `save then open round-trips text with LF`() {
        tmp = Files.createTempFile("kotomemo-test-", ".txt")
        val original = Contents.empty()
            .withText("hello\nworld")
            .copy(lineEnding = LineEnding.LF)

        val saved = SaveContentsCommand(repo)
            .execute(SaveContentsCommand.Input(original, tmp))
        assertEquals(tmp, saved.filePath)
        assertFalse(saved.isDirty)

        val reopened = OpenContentsCommand(repo).execute(tmp)
        assertEquals("hello\nworld", reopened.text)
        assertEquals(LineEnding.LF, reopened.lineEnding)
    }

    @Test
    fun `save with CRLF writes CRLF on disk`() {
        tmp = Files.createTempFile("kotomemo-test-", ".txt")
        val original = Contents.empty()
            .withText("a\nb\nc")
            .copy(lineEnding = LineEnding.CRLF)

        SaveContentsCommand(repo).execute(SaveContentsCommand.Input(original, tmp))
        val raw = Files.readString(tmp)
        assertTrue(raw.contains("\r\n"), "expected CRLF in $raw")
    }

    @Test
    fun `BOM round-trips when enabled`() {
        tmp = Files.createTempFile("kotomemo-test-", ".txt")
        val original = Contents.empty()
            .withText("xyz")
            .copy(hasBom = true)

        SaveContentsCommand(repo).execute(SaveContentsCommand.Input(original, tmp))
        val bytes = Files.readAllBytes(tmp)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        val reopened = OpenContentsCommand(repo).execute(tmp)
        assertTrue(reopened.hasBom)
        assertEquals("xyz", reopened.text)
    }

    @Test
    fun `NewContentsCommand returns empty untitled`() {
        val c = NewContentsCommand().execute(Unit)
        assertEquals("", c.text)
        assertEquals(Contents.UNTITLED, c.displayName)
    }

    @Test
    fun `open CRLF file normalises buffer to LF but keeps CRLF metadata`() {
        tmp = Files.createTempFile("kotomemo-test-", ".txt")
        Files.write(tmp, "a\r\nb\r\nc".toByteArray(Charsets.UTF_8))

        val loaded = OpenContentsCommand(repo).execute(tmp)

        assertEquals("a\nb\nc", loaded.text)
        assertEquals(LineEnding.CRLF, loaded.lineEnding)
    }

    @Test
    fun `open file with stray CR is normalised to LF`() {
        tmp = Files.createTempFile("kotomemo-test-", ".txt")
        Files.write(tmp, "a\rb\rc".toByteArray(Charsets.UTF_8))

        val loaded = OpenContentsCommand(repo).execute(tmp)

        assertEquals("a\nb\nc", loaded.text)
        // Bare CR (legacy Mac Classic) is detected as LF since detect()
        // gates on the presence of CRLF.
        assertEquals(LineEnding.LF, loaded.lineEnding)
    }
}
