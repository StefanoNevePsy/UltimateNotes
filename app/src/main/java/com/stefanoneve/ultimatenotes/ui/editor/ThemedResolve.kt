package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.ui.graphics.toArgb
import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.FrameElement
import com.stefanoneve.ultimatenotes.data.model.InkStroke
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TapePattern
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.ui.theme.AppStyle

/**
 * Theme-role color encoding: tiny Long values are impossible real ARGB
 * colors (alpha = 0), so they are reserved as palette-slot references.
 *
 *  - 0          → "auto", slot 0 of the relevant theme palette
 *  - 1..[MAX_ROLE] → slot (value - 1)
 *  - anything else → fixed ARGB color chosen by the user
 *
 * Role-based values are resolved at render time, so elements keep their
 * "role" (accent 1, accent 2, …) across theme switches; fixed colors stay.
 */
const val MAX_ROLE = 16L

fun roleValue(slot: Int): Long = (slot + 1).toLong()

fun isRole(value: Long): Boolean = value in 0..MAX_ROLE

fun resolveRole(value: Long, palette: List<Long>): Long =
    if (isRole(value)) {
        palette[((value - 1).coerceAtLeast(0) % palette.size).toInt()]
    } else value

fun ConnectorElement.resolvedColor(theme: AppStyle): Long =
    resolveRole(color, theme.resolvedElementColors())

fun ConnectorElement.resolvedLineStyle(theme: AppStyle): LineStyle =
    lineStyle ?: theme.connectorLineStyle

/**
 * Ink color. 0 = "auto": the theme's ink (onSurface), so a stroke stays
 * readable on any background; 1..MAX_ROLE follow an accent slot; anything
 * else is a fixed color the user picked.
 */
fun InkStroke.resolvedColor(theme: AppStyle): Long = when {
    color == 0L ->
        theme.colorScheme.onSurface.toArgb().toLong() and 0xFFFFFFFFL
    isRole(color) -> resolveRole(color, theme.resolvedElementColors())
    else -> color
}

fun FrameElement.resolvedColor(theme: AppStyle): Long =
    resolveRole(color, theme.resolvedElementColors())

fun FrameElement.resolvedShape(theme: AppStyle): FrameShape =
    shape ?: theme.frameShape

fun FrameElement.resolvedLineStyle(theme: AppStyle): LineStyle =
    lineStyle ?: theme.frameLineStyle

fun TapeElement.resolvedColor(theme: AppStyle): Long =
    resolveRole(color, theme.resolvedTapeColors())

fun TapeElement.resolvedPattern(theme: AppStyle): TapePattern =
    pattern ?: theme.tapePattern

fun TextElement.resolvedBgColor(theme: AppStyle): Long? = bgColor?.let {
    resolveRole(it, theme.resolvedStickyColors())
}

/** Block text color: null adapts to the theme's onSurface (handled by UI). */
fun TextElement.resolvedTextColor(theme: AppStyle): Long? = color?.let {
    resolveRole(it, theme.resolvedElementColors())
}

/**
 * Font a text block actually renders with. Precedence: the block's explicit
 * font, then the paragraph style's, then the theme's — heading styles take the
 * theme's display font, everything else its body font. Blank / "default" ids
 * (baked into old blocks) count as "follow the theme", never as a real font.
 */
fun TextElement.resolvedFontId(
    theme: AppStyle,
    styleSet: com.stefanoneve.ultimatenotes.data.model.StyleSet,
): String {
    fun String?.orNull() = this?.takeIf { it.isNotBlank() && it != "default" }
    return fontId.orNull()
        ?: styleSet.byId(styleId).fontId.orNull()
        ?: if (styleId.startsWith("title")) theme.displayFontId else theme.bodyFontId
}
