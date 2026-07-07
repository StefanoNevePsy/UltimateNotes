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
    /**
     * Optional sticky-note background; null = transparent,
     * [STICKY_AUTO] = follows the active theme's sticky color.
     */
    val bgColor: Long? = null,
    /** Optional block font size override (sp); null = paragraph style size. */
    val fontSize: Float? = null,
    /**
     * Block "skin": null = none, "auto" = the theme's signature decor
     * (parchment, OS window, glass…), or an explicit decor id.
     */
    val decor: String? = null,
) : NoteElement

/** Sentinel for "themed" sticky background (an impossible real color). */
const val STICKY_AUTO: Long = 1L

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
    /** 0 = "auto": follows the active theme's accent. */
    val color: Long = 0L,
    val width: Float = 3.5f,
    /** null = "auto": follows the active theme's connector style. */
    val lineStyle: LineStyle? = null,
    /** Marching-dashes animation along the line. */
    val animated: Boolean = false,
    val startCap: CapStyle = CapStyle.NONE,
    val endCap: CapStyle = CapStyle.ARROW,
    /** Offset of the bezier control point from the segment midpoint. */
    val curveDx: Float = 0f,
    val curveDy: Float = 0f,
    /**
     * Optional intermediate nodes: the line becomes a smooth spline through
     * all of them (double, triple… curves computed automatically).
     */
    val nodes: List<StrokePoint> = emptyList(),
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
    /** null = "auto": follows the active theme. */
    val shape: FrameShape? = null,
    val lineStyle: LineStyle? = null,
    val animated: Boolean = false,
    /** 0 = "auto": follows the active theme's accent. */
    val color: Long = 0L,
    val strokeWidth: Float = 3f,
    /** Fill the frame with a translucent tint of [color]. */
    val filled: Boolean = false,
    val label: String = "",
    /**
     * Optional skin drawn under the frame's content (parchment, OS window…):
     * null = plain outline, "auto" = the theme's signature decor.
     */
    val decor: String? = null,
    /**
     * When true the frame grows to always contain the elements inside it
     * (its stored size is the manual minimum; widen it freely beyond that).
     */
    val autoFit: Boolean = true,
    /**
     * Elements explicitly captured by this frame. Auto-fit follows these
     * robustly (they never drop out); empty = fall back to geometric
     * containment for frames drawn on the canvas.
     */
    val memberIds: List<String> = emptyList(),
)

@Serializable
enum class TapePattern { SOLID, STRIPES, DOTS, ZIGZAG, GRID }

/**
 * A straight strip of washi tape: decorative, semi-translucent, patterned.
 */
@Serializable
data class TapeElement(
    val id: String = UUID.randomUUID().toString(),
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 100f,
    val y2: Float = 0f,
    /** Strip thickness in canvas units. */
    val thickness: Float = 36f,
    /** 0 = "auto": follows the active theme's tape colors. */
    val color: Long = 0L,
    /** null = "auto": follows the active theme's tape pattern. */
    val pattern: TapePattern? = null,
    val alpha: Float = 0.85f,
)

@Serializable
enum class CanvasBackground { BLANK, DOTS, GRID, LINES, PAPER, SCANLINES }

/** Full drawable/editable content of a note. */
@Serializable
data class NoteContent(
    val elements: List<NoteElement> = emptyList(),
    val strokes: List<InkStroke> = emptyList(),
    val connectors: List<ConnectorElement> = emptyList(),
    val frames: List<FrameElement> = emptyList(),
    val tapes: List<TapeElement> = emptyList(),
    val background: CanvasBackground = CanvasBackground.DOTS,
) {
    /** Plain text extraction used for search and previews. */
    fun plainText(): String = elements.mapNotNull { e ->
        when (e) {
            is TextElement -> stripMarkup(e.text)
            is WebLinkElement -> "${e.title} ${e.url}".trim()
            is FileElement -> e.displayName
            else -> null
        }
    }.filter { it.isNotBlank() }.joinToString("\n")
}

/** Inline tags like {c:#ff0000}…{/c}, {f:serif}…{/f}, {s:24}…{/s}. */
private val inlineTagRegex = Regex("""\{[cfs]:[^{}]*\}|\{/[cfs]\}""")
private val lineMarkerRegex =
    Regex("""^\s*(#{1,6}\s+|>\s+|[-*+]\s+|\d{1,3}[.)]\s+|\[[ xX]\]\s*)+""")

/**
 * Removes markdown markers and inline styling tags so search and the home
 * previews see (and match) only the words the user actually wrote.
 */
fun stripMarkup(text: String): String = text
    .replace(inlineTagRegex, "")
    .lineSequence()
    .joinToString("\n") { line ->
        line.replace(lineMarkerRegex, "")
            .replace("**", "")
            .replace("~~", "")
            .replace("`", "")
    }
