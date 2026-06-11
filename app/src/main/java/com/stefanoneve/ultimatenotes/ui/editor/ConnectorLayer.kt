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

/** Geometry of a connector: endpoints on element edges + bezier control. */
data class ConnectorGeometry(
    val start: Offset,
    val control: Offset,
    val end: Offset,
) {
    fun pointAt(t: Float): Offset {
        val u = 1f - t
        return Offset(
            u * u * start.x + 2 * u * t * control.x + t * t * end.x,
            u * u * start.y + 2 * u * t * control.y + t * t * end.y,
        )
    }

    /** Visual midpoint of the curve — where the drag handle lives. */
    val midpoint: Offset get() = pointAt(0.5f)
}

fun connectorGeometry(
    connector: ConnectorElement,
    content: NoteContent,
    sizes: Map<String, Size>,
): ConnectorGeometry? {
    val from = content.elements.firstOrNull { it.id == connector.fromId } ?: return null
    val to = content.elements.firstOrNull { it.id == connector.toId } ?: return null
    val rectA = elementRect(from, sizes)
    val rectB = elementRect(to, sizes)
    val control = Offset(
        (rectA.center.x + rectB.center.x) / 2f + connector.curveDx,
        (rectA.center.y + rectB.center.y) / 2f + connector.curveDy,
    )
    val start = rectEdgePoint(rectA, control)
    val end = rectEdgePoint(rectB, control)
    return ConnectorGeometry(start, control, end)
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
        for (i in 0..24) {
            val p = geo.pointAt(i / 24f)
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
                drawConnector(connector, geo, connector.id == selectedConnectorId, dashPhase)
            }
        }
    }
}

private fun DrawScope.drawConnector(
    connector: ConnectorElement,
    geo: ConnectorGeometry,
    selected: Boolean,
    dashPhase: Float,
) {
    val color = Color(connector.color)
    val w = connector.width
    val path = Path().apply {
        moveTo(geo.start.x, geo.start.y)
        quadraticBezierTo(geo.control.x, geo.control.y, geo.end.x, geo.end.y)
    }

    val phase = if (connector.animated) -dashPhase else 0f
    val effect = when (connector.lineStyle) {
        LineStyle.SOLID ->
            if (connector.animated) {
                PathEffect.dashPathEffect(floatArrayOf(w * 6f, w * 3f), phase)
            } else null
        LineStyle.DASHED ->
            PathEffect.dashPathEffect(floatArrayOf(w * 4.5f, w * 3.5f), phase)
        LineStyle.DOTTED ->
            PathEffect.dashPathEffect(floatArrayOf(0.1f, w * 3f), phase)
    }

    if (selected) {
        drawPath(
            path,
            color.copy(alpha = 0.25f),
            style = Stroke(width = w + 10f, cap = StrokeCap.Round),
        )
    }
    drawPath(
        path,
        color,
        style = Stroke(width = w, cap = StrokeCap.Round, pathEffect = effect),
    )

    drawCap(connector.startCap, geo.start, geo.control, color, w)
    drawCap(connector.endCap, geo.end, geo.control, color, w)
}

/** Draws an arrowhead or dot at [tip], pointing away from [from]. */
private fun DrawScope.drawCap(
    cap: CapStyle,
    tip: Offset,
    from: Offset,
    color: Color,
    width: Float,
) {
    when (cap) {
        CapStyle.NONE -> Unit
        CapStyle.DOT -> drawCircle(color, radius = width * 1.8f, center = tip)
        CapStyle.ARROW -> {
            val angle = atan2(tip.y - from.y, tip.x - from.x)
            val len = width * 4.5f
            val spread = 0.5f
            val p1 = Offset(
                tip.x - len * cos(angle - spread),
                tip.y - len * sin(angle - spread),
            )
            val p2 = Offset(
                tip.x - len * cos(angle + spread),
                tip.y - len * sin(angle + spread),
            )
            val head = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            drawPath(head, color, style = Fill)
        }
    }
}
