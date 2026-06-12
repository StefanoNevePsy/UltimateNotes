package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.stefanoneve.ultimatenotes.data.model.FrameElement
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import kotlin.math.abs
import kotlin.random.Random

fun frameRect(frame: FrameElement): Rect =
    Rect(frame.x, frame.y, frame.x + frame.width, frame.y + frame.height)

/** True when the point sits on/near the frame border (for selection taps). */
fun hitTestFrameBorder(frame: FrameElement, x: Float, y: Float, tolerance: Float): Boolean {
    val r = frameRect(frame)
    val nearOuter = x >= r.left - tolerance && x <= r.right + tolerance &&
        y >= r.top - tolerance && y <= r.bottom + tolerance
    if (!nearOuter) return false
    val nearLeft = abs(x - r.left) <= tolerance
    val nearRight = abs(x - r.right) <= tolerance
    val nearTop = abs(y - r.top) <= tolerance
    val nearBottom = abs(y - r.bottom) <= tolerance
    return nearLeft || nearRight || nearTop || nearBottom
}

/** Draws every frame; selected one gets a soft halo. */
@Composable
fun FrameLayer(
    frames: List<FrameElement>,
    canvasState: CanvasState,
    selectedFrameId: String?,
    dashPhase: Float,
    theme: com.stefanoneve.ultimatenotes.ui.theme.AppStyle,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            frames.forEach { frame ->
                drawFrame(
                    frame.copy(
                        color = frame.resolvedColor(theme),
                        shape = frame.resolvedShape(theme),
                        lineStyle = frame.resolvedLineStyle(theme),
                    ),
                    frame.id == selectedFrameId,
                    dashPhase,
                    theme,
                )
            }
        }
    }
}

private fun DrawScope.drawFrame(
    frame: FrameElement,
    selected: Boolean,
    dashPhase: Float,
    theme: com.stefanoneve.ultimatenotes.ui.theme.AppStyle,
) {
    val color = Color(frame.color)
    val w = frame.strokeWidth
    val path = framePath(frame)

    // Skin panel under the frame's content ("unisce" the elements above).
    val decor = if (frame.decor == "auto") theme.blockDecor else frame.decor
    if (decor != null) {
        translate(frame.x, frame.y) {
            drawDecorShape(
                decor,
                androidx.compose.ui.geometry.Size(frame.width, frame.height),
                surface = Color(
                    theme.colorScheme.surface.toArgb().toLong() and 0xFFFFFFFFL,
                ),
                onSurface = Color(
                    theme.colorScheme.onSurface.toArgb().toLong() and 0xFFFFFFFFL,
                ),
                primary = Color(
                    theme.colorScheme.primary.toArgb().toLong() and 0xFFFFFFFFL,
                ),
                seed = frame.id.hashCode(),
            )
        }
        if (selected) {
            drawPath(
                path,
                color.copy(alpha = 0.25f),
                style = Stroke(width = w + 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        return
    }

    val effect = dashEffect(
        frame.lineStyle ?: LineStyle.SOLID,
        w,
        frame.animated,
        dashPhase,
    )
    if (frame.filled) {
        drawPath(path, color.copy(alpha = 0.08f))
    }
    if (selected) {
        drawPath(
            path,
            color.copy(alpha = 0.25f),
            style = Stroke(width = w + 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    drawFlavoredPath(path, color, w, effect, theme.strokeFlavor, seed = frame.id.hashCode())
}

private fun framePath(frame: FrameElement): Path {
    val r = frameRect(frame)
    return when (frame.shape ?: FrameShape.ROUNDED) {
        FrameShape.RECT -> Path().apply { addRect(r) }
        FrameShape.ROUNDED -> Path().apply {
            addRoundRect(RoundRect(r, CornerRadius(28f, 28f)))
        }
        FrameShape.ELLIPSE -> Path().apply { addOval(r) }
        FrameShape.SKETCHY -> sketchyRectPath(r, seed = frame.id.hashCode())
    }
}

/** Hand-drawn wobbly rectangle, same spirit as the themed card borders. */
private fun sketchyRectPath(r: Rect, seed: Int): Path {
    val rnd = Random(seed)
    fun j() = rnd.nextFloat() * 8f - 4f
    val c = 22f
    return Path().apply {
        moveTo(r.left + c + j(), r.top + j())
        quadraticBezierTo(r.center.x + j() * 2, r.top + j() * 2, r.right - c + j(), r.top + j())
        quadraticBezierTo(r.right + j(), r.top + j(), r.right + j(), r.top + c + j())
        quadraticBezierTo(r.right + j() * 2, r.center.y + j() * 2, r.right + j(), r.bottom - c + j())
        quadraticBezierTo(r.right + j(), r.bottom + j(), r.right - c + j(), r.bottom + j())
        quadraticBezierTo(r.center.x + j() * 2, r.bottom + j() * 2, r.left + c + j(), r.bottom + j())
        quadraticBezierTo(r.left + j(), r.bottom + j(), r.left + j(), r.bottom - c + j())
        quadraticBezierTo(r.left + j() * 2, r.center.y + j() * 2, r.left + j(), r.top + c + j())
        quadraticBezierTo(r.left + j(), r.top + j(), r.left + c + j(), r.top + j())
    }
}

/** Dashed preview of the lasso loop while the user is drawing it. */
@Composable
fun LassoOverlay(
    points: List<Offset>,
    canvasState: CanvasState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path,
                color,
                style = Stroke(
                    width = 2.5f / canvasState.scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                ),
            )
            drawPath(path, color.copy(alpha = 0.08f))
        }
    }
}

/** Bounding box (world units) of the current lasso selection. */
fun lassoBounds(
    selection: LassoSelection,
    content: com.stefanoneve.ultimatenotes.data.model.NoteContent,
    sizes: Map<String, Size>,
): Rect? {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var found = false
    content.elements.forEach { e ->
        if (e.id in selection.elementIds) {
            val r = elementRect(e, sizes)
            minX = minOf(minX, r.left); minY = minOf(minY, r.top)
            maxX = maxOf(maxX, r.right); maxY = maxOf(maxY, r.bottom)
            found = true
        }
    }
    content.strokes.forEach { s ->
        if (s.id in selection.strokeIds) {
            s.points.forEach { p ->
                minX = minOf(minX, p.x); minY = minOf(minY, p.y)
                maxX = maxOf(maxX, p.x); maxY = maxOf(maxY, p.y)
                found = true
            }
        }
    }
    return if (found) Rect(minX, minY, maxX, maxY) else null
}
