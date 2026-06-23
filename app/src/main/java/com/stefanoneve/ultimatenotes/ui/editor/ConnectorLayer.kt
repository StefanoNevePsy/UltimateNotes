package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.stefanoneve.ultimatenotes.data.model.CapStyle
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.model.NoteElement
import com.stefanoneve.ultimatenotes.data.model.TextElement
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** World-space bounding box of an element, using measured sizes when known. */
fun elementRect(element: NoteElement, sizes: Map<String, Size>): Rect {
    val measured = sizes[element.id]
    val (w, h) = when (element) {
        is ImageElement -> element.width to element.height
        is TextElement ->
            element.width * element.scale to
                ((measured?.height ?: 80f) * element.scale)
        is com.stefanoneve.ultimatenotes.data.model.NoteLinkElement ->
            element.width * element.scale to
                ((measured?.height ?: 120f) * element.scale)
        is com.stefanoneve.ultimatenotes.data.model.WebLinkElement ->
            element.width * element.scale to
                ((measured?.height ?: 110f) * element.scale)
        is com.stefanoneve.ultimatenotes.data.model.FileElement ->
            element.width * element.scale to
                ((measured?.height ?: 100f) * element.scale)
    }
    return Rect(element.x, element.y, element.x + w, element.y + h)
}

/**
 * Geometry of a connector as a sampled polyline: with no nodes it is a quad
 * bezier; with N intermediate nodes a Catmull-Rom spline through all of them.
 */
data class ConnectorGeometry(val samples: List<Offset>) {
    val start: Offset get() = samples.first()
    val end: Offset get() = samples.last()

    fun pointAt(t: Float): Offset {
        val f = (t.coerceIn(0f, 1f) * (samples.size - 1))
        val i = f.toInt().coerceAtMost(samples.size - 2)
        val frac = f - i
        val a = samples[i]
        val b = samples[i + 1]
        return Offset(a.x + (b.x - a.x) * frac, a.y + (b.y - a.y) * frac)
    }

    /** Visual midpoint of the curve — where the drag handle lives. */
    val midpoint: Offset get() = pointAt(0.5f)

    /** Reference points for the arrow-head tangents. */
    val afterStart: Offset get() = samples[1.coerceAtMost(samples.size - 1)]
    val beforeEnd: Offset get() = samples[(samples.size - 2).coerceAtLeast(0)]
}

/** Bounding rect of a connector endpoint: an element or a frame. */
fun anchorRect(id: String, content: NoteContent, sizes: Map<String, Size>): Rect? {
    content.elements.firstOrNull { it.id == id }?.let { return elementRect(it, sizes) }
    content.frames.firstOrNull { it.id == id }?.let {
        return effectiveFrameRect(it, content, sizes)
    }
    return null
}

fun connectorGeometry(
    connector: ConnectorElement,
    content: NoteContent,
    sizes: Map<String, Size>,
): ConnectorGeometry? {
    val rectA = anchorRect(connector.fromId, content, sizes) ?: return null
    val rectB = anchorRect(connector.toId, content, sizes) ?: return null
    if (connector.nodes.isEmpty()) {
        val control = Offset(
            (rectA.center.x + rectB.center.x) / 2f + connector.curveDx,
            (rectA.center.y + rectB.center.y) / 2f + connector.curveDy,
        )
        val start = rectEdgePoint(rectA, control)
        val end = rectEdgePoint(rectB, control)
        val samples = (0..32).map { i ->
            val t = i / 32f
            val u = 1f - t
            Offset(
                u * u * start.x + 2 * u * t * control.x + t * t * end.x,
                u * u * start.y + 2 * u * t * control.y + t * t * end.y,
            )
        }
        return ConnectorGeometry(samples)
    }
    val nodePts = connector.nodes.map { Offset(it.x, it.y) }
    val start = rectEdgePoint(rectA, nodePts.first())
    val end = rectEdgePoint(rectB, nodePts.last())
    return ConnectorGeometry(catmullRom(listOf(start) + nodePts + listOf(end)))
}

/** Point where the segment center→target crosses the rect border. */
private fun rectEdgePoint(rect: Rect, target: Offset): Offset {
    val c = rect.center
    val dx = target.x - c.x
    val dy = target.y - c.y
    if (dx == 0f && dy == 0f) return c
    val halfW = rect.width / 2f
    val halfH = rect.height / 2f
    val scaleX = if (dx != 0f) halfW / kotlin.math.abs(dx) else Float.MAX_VALUE
    val scaleY = if (dy != 0f) halfH / kotlin.math.abs(dy) else Float.MAX_VALUE
    val t = minOf(scaleX, scaleY, 1f)
    return Offset(c.x + dx * t, c.y + dy * t)
}

/** Point along the polyline at [distance] from the start or the end. */
fun pointAlong(samples: List<Offset>, fromEnd: Boolean, distance: Float): Offset {
    val pts = if (fromEnd) samples.asReversed() else samples
    var remaining = distance
    for (i in 0 until pts.size - 1) {
        val seg = hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
        if (seg >= remaining && seg > 0f) {
            val t = remaining / seg
            return Offset(
                pts[i].x + (pts[i + 1].x - pts[i].x) * t,
                pts[i].y + (pts[i + 1].y - pts[i].y) * t,
            )
        }
        remaining -= seg
    }
    return pts.last()
}

/** Cuts [trimStart]/[trimEnd] arc-length off the polyline ends. */
fun trimPolyline(samples: List<Offset>, trimStart: Float, trimEnd: Float): List<Offset> {
    if (samples.size < 2 || (trimStart <= 0f && trimEnd <= 0f)) return samples
    val cum = FloatArray(samples.size)
    for (i in 1 until samples.size) {
        cum[i] = cum[i - 1] +
            hypot(samples[i].x - samples[i - 1].x, samples[i].y - samples[i - 1].y)
    }
    val total = cum.last()
    if (trimStart + trimEnd >= total) {
        // Endpoints too close: collapse to the middle point.
        val mid = pointAlong(samples, false, total / 2f)
        return listOf(mid, mid)
    }
    val startD = trimStart.coerceAtLeast(0f)
    val endD = total - trimEnd.coerceAtLeast(0f)
    val out = mutableListOf(pointAlong(samples, false, startD))
    for (i in samples.indices) {
        if (cum[i] > startD && cum[i] < endD) out += samples[i]
    }
    out += pointAlong(samples, false, endD)
    return out
}

/** Returns the id of the connector whose curve passes near [world], if any. */
fun hitTestConnector(
    content: NoteContent,
    sizes: Map<String, Size>,
    world: Offset,
    tolerance: Float,
): String? {
    var best: String? = null
    var bestDist = tolerance
    content.connectors.forEach { connector ->
        val geo = connectorGeometry(connector, content, sizes) ?: return@forEach
        geo.samples.forEach { p ->
            val d = hypot(p.x - world.x, p.y - world.y)
            if (d < bestDist) {
                bestDist = d
                best = connector.id
            }
        }
    }
    return best
}

/** Draws every connector (and the selection halo) in world space. */
@Composable
fun ConnectorLayer(
    content: NoteContent,
    canvasState: CanvasState,
    selectedConnectorId: String?,
    elementSizes: Map<String, Size>,
    dashPhase: Float,
    theme: com.stefanoneve.ultimatenotes.ui.theme.AppStyle,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        withTransform({
            translate(canvasState.offset.x, canvasState.offset.y)
            scale(canvasState.scale, canvasState.scale, pivot = Offset.Zero)
        }) {
            content.connectors.forEach { connector ->
                val geo = connectorGeometry(connector, content, elementSizes)
                    ?: return@forEach
                drawConnector(
                    connector.copy(
                        color = connector.resolvedColor(theme),
                        lineStyle = connector.resolvedLineStyle(theme),
                    ),
                    geo,
                    connector.id == selectedConnectorId,
                    dashPhase,
                    theme.strokeFlavor,
                )
            }
        }
    }
}

/**
 * Dash intervals for a stroke width; the animation phase advances by exactly
 * one interval cycle per loop so the marching motion never visibly jumps.
 */
fun dashIntervals(lineStyle: LineStyle, w: Float, animatedSolid: Boolean): FloatArray? =
    when (lineStyle) {
        LineStyle.SOLID ->
            if (animatedSolid) floatArrayOf(w * 6f, w * 3f) else null
        LineStyle.DASHED -> floatArrayOf(w * 4.5f, w * 3.5f)
        LineStyle.DOTTED -> floatArrayOf(0.1f, w * 3f)
    }

fun dashEffect(
    lineStyle: LineStyle,
    w: Float,
    animated: Boolean,
    phase01: Float,
): PathEffect? {
    val intervals = dashIntervals(lineStyle, w, animated) ?: return null
    val cycle = intervals.sum()
    val phase = if (animated) -phase01 * cycle else 0f
    return PathEffect.dashPathEffect(intervals, phase)
}

private fun DrawScope.drawConnector(
    connector: ConnectorElement,
    geo: ConnectorGeometry,
    selected: Boolean,
    dashPhase: Float,
    flavor: String,
) {
    val color = Color(connector.color)
    val w = connector.width
    val headLen = capLength(w)

    // The line stops short under each cap, so dashes/glow never poke past
    // the arrow tip.
    val body = trimPolyline(
        geo.samples,
        if (connector.startCap != CapStyle.NONE) headLen * 0.55f else 0f,
        if (connector.endCap != CapStyle.NONE) headLen * 0.55f else 0f,
    )
    val path = Path().apply {
        moveTo(body.first().x, body.first().y)
        body.drop(1).forEach { lineTo(it.x, it.y) }
    }

    val effect = dashEffect(
        connector.lineStyle ?: LineStyle.SOLID,
        w,
        connector.animated,
        dashPhase,
    )

    if (selected) {
        drawPath(
            path,
            color.copy(alpha = 0.25f),
            style = Stroke(width = w + 10f, cap = StrokeCap.Round),
        )
    }
    drawFlavoredPath(path, color, w, effect, flavor, seed = connector.id.hashCode())

    // Tangents sampled a full head-length back along the arc: stable
    // direction even on tight multi-node curves.
    val startFrom = pointAlong(geo.samples, false, headLen)
    val endFrom = pointAlong(geo.samples, true, headLen)
    drawThemedCap(
        connector.startCap, geo.start, startFrom, color, w, flavor,
        seed = connector.id.hashCode(),
    )
    drawThemedCap(
        connector.endCap, geo.end, endFrom, color, w, flavor,
        seed = connector.id.hashCode() + 1,
    )
}

fun capLength(width: Float): Float = (width * 4.5f).coerceAtLeast(14f)

/**
 * Theme-flavored line ending, matching the stroke texture: open china "V",
 * grainy chalk, blocky pixel with drop shadow, glowing neon, clean fill.
 */
fun DrawScope.drawThemedCap(
    cap: CapStyle,
    tip: Offset,
    from: Offset,
    color: Color,
    width: Float,
    flavor: String,
    seed: Int,
) {
    if (cap == CapStyle.NONE) return
    val angle = atan2(tip.y - from.y, tip.x - from.x)
    val len = capLength(width)
    val rnd = Random(seed)

    if (cap == CapStyle.DOT) {
        val r = (width * 1.8f).coerceAtLeast(6f)
        when (flavor) {
            "ink" -> {
                drawCircle(color, r, tip)
                drawCircle(
                    color.copy(alpha = 0.45f), r * 0.7f,
                    Offset(tip.x + rnd.nextFloat() * 2f - 1f, tip.y + 1.5f),
                )
            }
            "chalk" -> repeat(3) {
                drawCircle(
                    color.copy(alpha = 0.38f),
                    r * (0.8f + rnd.nextFloat() * 0.4f),
                    Offset(
                        tip.x + rnd.nextFloat() * width - width / 2f,
                        tip.y + rnd.nextFloat() * width - width / 2f,
                    ),
                )
            }
            "pixel" -> {
                val side = r * 1.8f
                drawRect(
                    Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(
                        tip.x - side / 2f + width * 0.9f,
                        tip.y - side / 2f + width * 0.9f,
                    ),
                    size = androidx.compose.ui.geometry.Size(side, side),
                )
                drawRect(
                    color,
                    topLeft = Offset(tip.x - side / 2f, tip.y - side / 2f),
                    size = androidx.compose.ui.geometry.Size(side, side),
                )
            }
            "neon" -> {
                drawCircle(color.copy(alpha = 0.22f), r * 2.4f, tip)
                drawCircle(color, r, tip)
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.45f, tip)
            }
            else -> drawCircle(color, r, tip)
        }
        return
    }

    // ARROW
    val spread = 0.46f
    fun wing(side: Float, jitter: Float = 0f) = Offset(
        tip.x - len * cos(angle - spread * side) + jitter,
        tip.y - len * sin(angle - spread * side) + jitter,
    )
    val p1 = wing(1f)
    val p2 = wing(-1f)
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    when (flavor) {
        "ink" -> {
            // Open quill "V": two strokes, re-inked like the line.
            val stroke = Stroke(width, cap = StrokeCap.Round)
            drawLine(color, p1, tip, width, StrokeCap.Round)
            drawLine(color, p2, tip, width, StrokeCap.Round)
            val o = Offset(rnd.nextFloat() * width * 0.6f, width * 0.4f)
            drawLine(
                color.copy(alpha = 0.45f),
                p1 + o, tip + o, width * 0.5f, StrokeCap.Round,
            )
            drawLine(
                color.copy(alpha = 0.45f),
                p2 + o, tip + o, width * 0.5f, StrokeCap.Round,
            )
        }
        "chalk" -> repeat(3) {
            val o = Offset(
                rnd.nextFloat() * width - width / 2f,
                rnd.nextFloat() * width - width / 2f,
            )
            drawLine(
                color.copy(alpha = 0.38f),
                p1 + o, tip + o,
                width * (0.7f + rnd.nextFloat() * 0.5f), StrokeCap.Round,
            )
            drawLine(
                color.copy(alpha = 0.38f),
                p2 + o, tip + o,
                width * (0.7f + rnd.nextFloat() * 0.5f), StrokeCap.Round,
            )
        }
        "pixel" -> {
            val shadow = Offset(width * 0.9f, width * 0.9f)
            val shadowHead = Path().apply {
                moveTo(tip.x + shadow.x, tip.y + shadow.y)
                lineTo(p1.x + shadow.x, p1.y + shadow.y)
                lineTo(p2.x + shadow.x, p2.y + shadow.y)
                close()
            }
            drawPath(shadowHead, Color.Black.copy(alpha = 0.3f), style = Fill)
            drawPath(head, color, style = Fill)
        }
        "neon" -> {
            drawPath(
                head,
                color.copy(alpha = 0.25f),
                style = Stroke(width * 3f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
            drawPath(head, color, style = Fill)
            // Bright core.
            val coreLen = len * 0.45f
            val c1 = Offset(
                tip.x - coreLen * cos(angle - spread),
                tip.y - coreLen * sin(angle - spread),
            )
            val c2 = Offset(
                tip.x - coreLen * cos(angle + spread),
                tip.y - coreLen * sin(angle + spread),
            )
            drawLine(Color.White.copy(alpha = 0.55f), c1, tip, width * 0.38f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.55f), c2, tip, width * 0.38f, StrokeCap.Round)
        }
        else -> drawPath(head, color, style = Fill)
    }
}
