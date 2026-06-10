package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.stefanoneve.ultimatenotes.data.model.StyleSet

/**
 * Live markdown styling: characters stay in place (identity offset mapping, so
 * editing is seamless) while the text is rendered with the markdown semantics:
 * #/##/### headers, **bold**, *italic*, ~~strike~~, `code`, > quote, - lists,
 * - [ ] checkboxes.
 */
class MarkdownVisualTransformation(
    private val styleSet: StyleSet,
    private val baseColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(styleMarkdown(text.text, styleSet, baseColor), OffsetMapping.Identity)
}

private val boldRegex = Regex("""\*\*(.+?)\*\*""")
private val italicRegex = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
private val strikeRegex = Regex("""~~(.+?)~~""")
private val codeRegex = Regex("""`([^`\n]+)`""")

fun styleMarkdown(
    source: String,
    styleSet: StyleSet,
    baseColor: Color,
): AnnotatedString {
    val builder = AnnotatedString.Builder(source)
    var lineStart = 0
    // Iterate lines without copying the string.
    while (lineStart <= source.length) {
        val lineEnd = source.indexOf('\n', lineStart).let { if (it == -1) source.length else it }
        styleLine(builder, source, lineStart, lineEnd, styleSet, baseColor)
        if (lineEnd == source.length) break
        lineStart = lineEnd + 1
    }
    return builder.toAnnotatedString()
}

private fun styleLine(
    builder: AnnotatedString.Builder,
    source: String,
    start: Int,
    end: Int,
    styleSet: StyleSet,
    baseColor: Color,
) {
    if (start >= end) return
    val line = source.substring(start, end)
    val markerColor = baseColor.copy(alpha = 0.35f)

    fun span(style: SpanStyle, from: Int, to: Int) =
        builder.addStyle(style, start + from, start + to)

    val headerLevel = when {
        line.startsWith("### ") -> 3
        line.startsWith("## ") -> 2
        line.startsWith("# ") -> 1
        else -> 0
    }
    if (headerLevel > 0) {
        val def = styleSet.byId("title$headerLevel")
        span(SpanStyle(color = markerColor), 0, headerLevel + 1)
        span(
            SpanStyle(
                fontSize = def.fontSize.sp,
                fontWeight = FontWeight(def.fontWeight),
            ),
            0,
            line.length,
        )
    }
    if (line.startsWith("> ")) {
        span(
            SpanStyle(color = baseColor.copy(alpha = 0.7f), fontStyle = FontStyle.Italic),
            0,
            line.length,
        )
        span(SpanStyle(color = markerColor), 0, 2)
    }
    if (line.startsWith("- [ ] ") || line.startsWith("- [x] ")) {
        span(SpanStyle(color = markerColor), 0, 6)
        if (line.startsWith("- [x] ")) {
            span(
                SpanStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = baseColor.copy(alpha = 0.5f),
                ),
                6,
                line.length,
            )
        }
    } else if (line.startsWith("- ") || line.startsWith("* ")) {
        span(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold), 0, 2)
    }

    boldRegex.findAll(line).forEach { m ->
        span(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
        span(SpanStyle(color = markerColor), m.range.first, m.range.first + 2)
        span(SpanStyle(color = markerColor), m.range.last - 1, m.range.last + 1)
    }
    italicRegex.findAll(line).forEach { m ->
        span(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
        span(SpanStyle(color = markerColor), m.range.first, m.range.first + 1)
        span(SpanStyle(color = markerColor), m.range.last, m.range.last + 1)
    }
    strikeRegex.findAll(line).forEach { m ->
        span(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            m.range.first,
            m.range.last + 1,
        )
        span(SpanStyle(color = markerColor), m.range.first, m.range.first + 2)
        span(SpanStyle(color = markerColor), m.range.last - 1, m.range.last + 1)
    }
    codeRegex.findAll(line).forEach { m ->
        span(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = baseColor.copy(alpha = 0.08f),
            ),
            m.range.first,
            m.range.last + 1,
        )
    }
}

/** Base text style for a text block, derived from its paragraph style. */
fun baseTextStyle(
    styleSet: StyleSet,
    styleId: String,
    fontFamily: FontFamily,
    color: Color,
): TextStyle {
    val def = styleSet.byId(styleId)
    return TextStyle(
        fontSize = def.fontSize.sp,
        fontWeight = FontWeight(def.fontWeight),
        fontFamily = fontFamily,
        color = color,
        lineHeight = (def.fontSize * 1.4f).sp,
    )
}
