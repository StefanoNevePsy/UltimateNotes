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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.stefanoneve.ultimatenotes.data.model.CanvasBackground
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import com.stefanoneve.ultimatenotes.data.model.StrokeType
import kotlin.math.hypot
import kotlin.random.Random

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

/**
 * Draws the background pattern only. Kept separate from [StrokesLayer] so
 * frames (decor panels, fills) can render between the paper and the ink:
 * strokes always stay visible, even on top of a frame's themed skin.
 */
@Composable
fun BackgroundLayer(
    background: CanvasBackground,
    canvasState: CanvasState,
    patternColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawBackgroundPattern(background, canvasState, patternColor)
    }
}

/** Draws committed strokes and the in-progress stroke. */
@Composable
fun StrokesLayer(
    strokes: List<InkStroke>,
    activeStroke: InkStroke?,
    canvasState: CanvasState,
    theme: com.stefanoneve.ultimatenotes.ui.theme.AppStyle,
    selectedStrokeId: String? = null,
    dashPhase: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            strokes.forEach {
                drawInkStroke(it, theme, it.id == selectedStrokeId, dashPhase)
            }
            activeStroke?.let { drawInkStroke(it, theme, false, dashPhase) }
        }
    }
}

/** Smooth path through the sampled points (Catmull-Rom, like connectors). */
private fun strokePath(points: List<StrokePoint>): Path {
    val pts = catmullRom(points.map { Offset(it.x, it.y) })
    return Path().apply {
        moveTo(pts.first().x, pts.first().y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
    }
}

private fun DrawScope.drawInkStroke(
    stroke: InkStroke,
    theme: com.stefanoneve.ultimatenotes.ui.theme.AppStyle,
    selected: Boolean,
    dashPhase: Float,
) {
    val points = stroke.points
    if (points.isEmpty()) return
    val color = Color(stroke.resolvedColor(theme))
    if (points.size == 1) {
        drawCircle(color, stroke.width / 2f, Offset(points[0].x, points[0].y))
        return
    }
    val isHighlighter = stroke.type == StrokeType.HIGHLIGHTER
    val lineStyle = stroke.lineStyle ?: LineStyle.SOLID
    val flavor = if (isHighlighter) "clean" else theme.strokeFlavor

    if (selected) {
        drawPath(
            strokePath(points),
            color.copy(alpha = 0.28f),
            style = Stroke(
                width = stroke.width + 14f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }

    // A plain solid pen on a plain theme keeps the per-segment rendering: it
    // is the only path that can modulate width with S Pen pressure. Anything
    // decorative (dashes, animation, a textured theme, the highlighter) is
    // drawn as one smooth path so the style reads correctly and overlapping
    // parts of the same stroke don't stack into a darker blob.
    val plainPen = !isHighlighter && lineStyle == LineStyle.SOLID &&
        !stroke.animated && flavor == "clean"
    if (plainPen) {
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val pressure = (a.p + b.p) / 2f
            drawLine(
                color = color,
                start = Offset(a.x, a.y),
                end = Offset(b.x, b.y),
                strokeWidth = stroke.width * (0.4f + 0.8f * pressure),
                cap = StrokeCap.Round,
            )
        }
        return
    }

    val path = strokePath(points)
    val effect = dashEffect(lineStyle, stroke.width, stroke.animated, dashPhase)
    if (isHighlighter) {
        drawPath(
            path,
            color,
            style = Stroke(
                width = stroke.width,
                cap = StrokeCap.Square,
                join = StrokeJoin.Round,
                pathEffect = effect,
            ),
        )
    } else {
        drawFlavoredPath(
            path, color, stroke.width, effect, flavor, seed = stroke.id.hashCode(),
        )
    }
}

/** Id of the stroke passing near [world], if any (segment distance). */
fun hitTestStroke(
    strokes: List<InkStroke>,
    world: Offset,
    tolerance: Float,
): String? {
    var best: String? = null
    var bestDist = tolerance
    strokes.forEach { stroke ->
        val pts = stroke.points
        val reach = tolerance + stroke.width / 2f
        if (pts.size == 1) {
            val d = hypot(pts[0].x - world.x, pts[0].y - world.y)
            if (d < reach && d < bestDist) {
                bestDist = d
                best = stroke.id
            }
            return@forEach
        }
        for (i in 1 until pts.size) {
            val a = pts[i - 1]
            val b = pts[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val lenSq = dx * dx + dy * dy
            val t =
                if (lenSq == 0f) 0f
                else (((world.x - a.x) * dx + (world.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
            val d = hypot(world.x - (a.x + t * dx), world.y - (a.y + t * dy))
            if (d < reach && d < bestDist) {
                bestDist = d
                best = stroke.id
            }
        }
    }
    return best
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
        CanvasBackground.PAPER -> {
            // Paper grain: a deterministic speckle field anchored to world
            // coordinates so it pans naturally with the canvas.
            val cell = spacing / 2f
            val startXp = state.offset.x % cell
            val startYp = state.offset.y % cell
            var y = startYp - cell
            var row = 0
            while (y < size.height + cell) {
                var x = startXp - cell
                var col = 0
                while (x < size.width + cell) {
                    val worldX = ((x - state.offset.x) / cell).toInt()
                    val worldY = ((y - state.offset.y) / cell).toInt()
                    val rnd = Random(worldX * 92821 + worldY * 31337)
                    if (rnd.nextFloat() < 0.55f) {
                        drawCircle(
                            color.copy(alpha = 0.25f + rnd.nextFloat() * 0.3f),
                            radius = 0.8f + rnd.nextFloat() * 1.4f,
                            center = Offset(
                                x + rnd.nextFloat() * cell,
                                y + rnd.nextFloat() * cell,
                            ),
                        )
                    }
                    x += cell
                    col++
                }
                y += cell
                row++
            }
        }
        CanvasBackground.SCANLINES -> {
            // CRT scanlines: tight horizontal lines, screen-fixed.
            val gap = 7f
            var y = state.offset.y % gap
            while (y < size.height) {
                drawLine(
                    color.copy(alpha = 0.35f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += gap
            }
        }
        CanvasBackground.BLANK -> Unit
    }
}
