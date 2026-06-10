package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.StrokeType

/** Pan/zoom state of the infinite canvas. World → screen: p * scale + offset. */
class CanvasState {
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableFloatStateOf(1f)

    fun toWorld(screen: Offset): Offset = (screen - offset) / scale

    fun applyZoom(centroid: Offset, zoom: Float, pan: Offset) {
        val newScale = (scale * zoom).coerceIn(0.1f, 8f)
        val effectiveZoom = newScale / scale
        offset = centroid - (centroid - offset) * effectiveZoom + pan
        scale = newScale
    }
}

/** Draws background pattern, committed strokes and the in-progress stroke. */
@Composable
fun InkLayer(
    strokes: List<InkStroke>,
    activeStroke: InkStroke?,
    background: CanvasBackground,
    canvasState: CanvasState,
    patternColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawBackgroundPattern(background, canvasState, patternColor)
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            strokes.forEach { drawInkStroke(it) }
            activeStroke?.let { drawInkStroke(it) }
        }
    }
}

private fun DrawScope.drawInkStroke(stroke: InkStroke) {
    val points = stroke.points
    if (points.isEmpty()) return
    val color = Color(stroke.color)
    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = stroke.width / 2f,
            center = Offset(points[0].x, points[0].y),
        )
        return
    }
    val isHighlighter = stroke.type == StrokeType.HIGHLIGHTER
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val pressure = if (isHighlighter) 1f else (a.p + b.p) / 2f
        drawLine(
            color = color,
            start = Offset(a.x, a.y),
            end = Offset(b.x, b.y),
            strokeWidth = stroke.width * (if (isHighlighter) 1f else 0.4f + 0.8f * pressure),
            cap = if (isHighlighter) StrokeCap.Square else StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBackgroundPattern(
    background: CanvasBackground,
    state: CanvasState,
    color: Color,
) {
    if (background == CanvasBackground.BLANK) return
    // Keep apparent density stable across zoom levels.
    var spacing = 56f * state.scale
    while (spacing < 28f) spacing *= 2f
    while (spacing > 112f) spacing /= 2f

    val startX = state.offset.x % spacing
    val startY = state.offset.y % spacing

    when (background) {
        CanvasBackground.DOTS -> {
            var y = startY
            while (y < size.height) {
                var x = startX
                while (x < size.width) {
                    drawCircle(color, radius = 1.5f, center = Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }
        CanvasBackground.GRID -> {
            var x = startX
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += spacing
            }
            var y = startY
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += spacing
            }
        }
        CanvasBackground.LINES -> {
            var y = startY
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += spacing
            }
        }
        CanvasBackground.BLANK -> Unit
    }
}
