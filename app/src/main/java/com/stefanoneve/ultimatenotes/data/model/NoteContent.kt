package com.stefanoneve.ultimatenotes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/** A single sampled point of an ink stroke, in canvas (world) coordinates. */
@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    /** Stylus pressure in 0..1, used to modulate stroke width. */
    val p: Float = 1f,
)

@Serializable
enum class StrokeType { PEN, HIGHLIGHTER }

@Serializable
data class InkStroke(
    val id: String = UUID.randomUUID().toString(),
    val type: StrokeType = StrokeType.PEN,
    /** ARGB color packed in a Long (0xAARRGGBB). */
    val color: Long = 0xFF1A1A1A,
    /** Base stroke width in canvas units. */
    val width: Float = 4f,
    val points: List<StrokePoint> = emptyList(),
)

/** Anything placed freely on the infinite canvas. */
@Serializable
sealed interface NoteElement {
    val id: String
    val x: Float
    val y: Float

    /** Elements sharing a non-null groupId move together. */
    val groupId: String?
}

@Serializable
@SerialName("text")
data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val groupId: String? = null,
    val width: Float = 600f,
    /** Vector zoom factor of the whole block (text stays crisp). */
    val scale: Float = 1f,
    /** Markdown source of the block (supports inline {c:#hex} / {f:id} tags). */
    val text: String = "",
    /** Id of the paragraph style (see [TextStyleDef]) used as the base style. */
    val styleId: String = "body",
    /** Optional font id from FontManager; null follows the active theme. */
    val fontId: String? = null,
    /** Optional ARGB color override; null adapts to the active theme. */
    val color: Long? = null,
    /** Optional sticky-note background color; null = transparent. */
    val bgColor: Long? = null,
) : NoteElement

@Serializable
@SerialName("image")
data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val groupId: String? = null,
    val width: Float = 400f,
    val height: Float = 400f,
    /** File name inside the note's asset directory. */
    val fileName: String = "",
    /** True when the image is a rendered PDF page (annotatable like any image). */
    val isPdfPage: Boolean = false,
    /** 1-based page number when [isPdfPage]. */
    val pdfPage: Int = 0,
) : NoteElement

/** A live link to another note, rendered as a preview card on the canvas. */
@Serializable
@SerialName("notelink")
data class NoteLinkElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val groupId: String? = null,
    val width: Float = 420f,
    val scale: Float = 1f,
    val targetNoteId: String = "",
) : NoteElement

/** An embedded web link, rendered as a card that opens the browser. */
@Serializable
@SerialName("weblink")
data class WebLinkElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val groupId: String? = null,
    val width: Float = 420f,
    val scale: Float = 1f,
    val url: String = "",
    /** Page title, fetched best-effort when the link is added. */
    val title: String = "",
) : NoteElement

/** A file attached to the note (any type), openable with the system viewer. */
@Serializable
@SerialName("file")
data class FileElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val groupId: String? = null,
    val width: Float = 380f,
    val scale: Float = 1f,
    /** File name inside the note's asset directory. */
    val fileName: String = "",
    /** Original display name. */
    val displayName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
) : NoteElement

@Serializable
enum class LineStyle { SOLID, DASHED, DOTTED }

@Serializable
enum class CapStyle { NONE, ARROW, DOT }

/**
 * Kinopio-style connector between two elements: a quadratic bezier whose
 * control point can be dragged, with configurable stroke and end caps.
 */
@Serializable
data class ConnectorElement(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val color: Long = 0xFF9A8FE5,
    val width: Float = 3.5f,
    val lineStyle: LineStyle = LineStyle.SOLID,
    /** Marching-dashes animation along the line. */
    val animated: Boolean = false,
    val startCap: CapStyle = CapStyle.NONE,
    val endCap: CapStyle = CapStyle.ARROW,
    /** Offset of the bezier control point from the segment midpoint. */
    val curveDx: Float = 0f,
    val curveDy: Float = 0f,
)

@Serializable
enum class FrameShape { RECT, ROUNDED, ELLIPSE, SKETCHY }

/**
 * A decorative frame that visually groups a region of the canvas. Moving a
 * frame drags along every element currently inside it.
 */
@Serializable
data class FrameElement(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 400f,
    val height: Float = 300f,
    val shape: FrameShape = FrameShape.ROUNDED,
    val lineStyle: LineStyle = LineStyle.SOLID,
    val animated: Boolean = false,
    val color: Long = 0xFF9A8FE5,
    val strokeWidth: Float = 3f,
    /** Fill the frame with a translucent tint of [color]. */
    val filled: Boolean = false,
    val label: String = "",
)

@Serializable
enum class CanvasBackground { BLANK, DOTS, GRID, LINES }

/** Full drawable/editable content of a note. */
@Serializable
data class NoteContent(
    val elements: List<NoteElement> = emptyList(),
    val strokes: List<InkStroke> = emptyList(),
    val connectors: List<ConnectorElement> = emptyList(),
    val frames: List<FrameElement> = emptyList(),
    val background: CanvasBackground = CanvasBackground.DOTS,
) {
    /** Plain text extraction used for search and previews. */
    fun plainText(): String = elements.mapNotNull { e ->
        when (e) {
            is TextElement -> e.text
            is WebLinkElement -> "${e.title} ${e.url}".trim()
            is FileElement -> e.displayName
            else -> null
        }
    }.filter { it.isNotBlank() }.joinToString("\n")
}
