package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.random.Random

/**
 * Theme stroke "texture": how connector/frame lines are painted, independent
 * of the dash pattern. clean = smooth digital, ink = double-passed china,
 * chalk = grainy multi-pass, pixel = hard caps with a retro drop shadow,
 * neon = glowing tube.
 */
fun DrawScope.drawFlavoredPath(
    path: Path,
    color: Color,
    width: Float,
    effect: PathEffect?,
    flavor: String,
    seed: Int,
) {
    when (flavor) {
        "ink" -> {
            val rnd = Random(seed)
            // Main china pass + a thinner offset pass, like re-inked lines.
            drawPath(
                path, color,
                style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
            )
            translate(
                rnd.nextFloat() * width * 0.7f - width * 0.35f,
                rnd.nextFloat() * width * 0.7f + width * 0.25f,
            ) {
                drawPath(
                    path, color.copy(alpha = color.alpha * 0.45f),
                    style = Stroke(
                        width * 0.5f,
                        cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect,
                    ),
                )
            }
        }
        "chalk" -> {
            val rnd = Random(seed)
            repeat(3) {
                translate(
                    rnd.nextFloat() * width - width / 2f,
                    rnd.nextFloat() * width - width / 2f,
                ) {
                    drawPath(
                        path, color.copy(alpha = color.alpha * 0.38f),
                        style = Stroke(
                            width * (0.7f + rnd.nextFloat() * 0.5f),
                            cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect,
                        ),
                    )
                }
            }
        }
        "pixel" -> {
            // Retro: hard caps + a 3D drop shadow, like Win95 line art.
            translate(width * 0.9f, width * 0.9f) {
                drawPath(
                    path, Color.Black.copy(alpha = 0.3f),
                    style = Stroke(width, cap = StrokeCap.Square, pathEffect = effect),
                )
            }
            drawPath(
                path, color,
                style = Stroke(width, cap = StrokeCap.Square, pathEffect = effect),
            )
        }
        "neon" -> {
            // Glowing tube: wide halo, colored body, bright core.
            drawPath(
                path, color.copy(alpha = 0.22f),
                style = Stroke(width * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
            )
            drawPath(
                path, color,
                style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
            )
            drawPath(
                path, Color.White.copy(alpha = 0.55f),
                style = Stroke(width * 0.38f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
            )
        }
        else -> drawPath(
            path, color,
            style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
        )
    }
}

/** Catmull-Rom spline through [points], sampled [perSegment] times each. */
fun catmullRom(points: List<Offset>, perSegment: Int = 16): List<Offset> {
    if (points.size < 2) return points
    if (points.size == 2) return points
    val pts = listOf(points.first()) + points + listOf(points.last())
    val out = mutableListOf<Offset>()
    for (i in 0 until pts.size - 3) {
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val p2 = pts[i + 2]
        val p3 = pts[i + 3]
        for (j in 0 until perSegment) {
            val t = j / perSegment.toFloat()
            val t2 = t * t
            val t3 = t2 * t
            out += Offset(
                0.5f * (
                    2 * p1.x + (p2.x - p0.x) * t +
                        (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                        (3 * p1.x - p0.x - 3 * p2.x + p3.x) * t3
                    ),
                0.5f * (
                    2 * p1.y + (p2.y - p0.y) * t +
                        (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                        (3 * p1.y - p0.y - 3 * p2.y + p3.y) * t3
                    ),
            )
        }
    }
    out += points.last()
    return out
}
