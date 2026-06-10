package com.stefanoneve.ultimatenotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stefanoneve.ultimatenotes.ui.theme.LocalAppStyle
import kotlin.math.min
import kotlin.random.Random

/**
 * Floating translucent "glass" panel: soft shadow, rounded clip, translucent
 * surface and a subtle luminous edge. Radius and translucency come from the
 * active theme, so each theme keeps its own personality.
 */
@Composable
fun Modifier.glass(
    corner: Dp? = null,
    elevation: Dp = 10.dp,
): Modifier {
    val style = LocalAppStyle.current
    val shape = RoundedCornerShape(corner ?: style.corner)
    val edge = if (style.dark) Color.White.copy(alpha = 0.12f)
    else Color.White.copy(alpha = 0.65f)
    val edgeBottom = if (style.dark) Color.White.copy(alpha = 0.03f)
    else Color.White.copy(alpha = 0.15f)
    return this
        .shadow(elevation, shape, spotColor = Color.Black.copy(alpha = 0.35f))
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = style.glassAlpha))
        .border(1.dp, Brush.verticalGradient(listOf(edge, edgeBottom)), shape)
}

/**
 * Theme-aware card surface: glassy and smooth on modern themes, paper with a
 * sketchy hand-drawn ink border on "analog" themes (Seppia, Foresta).
 */
@Composable
fun Modifier.themedCard(
    seed: Int,
    corner: Dp? = null,
): Modifier {
    val style = LocalAppStyle.current
    val radius = corner ?: style.corner
    val shape = RoundedCornerShape(radius)
    return if (style.handDrawn) {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .handDrawnBorder(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                corner = radius,
                seed = seed,
            )
    } else {
        this
            .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (style.dark) 0.10f else 0.06f),
                shape,
            )
    }
}

/**
 * Sketchy "ink" border, excalidraw-style: each edge is a couple of slightly
 * wobbly bezier segments. Deterministic per [seed] so cards don't flicker.
 */
fun Modifier.handDrawnBorder(
    color: Color,
    corner: Dp,
    seed: Int,
    strokeWidth: Dp = 1.6.dp,
): Modifier = drawBehind {
    val rnd = Random(seed)
    val c = min(corner.toPx(), min(size.width, size.height) / 3f)
    val w = size.width
    val h = size.height
    fun j() = rnd.nextFloat() * 5f - 2.5f // jitter in px

    val path = Path().apply {
        moveTo(c + j(), j())
        // top edge
        quadraticBezierTo(w / 2f + j() * 2, j() * 2, w - c + j(), j())
        // top-right corner
        quadraticBezierTo(w + j(), j(), w + j(), c + j())
        // right edge
        quadraticBezierTo(w + j() * 2, h / 2f + j() * 2, w + j(), h - c + j())
        // bottom-right corner
        quadraticBezierTo(w + j(), h + j(), w - c + j(), h + j())
        // bottom edge
        quadraticBezierTo(w / 2f + j() * 2, h + j() * 2, c + j(), h + j())
        // bottom-left corner
        quadraticBezierTo(j(), h + j(), j(), h - c + j())
        // left edge
        quadraticBezierTo(j() * 2, h / 2f + j() * 2, j(), c + j())
        // top-left corner
        quadraticBezierTo(j(), j(), c + j(), j())
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

/** Accent gradient brush of the active theme. */
@Composable
fun themeGradient(): Brush {
    val style = LocalAppStyle.current
    return Brush.linearGradient(style.gradient, start = Offset.Zero)
}

/** Shape helper using the theme corner radius. */
@Composable
fun themeShape(corner: Dp? = null): Shape =
    RoundedCornerShape(corner ?: LocalAppStyle.current.corner)
