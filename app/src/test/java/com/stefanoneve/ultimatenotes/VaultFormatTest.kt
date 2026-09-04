package com.stefanoneve.ultimatenotes

import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.CapStyle
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.FileElement
import com.stefanoneve.ultimatenotes.data.model.FrameElement
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.model.NoteLinkElement
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.data.model.WebLinkElement
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the on-disk shape described by docs/VAULT_FORMAT.md. The macOS app
 * decodes exactly this JSON, so a change here that isn't mirrored in Swift
 * silently breaks sync — these assertions make that break loud instead.
 */
class VaultFormatTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val sample = NoteContent(
        elements = listOf(
            TextElement(id = "t1", x = 1f, y = 2f, text = "ciao", styleId = "title1"),
            ImageElement(id = "i1", fileName = "img.webp", isPdfPage = true, pdfPage = 3),
            NoteLinkElement(id = "n1", targetNoteId = "other"),
            WebLinkElement(id = "w1", url = "https://example.com", title = "Esempio"),
            FileElement(id = "f1", fileName = "a.pdf", displayName = "A", sizeBytes = 42),
        ),
        strokes = listOf(
            InkStroke(
                id = "s1",
                color = 0L,
                points = listOf(StrokePoint(1f, 2f, 0.5f)),
                lineStyle = LineStyle.DASHED,
                animated = true,
            ),
        ),
        connectors = listOf(
            ConnectorElement(id = "c1", fromId = "t1", toId = "i1", endCap = CapStyle.ARROW),
        ),
        frames = listOf(FrameElement(id = "fr1", memberIds = listOf("t1"), decor = "auto")),
        tapes = listOf(TapeElement(id = "tp1")),
        background = CanvasBackground.GRID,
    )

    @Test
    fun `element type discriminator is inline and lowercase`() {
        val text = json.encodeToString(NoteContent.serializer(), sample)
        // The discriminator must sit alongside the fields, not nested, and use
        // exactly these names — the Swift decoder switches on them.
        listOf("text", "image", "notelink", "weblink", "file").forEach {
            assertTrue("manca il discriminatore \"$it\" in: $text", text.contains("\"type\":\"$it\""))
        }
        assertTrue(text.contains("\"id\":\"t1\""))
    }

    @Test
    fun `enums serialize as their uppercase names`() {
        val text = json.encodeToString(NoteContent.serializer(), sample)
        assertTrue(text.contains("\"background\":\"GRID\""))
        assertTrue(text.contains("\"lineStyle\":\"DASHED\""))
        assertTrue(text.contains("\"endCap\":\"ARROW\""))
        assertTrue(text.contains("\"type\":\"PEN\""))
    }

    @Test
    fun `round trip preserves every element kind`() {
        val text = json.encodeToString(NoteContent.serializer(), sample)
        val back = json.decodeFromString(NoteContent.serializer(), text)
        assertEquals(sample, back)
    }

    @Test
    fun `decoding tolerates a minimal note written by another client`() {
        // What a lean writer might produce: only the fields it cares about.
        val minimal = """
            {"elements":[{"type":"text","id":"x","x":0.0,"y":0.0,"text":"hi"}],
             "strokes":[],"connectors":[],"frames":[],"tapes":[],"background":"DOTS"}
        """.trimIndent()
        val content = json.decodeFromString(NoteContent.serializer(), minimal)
        val element = content.elements.single() as TextElement
        assertEquals("hi", element.text)
        // Defaults must fill in the rest rather than failing the decode.
        assertEquals("body", element.styleId)
        assertEquals(600f, element.width)
    }

    @Test
    fun `plainText strips markup so search and previews stay clean`() {
        val content = NoteContent(
            elements = listOf(
                TextElement(text = "# Titolo\n- {c:#FF0000}rosso{/c} e **grassetto**"),
            ),
        )
        assertEquals("Titolo\nrosso e grassetto", content.plainText())
    }
}
