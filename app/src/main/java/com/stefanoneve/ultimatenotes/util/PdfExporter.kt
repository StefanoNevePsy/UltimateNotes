package com.stefanoneve.ultimatenotes.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.FileElement
import com.stefanoneve.ultimatenotes.data.model.FrameShape
import com.stefanoneve.ultimatenotes.data.model.ImageElement
import com.stefanoneve.ultimatenotes.data.model.LineStyle
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.model.NoteLinkElement
import com.stefanoneve.ultimatenotes.data.model.StyleSet
import com.stefanoneve.ultimatenotes.data.model.TapeElement
import com.stefanoneve.ultimatenotes.data.model.TextElement
import com.stefanoneve.ultimatenotes.data.model.WebLinkElement
import com.stefanoneve.ultimatenotes.ui.editor.boldRegex
import com.stefanoneve.ultimatenotes.ui.editor.codeRegex
import com.stefanoneve.ultimatenotes.ui.editor.colorTagRegex
import com.stefanoneve.ultimatenotes.ui.editor.capLength
import com.stefanoneve.ultimatenotes.ui.editor.connectorGeometry
import com.stefanoneve.ultimatenotes.ui.editor.elementRect
import com.stefanoneve.ultimatenotes.ui.editor.pointAlong
import com.stefanoneve.ultimatenotes.ui.editor.trimPolyline
import com.stefanoneve.ultimatenotes.ui.editor.italicRegex
import com.stefanoneve.ultimatenotes.ui.editor.parseColorToken
import com.stefanoneve.ultimatenotes.ui.editor.parseHexColor
import com.stefanoneve.ultimatenotes.ui.editor.resolvedBgColor
import com.stefanoneve.ultimatenotes.ui.editor.resolvedColor
import com.stefanoneve.ultimatenotes.ui.editor.resolvedDecor
import com.stefanoneve.ultimatenotes.ui.editor.resolvedFontId
import com.stefanoneve.ultimatenotes.ui.editor.resolvedLineStyle
import com.stefanoneve.ultimatenotes.ui.editor.resolvedPattern
import com.stefanoneve.ultimatenotes.ui.editor.resolvedShape
import com.stefanoneve.ultimatenotes.ui.editor.resolvedTextColor
import com.stefanoneve.ultimatenotes.ui.editor.strikeRegex
import com.stefanoneve.ultimatenotes.ui.theme.AppStyle
import java.io.File
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Renders the whole note (ink, tape, frames, connectors, images, styled
 * text, embed cards) into a single-page PDF sized to the content bounds.
 */
class PdfExporter(
    private val context: Context,
    private val fontManager: FontManager,
) {

    /** Measured layouts + world-space bounds, shared by PDF and PNG export. */
    private class RenderPlan(
        val layouts: Map<String, StaticLayout>,
        val sizes: Map<String, Size>,
        val bounds: RectF,
    )

    private fun plan(content: NoteContent, styleSet: StyleSet, theme: AppStyle): RenderPlan {
        val density = context.resources.displayMetrics.density
        val textColor = theme.colorScheme.onBackground.toArgb()

        // Measure text blocks so bounds and rendering match.
        val layouts = mutableMapOf<String, StaticLayout>()
        val sizes = mutableMapOf<String, Size>()
        content.elements.forEach { e ->
            when (e) {
                is TextElement -> {
                    val layout = textLayout(e, styleSet, theme, textColor, density)
                    layouts[e.id] = layout
                    sizes[e.id] = Size(e.width, layout.height.toFloat())
                }
                is NoteLinkElement -> sizes[e.id] = Size(e.width, 120f)
                is WebLinkElement -> sizes[e.id] = Size(e.width, 110f)
                is FileElement -> sizes[e.id] = Size(e.width, 100f)
                is ImageElement -> sizes[e.id] = Size(e.width, e.height)
            }
        }

        // Content bounds.
        val bounds = RectF()
        var first = true
        fun include(l: Float, t: Float, r: Float, b: Float) {
            if (first) {
                bounds.set(l, t, r, b)
                first = false
            } else bounds.union(l, t, r, b)
        }
        content.elements.forEach { e ->
            val r = elementRect(e, sizes)
            include(r.left, r.top, r.right, r.bottom)
        }
        content.strokes.forEach { s ->
            s.points.forEach { include(it.x, it.y, it.x, it.y) }
        }
        content.frames.forEach {
            val r = com.stefanoneve.ultimatenotes.ui.editor.effectiveFrameRect(it, content, sizes)
            include(r.left, r.top, r.right, r.bottom)
        }
        content.tapes.forEach {
            include(
                min(it.x1, it.x2) - it.thickness, min(it.y1, it.y2) - it.thickness,
                max(it.x1, it.x2) + it.thickness, max(it.y1, it.y2) + it.thickness,
            )
        }
        content.connectors.forEach { c ->
            connectorGeometry(c, content, sizes)?.samples?.forEach {
                include(it.x, it.y, it.x, it.y)
            }
        }
        if (first) bounds.set(0f, 0f, 800f, 600f)
        bounds.inset(-60f, -60f)
        return RenderPlan(layouts, sizes, bounds)
    }

    /** Full note draw pass, same z-order as the editor (frames under ink). */
    private fun drawAll(
        canvas: Canvas,
        content: NoteContent,
        p: RenderPlan,
        assetsDir: File,
        theme: AppStyle,
    ) {
        canvas.drawColor(theme.colorScheme.background.toArgb())
        drawFrames(canvas, content, p.sizes, theme)
        drawStrokes(canvas, content, theme)
        drawConnectors(canvas, content, p.sizes, theme)
        drawElements(canvas, content, p.layouts, p.sizes, assetsDir, theme)
        drawTapes(canvas, content, theme)
    }

    fun export(
        content: NoteContent,
        styleSet: StyleSet,
        theme: AppStyle,
        assetsDir: File,
        uri: Uri,
    ): Result<Unit> = runCatching {
        val p = plan(content, styleSet, theme)
        val bounds = p.bounds
        val scale = min(1f, 13000f / max(bounds.width(), bounds.height()))
        val pageW = (bounds.width() * scale).toInt().coerceAtLeast(64)
        val pageH = (bounds.height() * scale).toInt().coerceAtLeast(64)

        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(pageW, pageH, 1).create(),
        )
        val canvas = page.canvas
        canvas.scale(scale, scale)
        canvas.translate(-bounds.left, -bounds.top)
        drawAll(canvas, content, p, assetsDir, theme)

        try {
            document.finishPage(page)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                document.writeTo(out)
            } ?: error("Impossibile aprire la destinazione")
        } finally {
            document.close()
        }
    }

    /** Renders the note into a PNG image sized to the content bounds. */
    fun exportPng(
        content: NoteContent,
        styleSet: StyleSet,
        theme: AppStyle,
        assetsDir: File,
        uri: Uri,
    ): Result<Unit> = runCatching {
        val p = plan(content, styleSet, theme)
        val bounds = p.bounds
        // Render at up to 2x for crispness, capped to a safe bitmap size.
        val scale = min(2f, 4096f / max(bounds.width(), bounds.height()))
        val w = (bounds.width() * scale).toInt().coerceIn(64, 4096)
        val h = (bounds.height() * scale).toInt().coerceIn(64, 4096)
        val bitmap = android.graphics.Bitmap.createBitmap(
            w, h, android.graphics.Bitmap.Config.ARGB_8888,
        )
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            canvas.translate(-bounds.left, -bounds.top)
            drawAll(canvas, content, p, assetsDir, theme)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            } ?: error("Impossibile aprire la destinazione")
        } finally {
            bitmap.recycle()
        }
    }

    private fun dashFor(
        style: LineStyle,
        w: Float,
        animated: Boolean = false,
    ): DashPathEffect? = when (style) {
        // An "animated" solid line renders as marching dashes on screen; the
        // export freezes that same dash pattern instead of a plain line.
        LineStyle.SOLID -> if (animated) DashPathEffect(floatArrayOf(w * 6f, w * 3f), 0f) else null
        LineStyle.DASHED -> DashPathEffect(floatArrayOf(w * 4.5f, w * 3.5f), 0f)
        LineStyle.DOTTED -> DashPathEffect(floatArrayOf(0.1f, w * 3f), 0f)
    }

    private fun drawStrokes(canvas: Canvas, content: NoteContent, theme: AppStyle) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        content.strokes.forEach { stroke ->
            paint.color = stroke.resolvedColor(theme).toInt()
            val isHl =
                stroke.type == com.stefanoneve.ultimatenotes.data.model.StrokeType.HIGHLIGHTER
            paint.strokeCap = if (isHl) Paint.Cap.SQUARE else Paint.Cap.ROUND
            paint.pathEffect =
                dashFor(stroke.lineStyle ?: LineStyle.SOLID, stroke.width, stroke.animated)
            val pts = stroke.points
            if (pts.size == 1) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(pts[0].x, pts[0].y, stroke.width / 2f, paint)
                paint.style = Paint.Style.STROKE
            }
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val pressure = if (isHl) 1f else (a.p + b.p) / 2f
                paint.strokeWidth =
                    stroke.width * (if (isHl) 1f else 0.4f + 0.8f * pressure)
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            }
        }
    }

    private fun drawFrames(
        canvas: Canvas,
        content: NoteContent,
        sizes: Map<String, Size>,
        theme: AppStyle,
    ) {
        content.frames.forEach { raw ->
            val eff = com.stefanoneve.ultimatenotes.ui.editor.effectiveFrameRect(
                raw, content, sizes,
            )
            val f = raw.copy(
                color = raw.resolvedColor(theme),
                shape = raw.resolvedShape(theme),
                lineStyle = raw.resolvedLineStyle(theme),
                x = eff.left, y = eff.top, width = eff.width, height = eff.height,
            )
            val rect = RectF(f.x, f.y, f.x + f.width, f.y + f.height)
            val path = Path().apply {
                when (f.shape) {
                    FrameShape.RECT -> addRect(rect, Path.Direction.CW)
                    FrameShape.ELLIPSE -> addOval(rect, Path.Direction.CW)
                    else -> addRoundRect(rect, 28f, 28f, Path.Direction.CW)
                }
            }
            val decor = f.resolvedDecor(theme)
            if (decor != null) {
                // Simplified skin panel for export. Like the live editor
                // (FrameLayer.drawFrame), the panel replaces fill + outline.
                canvas.drawRoundRect(
                    rect, 16f, 16f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = theme.colorScheme.surface.toArgb()
                    },
                )
            } else {
                if (f.filled) {
                    canvas.drawPath(
                        path,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = f.color.toInt()
                            alpha = 20
                        },
                    )
                }
                canvas.drawPath(
                    path,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = f.strokeWidth
                        color = f.color.toInt()
                        pathEffect = dashFor(f.lineStyle ?: LineStyle.SOLID, f.strokeWidth, f.animated)
                    },
                )
            }
            if (f.label.isNotBlank()) {
                canvas.drawText(
                    f.label,
                    f.x,
                    f.y - 12f,
                    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = f.color.toInt()
                        textSize = 26f
                        typeface = fontManager.typefaceOf(theme.displayFontId)
                    },
                )
            }
        }
    }

    private fun drawTapes(canvas: Canvas, content: NoteContent, theme: AppStyle) {
        content.tapes.forEach { raw ->
            val t = raw.copy(
                color = raw.resolvedColor(theme),
                pattern = raw.resolvedPattern(theme),
            )
            val length = hypot(t.x2 - t.x1, t.y2 - t.y1)
            if (length < 1f) return@forEach
            val angle = Math.toDegrees(
                atan2((t.y2 - t.y1).toDouble(), (t.x2 - t.x1).toDouble()),
            ).toFloat()
            canvas.save()
            canvas.rotate(angle, t.x1, t.y1)
            val rect = RectF(t.x1, t.y1 - t.thickness / 2f, t.x1 + length, t.y1 + t.thickness / 2f)
            canvas.drawRect(
                rect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = t.color.toInt()
                    alpha = (t.alpha * 255).toInt()
                },
            )
            // Simplified stripes pattern for all decorated tapes.
            if (t.pattern != com.stefanoneve.ultimatenotes.data.model.TapePattern.SOLID) {
                val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = 80
                    strokeWidth = t.thickness * 0.25f
                }
                canvas.save()
                canvas.clipRect(rect)
                var x = rect.left - t.thickness
                while (x < rect.right + t.thickness) {
                    canvas.drawLine(x, rect.bottom, x + t.thickness, rect.top, deco)
                    x += t.thickness * 0.9f
                }
                canvas.restore()
            }
            canvas.restore()
        }
    }

    private fun drawConnectors(
        canvas: Canvas,
        content: NoteContent,
        sizes: Map<String, Size>,
        theme: AppStyle,
    ) {
        content.connectors.forEach { raw ->
            val c = raw.copy(
                color = raw.resolvedColor(theme),
                lineStyle = raw.resolvedLineStyle(theme),
            )
            val geo = connectorGeometry(c, content, sizes) ?: return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = c.width
                strokeCap = Paint.Cap.ROUND
                color = c.color.toInt()
                pathEffect = dashFor(c.lineStyle ?: LineStyle.SOLID, c.width, c.animated)
            }
            val headLen = capLength(c.width)
            val body = trimPolyline(
                geo.samples,
                if (c.startCap != com.stefanoneve.ultimatenotes.data.model.CapStyle.NONE) {
                    headLen * 0.55f
                } else 0f,
                if (c.endCap != com.stefanoneve.ultimatenotes.data.model.CapStyle.NONE) {
                    headLen * 0.55f
                } else 0f,
            )
            val path = Path().apply {
                moveTo(body.first().x, body.first().y)
                body.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
            // Arrow heads.
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.color.toInt() }
            fun cap(
                capStyle: com.stefanoneve.ultimatenotes.data.model.CapStyle,
                tip: androidx.compose.ui.geometry.Offset,
                from: androidx.compose.ui.geometry.Offset,
            ) {
                when (capStyle) {
                    com.stefanoneve.ultimatenotes.data.model.CapStyle.NONE -> Unit
                    com.stefanoneve.ultimatenotes.data.model.CapStyle.DOT ->
                        canvas.drawCircle(
                            tip.x, tip.y, (c.width * 1.8f).coerceAtLeast(6f), fill,
                        )
                    com.stefanoneve.ultimatenotes.data.model.CapStyle.ARROW -> {
                        val ang = atan2(tip.y - from.y, tip.x - from.x)
                        val len = headLen
                        val spread = 0.46f
                        val head = Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(
                                tip.x - len * kotlin.math.cos(ang - spread),
                                tip.y - len * kotlin.math.sin(ang - spread),
                            )
                            lineTo(
                                tip.x - len * kotlin.math.cos(ang + spread),
                                tip.y - len * kotlin.math.sin(ang + spread),
                            )
                            close()
                        }
                        canvas.drawPath(head, fill)
                    }
                }
            }
            cap(c.startCap, geo.start, pointAlong(geo.samples, false, capLength(c.width)))
            cap(c.endCap, geo.end, pointAlong(geo.samples, true, capLength(c.width)))
        }
    }

    private fun drawElements(
        canvas: Canvas,
        content: NoteContent,
        layouts: Map<String, StaticLayout>,
        sizes: Map<String, Size>,
        assetsDir: File,
        theme: AppStyle,
    ) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.colorScheme.surface.toArgb()
        }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = theme.colorScheme.primary.toArgb()
            alpha = 120
        }
        val cardText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.colorScheme.onBackground.toArgb()
            textSize = 30f
            typeface = fontManager.typefaceOf(theme.bodyFontId)
        }

        content.elements.forEach { e ->
            when (e) {
                is ImageElement -> {
                    val file = File(assetsDir, e.fileName)
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                        val sample = generateSequence(1) { it * 2 }
                            .first { opts.outWidth / it <= 2048 && opts.outHeight / it <= 2048 }
                        val bmp = BitmapFactory.decodeFile(
                            file.absolutePath,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        )
                        if (bmp != null) {
                            canvas.drawBitmap(
                                bmp,
                                null,
                                RectF(e.x, e.y, e.x + e.width, e.y + e.height),
                                Paint(Paint.FILTER_BITMAP_FLAG),
                            )
                            bmp.recycle()
                        }
                    }
                }
                is TextElement -> {
                    val layout = layouts[e.id] ?: return@forEach
                    canvas.save()
                    canvas.translate(e.x, e.y)
                    canvas.scale(e.scale, e.scale)
                    val bg = e.resolvedBgColor(theme)
                    if (bg != null) {
                        canvas.drawRoundRect(
                            RectF(-6f, -6f, e.width + 6f, layout.height + 6f),
                            14f, 14f,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg.toInt() },
                        )
                    }
                    layout.draw(canvas)
                    canvas.restore()
                }
                is NoteLinkElement, is WebLinkElement, is FileElement -> {
                    val r = elementRect(e, sizes)
                    val rect = RectF(r.left, r.top, r.right, r.bottom)
                    canvas.drawRoundRect(rect, 16f, 16f, cardPaint)
                    canvas.drawRoundRect(rect, 16f, 16f, cardStroke)
                    val label = when (e) {
                        is WebLinkElement -> e.title.ifBlank { e.url }
                        is FileElement -> e.displayName
                        else -> "Nota collegata"
                    }
                    canvas.drawText(
                        android.text.TextUtils.ellipsize(
                            label, cardText, rect.width() - 40f,
                            android.text.TextUtils.TruncateAt.END,
                        ).toString(),
                        rect.left + 20f,
                        rect.top + 50f,
                        cardText,
                    )
                }
            }
        }
    }

    private fun textLayout(
        e: TextElement,
        styleSet: StyleSet,
        theme: AppStyle,
        defaultColor: Int,
        density: Float,
    ): StaticLayout {
        val def = styleSet.byId(e.styleId)
        val baseSizePx = (e.fontSize ?: def.fontSize) * density
        val color = e.resolvedTextColor(theme)?.toInt() ?: defaultColor
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = baseSizePx
            this.color = color
            typeface = fontManager.typefaceOf(e.resolvedFontId(theme, styleSet))
        }
        val span = buildSpannable(e.text, styleSet, theme, color, density)
        return StaticLayout.Builder
            .obtain(span, 0, span.length, paint, e.width.toInt().coerceAtLeast(40))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .build()
    }

    /** Lightweight markdown styling for export (mirrors the live editor). */
    private fun buildSpannable(
        text: String,
        styleSet: StyleSet,
        theme: AppStyle,
        baseColor: Int,
        density: Float,
    ): Spanned {
        val sb = SpannableStringBuilder(text)
        val dim = (baseColor and 0x00FFFFFF) or (0x55 shl 24)
        fun set(what: Any, s: Int, e: Int) {
            if (s in 0..e && e <= sb.length) {
                sb.setSpan(what, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        var lineStart = 0
        while (lineStart <= text.length) {
            val lineEnd =
                text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            val line = text.substring(lineStart, lineEnd)
            val level = when {
                line.startsWith("### ") -> 3
                line.startsWith("## ") -> 2
                line.startsWith("# ") -> 1
                else -> 0
            }
            if (level > 0) {
                val def = styleSet.byId("title$level")
                set(AbsoluteSizeSpan((def.fontSize * density).toInt()), lineStart, lineEnd)
                set(StyleSpan(Typeface.BOLD), lineStart, lineEnd)
                set(PdfFontSpan(fontManager.typefaceOf(theme.displayFontId)), lineStart, lineEnd)
                set(ForegroundColorSpan(dim), lineStart, lineStart + level + 1)
            }
            // Hanging indent for list items, so wrapped lines align with text.
            com.stefanoneve.ultimatenotes.ui.editor.parseListLine(line)?.let { info ->
                val margin = (info.prefixLength * styleSet.byId("body").fontSize *
                    density * 0.6f).toInt()
                runCatching {
                    sb.setSpan(
                        android.text.style.LeadingMarginSpan.Standard(0, margin),
                        lineStart,
                        if (lineEnd < sb.length) lineEnd + 1 else sb.length,
                        Spanned.SPAN_PARAGRAPH,
                    )
                }
            }
            boldRegex.findAll(line).forEach {
                set(
                    StyleSpan(Typeface.BOLD),
                    lineStart + it.range.first, lineStart + it.range.last + 1,
                )
            }
            italicRegex.findAll(line).forEach {
                set(
                    StyleSpan(Typeface.ITALIC),
                    lineStart + it.range.first, lineStart + it.range.last + 1,
                )
            }
            strikeRegex.findAll(line).forEach {
                set(
                    StrikethroughSpan(),
                    lineStart + it.range.first, lineStart + it.range.last + 1,
                )
            }
            codeRegex.findAll(line).forEach {
                set(
                    android.text.style.TypefaceSpan("monospace"),
                    lineStart + it.range.first, lineStart + it.range.last + 1,
                )
            }
            if (lineEnd == text.length) break
            lineStart = lineEnd + 1
        }
        colorTagRegex.findAll(text).forEach { m ->
            val color = parseColorToken(
                m.groupValues[1], theme.resolvedElementColors(),
            ) ?: return@forEach
            val content = m.groups[2] ?: return@forEach
            set(ForegroundColorSpan(color.toInt()), content.range.first, content.range.last + 1)
            set(ForegroundColorSpan(dim), m.range.first, content.range.first)
            set(ForegroundColorSpan(dim), content.range.last + 1, m.range.last + 1)
        }
        return sb
    }
}

/**
 * Inline span that paints text with an arbitrary [Typeface] (TypefaceSpan
 * with a Typeface argument requires API 28; this works from minSdk 26).
 */
private class PdfFontSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = typeface
    }
}
