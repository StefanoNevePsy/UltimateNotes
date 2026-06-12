package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.ui.theme.AppStyle
import kotlin.random.Random

/** Available block skins; "auto" resolves to the theme's signature one. */
val BlockDecors: List<Pair<String, String>> = listOf(
    "auto" to "Tema (auto)",
    "glass" to "Carta moderna",
    "parchment" to "Pergamena",
    "window" to "Finestra retrò",
    "sketch" to "Schizzo",
    "terminal" to "Terminale",
)

fun TextElement.resolvedDecor(theme: AppStyle): String? =
    if (decor == "auto") theme.blockDecor else decor

/** Extra padding the decor needs around the text. */
fun decorPadding(decor: String?): PaddingValues = when (decor) {
    null -> PaddingValues(4.dp)
    "window" -> PaddingValues(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 14.dp)
    "parchment" -> PaddingValues(18.dp)
    else -> PaddingValues(14.dp)
}

/** Theme-aware block skin: parchment scraps, OS windows, glass cards… */
@Composable
fun Modifier.blockDecor(decor: String?, seed: Int): Modifier {
    if (decor == null) return this
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface
    val onSurface = scheme.onSurface
    val primary = scheme.primary
    return drawBehind {
        when (decor) {
            "glass" -> {
                drawRoundRect(
                    surface.copy(alpha = 0.85f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
                drawRoundRect(
                    onSurface.copy(alpha = 0.1f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 2f),
                )
            }
            "parchment" -> drawParchment(surface, onSurface, seed)
            "window" -> drawRetroWindow(surface, primary)
            "sketch" -> {
                drawRoundRect(surface, cornerRadius = CornerRadius(10f, 10f))
                drawPath(
                    wobblyRect(size, seed),
                    onSurface.copy(alpha = 0.6f),
                    style = Stroke(width = 3f),
                )
            }
            "terminal" -> {
                drawRect(surface.copy(alpha = 0.92f))
                drawRect(primary.copy(alpha = 0.8f), style = Stroke(width = 2.5f))
                // Corner brackets, like an old phosphor UI.
                val l = 26f
                val t = 6f
                listOf(
                    Offset(0f, 0f) to listOf(Offset(l, 0f), Offset(0f, l)),
                    Offset(size.width, 0f) to
                        listOf(Offset(size.width - l, 0f), Offset(size.width, l)),
                    Offset(0f, size.height) to
                        listOf(Offset(l, size.height), Offset(0f, size.height - l)),
                    Offset(size.width, size.height) to listOf(
                        Offset(size.width - l, size.height),
                        Offset(size.width, size.height - l),
                    ),
                ).forEach { (corner, ends) ->
                    ends.forEach { end ->
                        drawLine(primary, corner, end, strokeWidth = t)
                    }
                }
            }
        }
    }
}

/** Torn parchment scrap: irregular edges, aged border, corner shading. */
private fun DrawScope.drawParchment(surface: Color, onSurface: Color, seed: Int) {
    val rnd = Random(seed)
    fun j(range: Float = 7f) = rnd.nextFloat() * range * 2 - range
    val w = size.width
    val h = size.height
    val steps = 7
    val path = Path().apply {
        moveTo(j(), j())
        for (i in 1..steps) lineTo(w * i / steps + j(), j(10f))
        for (i in 1..steps) lineTo(w + j(10f), h * i / steps + j())
        for (i in 1..steps) lineTo(w - w * i / steps + j(), h + j(10f))
        for (i in 1..steps) lineTo(j(10f), h - h * i / steps + j())
        close()
    }
    drawPath(path, surface)
    // Aged edge: darker outline + soft inner shadow tone.
    drawPath(path, onSurface.copy(alpha = 0.45f), style = Stroke(width = 3f))
    drawPath(
        path,
        onSurface.copy(alpha = 0.08f),
        style = Stroke(width = 14f),
    )
}

/** Win95-style window: title bar with fake buttons + beveled panel. */
private fun DrawScope.drawRetroWindow(surface: Color, primary: Color) {
    val w = size.width
    val h = size.height
    val bar = 30f
    val t = 2.5f
    drawRect(surface)
    // Bevel: light top/left, dark bottom/right.
    drawRect(Color.White.copy(alpha = 0.9f), size = Size(w, t))
    drawRect(Color.White.copy(alpha = 0.9f), size = Size(t, h))
    drawRect(Color(0xFF555555), topLeft = Offset(0f, h - t), size = Size(w, t))
    drawRect(Color(0xFF555555), topLeft = Offset(w - t, 0f), size = Size(t, h))
    // Title bar.
    drawRect(primary, topLeft = Offset(t * 2, t * 2), size = Size(w - t * 4, bar))
    // Fake buttons: minimize, maximize, close.
    val btn = 18f
    var bx = w - t * 2 - btn - 4f
    repeat(3) {
        drawRect(
            surface,
            topLeft = Offset(bx, t * 2 + (bar - btn) / 2f),
            size = Size(btn, btn),
        )
        drawRect(
            Color(0xFF555555),
            topLeft = Offset(bx, t * 2 + (bar - btn) / 2f),
            size = Size(btn, btn),
            style = Stroke(width = 1.5f),
        )
        bx -= btn + 4f
    }
    // Title bar grip lines.
    var gx = t * 2 + 8f
    repeat(3) {
        drawLine(
            Color.White.copy(alpha = 0.55f),
            Offset(gx, t * 2 + 7f),
            Offset(gx, t * 2 + bar - 7f),
            strokeWidth = 2f,
        )
        gx += 5f
    }
}

private fun wobblyRect(size: Size, seed: Int): Path {
    val rnd = Random(seed)
    fun j() = rnd.nextFloat() * 5f - 2.5f
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(j(), j())
        quadraticBezierTo(w / 2 + j() * 2, j() * 2, w + j(), j())
        quadraticBezierTo(w + j() * 2, h / 2 + j() * 2, w + j(), h + j())
        quadraticBezierTo(w / 2 + j() * 2, h + j() * 2, j(), h + j())
        quadraticBezierTo(j() * 2, h / 2 + j() * 2, j(), j())
        close()
    }
}
