package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TapePattern
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Distance from a point to the tape's center line, for selection taps. */
fun hitTestTape(tapes: List<TapeElement>, x: Float, y: Float, tolerance: Float): String? {
    var best: String? = null
    var bestDist = Float.MAX_VALUE
    tapes.forEach { tape ->
        val d = distanceToSegment(x, y, tape.x1, tape.y1, tape.x2, tape.y2)
        val limit = tape.thickness / 2f + tolerance
        if (d <= limit && d < bestDist) {
            bestDist = d
            best = tape.id
        }
    }
    return best
}

private fun distanceToSegment(
    px: Float, py: Float,
    x1: Float, y1: Float, x2: Float, y2: Float,
): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lenSq = dx * dx + dy * dy
    val t = if (lenSq == 0f) 0f else (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
    return hypot(px - (x1 + t * dx), py - (y1 + t * dy))
}

/** Draws every washi-tape strip (decorative, semi-translucent, patterned). */
@Composable
fun TapeLayer(
    tapes: List<TapeElement>,
    canvasState: CanvasState,
    selectedTapeId: String?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            tapes.forEach { tape ->
                drawTape(tape, tape.id == selectedTapeId)
            }
        }
    }
}

fun DrawScope.drawTape(tape: TapeElement, selected: Boolean) {
    val length = hypot(tape.x2 - tape.x1, tape.y2 - tape.y1)
    if (length < 1f) return
    val angle = Math.toDegrees(
        atan2((tape.y2 - tape.y1).toDouble(), (tape.x2 - tape.x1).toDouble()),
    ).toFloat()
    val h = tape.thickness
    val color = Color(tape.color).copy(alpha = tape.alpha)

    rotate(degrees = angle, pivot = Offset(tape.x1, tape.y1)) {
        val left = tape.x1
        val top = tape.y1 - h / 2f
        val outline = tapeOutline(left, top, length, h, seed = tape.id.hashCode())

        if (selected) {
            drawPath(
                outline,
                Color(tape.color).copy(alpha = 0.35f),
                style = Stroke(width = 10f),
            )
        }
        drawPath(outline, color)
        // Subtle edge lines, like real tape borders.
        drawLine(
            Color.White.copy(alpha = 0.25f * tape.alpha),
            Offset(left, top + 2f),
            Offset(left + length, top + 2f),
            strokeWidth = 1.5f,
        )
        clipPath(outline) {
            drawTapePattern(tape, left, top, length, h)
        }
    }
}

/** Tape body with slightly serrated (torn) short ends. */
private fun tapeOutline(left: Float, top: Float, length: Float, h: Float, seed: Int): Path {
    val rnd = Random(seed)
    val teeth = 5
    val step = h / teeth
    return Path().apply {
        moveTo(left, top)
        lineTo(left + length, top)
        // Right torn edge.
        var y = top
        for (i in 0 until teeth) {
            val jag = if (i % 2 == 0) 4f + rnd.nextFloat() * 3f else -(2f + rnd.nextFloat() * 3f)
            y += step
            lineTo(left + length + jag, y)
        }
        lineTo(left, top + h)
        // Left torn edge.
        y = top + h
        for (i in 0 until teeth) {
            val jag = if (i % 2 == 0) -(4f + rnd.nextFloat() * 3f) else 2f + rnd.nextFloat() * 3f
            y -= step
            lineTo(left + jag, y)
        }
        close()
    }
}

private fun DrawScope.drawTapePattern(
    tape: TapeElement,
    left: Float,
    top: Float,
    length: Float,
    h: Float,
) {
    val deco = decoColorFor(tape)
    when (tape.pattern) {
        TapePattern.SOLID -> Unit
        TapePattern.STRIPES -> {
            // Diagonal candy stripes.
            val step = h * 0.9f
            var x = left - h
            while (x < left + length + h) {
                drawLine(
                    deco,
                    Offset(x, top + h),
                    Offset(x + h, top),
                    strokeWidth = h * 0.28f,
                )
                x += step
            }
        }
        TapePattern.DOTS -> {
            val step = h * 0.75f
            var x = left + step / 2f
            var even = true
            while (x < left + length) {
                val cy = if (even) top + h * 0.3f else top + h * 0.7f
                drawCircle(deco, radius = h * 0.12f, center = Offset(x, cy))
                even = !even
                x += step / 2f
            }
        }
        TapePattern.ZIGZAG -> {
            val step = h * 0.6f
            val path = Path()
            var x = left
            var up = true
            path.moveTo(x, top + h * 0.7f)
            while (x < left + length) {
                x += step
                path.lineTo(x, if (up) top + h * 0.3f else top + h * 0.7f)
                up = !up
            }
            drawPath(path, deco, style = Stroke(width = h * 0.12f))
        }
        TapePattern.GRID -> {
            val step = h / 3f
            var x = left
            while (x < left + length) {
                drawLine(deco, Offset(x, top), Offset(x, top + h), strokeWidth = 1.5f)
                x += step
            }
            var y = top
            while (y < top + h) {
                drawLine(deco, Offset(left, y), Offset(left + length, y), strokeWidth = 1.5f)
                y += step
            }
        }
    }
}

/** Pattern ink: white on dark tape, translucent black on light tape. */
private fun decoColorFor(tape: TapeElement): Color {
    val c = tape.color
    val r = (c shr 16) and 0xFF
    val g = (c shr 8) and 0xFF
    val b = c and 0xFF
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return if (luminance < 140) Color.White.copy(alpha = 0.45f)
    else Color.Black.copy(alpha = 0.22f)
}
