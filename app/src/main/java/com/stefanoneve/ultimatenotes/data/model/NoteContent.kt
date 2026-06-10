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
}

@Serializable
@SerialName("text")
data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    val width: Float = 600f,
    /** Markdown source of the block. */
    val text: String = "",
    /** Id of the paragraph style (see [TextStyleDef]) used as the base style. */
    val styleId: String = "body",
    /** Optional font id from FontManager; null = style/app default. */
    val fontId: String? = null,
    /** Optional ARGB color override. */
    val color: Long? = null,
) : NoteElement

@Serializable
@SerialName("image")
data class ImageElement(
    override val id: String = UUID.randomUUID().toString(),
    override val x: Float = 0f,
    override val y: Float = 0f,
    val width: Float = 400f,
    val height: Float = 400f,
    /** File name inside the note's asset directory. */
    val fileName: String = "",
    /** True when the image is a rendered PDF page (annotatable like any image). */
    val isPdfPage: Boolean = false,
    /** 1-based page number when [isPdfPage]. */
    val pdfPage: Int = 0,
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
enum class CanvasBackground { BLANK, DOTS, GRID, LINES }

/** Full drawable/editable content of a note. */
@Serializable
data class NoteContent(
    val elements: List<NoteElement> = emptyList(),
    val strokes: List<InkStroke> = emptyList(),
    val connectors: List<ConnectorElement> = emptyList(),
    val background: CanvasBackground = CanvasBackground.DOTS,
) {
    /** Plain text extraction used for search and previews. */
    fun plainText(): String =
        elements.filterIsInstance<TextElement>().joinToString("\n") { it.text }
}
