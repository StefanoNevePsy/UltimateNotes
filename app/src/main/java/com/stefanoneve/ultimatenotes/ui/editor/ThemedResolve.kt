package com.stefanoneve.ultimatenotes.ui.editor

import com.stefanoneve.ultimatenotes.data.model.ConnectorElement
import com.stefanoneve.ultimatenotes.data.model.FrameElement
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.STICKY_AUTO
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TapePattern
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.ui.theme.AppStyle

/**
 * "Auto" values (color 0, null enums) are resolved against the active theme
 * at render time, so existing canvas objects re-skin when the theme changes.
 * Anything the user picked explicitly stays fixed.
 */

fun ConnectorElement.resolvedColor(theme: AppStyle): Long =
    if (color == 0L) theme.accentArgb() else color

fun ConnectorElement.resolvedLineStyle(theme: AppStyle): LineStyle =
    lineStyle ?: theme.connectorLineStyle

fun FrameElement.resolvedColor(theme: AppStyle): Long =
    if (color == 0L) theme.accentArgb() else color

fun FrameElement.resolvedShape(theme: AppStyle): FrameShape =
    shape ?: theme.frameShape

fun FrameElement.resolvedLineStyle(theme: AppStyle): LineStyle =
    lineStyle ?: theme.frameLineStyle

fun TapeElement.resolvedColor(theme: AppStyle): Long =
    if (color == 0L) theme.resolvedTapeColors().first() else color

fun TapeElement.resolvedPattern(theme: AppStyle): TapePattern =
    pattern ?: theme.tapePattern

fun TextElement.resolvedBgColor(theme: AppStyle): Long? = when (bgColor) {
    null -> null
    STICKY_AUTO -> theme.resolvedStickyColors().first()
    else -> bgColor
}
