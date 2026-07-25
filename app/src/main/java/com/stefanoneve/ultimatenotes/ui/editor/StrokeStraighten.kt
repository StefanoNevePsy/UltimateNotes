package com.stefanoneve.ultimatenotes.ui.editor

import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.StrokePoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** How straight a stroke must be to be cleaned up: max sag / chord length. */
private const val STRAIGHT_TOLERANCE = 0.045f

/** Angles (degrees) a straightened stroke snaps to when close enough. */
private const val ANGLE_SNAP_STEP = 15.0
private const val ANGLE_SNAP_WINDOW = 7.0

/**
 * Turns a roughly-straight freehand stroke into a clean line, the way
 * Samsung Notes does: if every sampled point sits close enough to the chord
 * between the ends, the stroke collapses to that chord — and if the chord is
 * near a multiple of 15° it snaps to it, so horizontals and verticals come
 * out exactly level. Anything genuinely curved is returned untouched.
 */
fun straightenStroke(stroke: InkStroke): InkStroke {
    val pts = stroke.points
    if (pts.size < 3) return stroke
    val start = pts.first()
    val end = pts.last()
    val dx = end.x - start.x
    val dy = end.y - start.y
    val chord = hypot(dx, dy)
    // Too short to judge, or a closed-ish loop: leave it alone.
    if (chord < 48f) return stroke

    // Reject strokes that double back: the drawn length must be close to the
    // straight distance, otherwise a "there and back" scribble would flatten.
    var drawn = 0f
    for (i in 1 until pts.size) {
        drawn += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
    }
    if (drawn > chord * 1.25f) return stroke

    // Largest perpendicular distance from the chord.
    var maxSag = 0f
    for (p in pts) {
        val sag = abs(dx * (start.y - p.y) - dy * (start.x - p.x)) / chord
        if (sag > maxSag) maxSag = sag
    }
    if (maxSag > chord * STRAIGHT_TOLERANCE) return stroke

    // Straight enough: snap the direction to a nice angle, keeping the start
    // anchored and the length as drawn.
    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    val snapped = (angle / ANGLE_SNAP_STEP).roundToInt() * ANGLE_SNAP_STEP
    val useAngle =
        if (abs(angle - snapped) <= ANGLE_SNAP_WINDOW) snapped else angle
    val rad = Math.toRadians(useAngle)
    val tip = StrokePoint(
        x = start.x + (chord * cos(rad)).toFloat(),
        y = start.y + (chord * sin(rad)).toFloat(),
        p = end.p,
    )
    // Average pressure across the two ends so the line keeps an even weight.
    val evenPressure = (start.p + end.p) / 2f
    return stroke.copy(
        points = listOf(start.copy(p = evenPressure), tip.copy(p = evenPressure)),
    )
}
