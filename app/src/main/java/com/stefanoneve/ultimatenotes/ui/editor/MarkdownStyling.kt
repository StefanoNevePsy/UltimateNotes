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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stefanoneve.ultimatenotes.data.model.StyleSet

/**
 * Live markdown styling: characters stay in place (identity offset mapping, so
 * editing is seamless) while the text is rendered with the markdown semantics:
 * #/##/### headers, **bold**, *italic*, ~~strike~~, `code`, > quote, - lists,
 * - [ ] checkboxes, plus inline {c:#RRGGBB}color{/c} and {f:id}font{/f} tags.
 */
class MarkdownVisualTransformation(
    private val styleSet: StyleSet,
    private val baseColor: Color,
    private val fontResolver: (String) -> FontFamily? = { null },
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            styleMarkdown(text.text, styleSet, baseColor, fontResolver),
            OffsetMapping.Identity,
        )
}

val boldRegex = Regex("""\*\*(.+?)\*\*""")
val italicRegex = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
val strikeRegex = Regex("""~~(.+?)~~""")
val codeRegex = Regex("""`([^`\n]+)`""")
val colorTagRegex = Regex("""\{c:(#[0-9a-fA-F]{6,8})\}(.+?)\{/c\}""", RegexOption.DOT_MATCHES_ALL)
val fontTagRegex = Regex("""\{f:([\w.:\- ]+)\}(.+?)\{/f\}""", RegexOption.DOT_MATCHES_ALL)

/** Parses #RRGGBB / #AARRGGBB into a packed ARGB Long. */
fun parseHexColor(hex: String): Long? = runCatching {
    val clean = hex.removePrefix("#")
    when (clean.length) {
        6 -> 0xFF000000L or clean.toLong(16)
        8 -> clean.toULong(16).toLong()
        else -> null
    }
}.getOrNull()

fun styleMarkdown(
    source: String,
    styleSet: StyleSet,
    baseColor: Color,
    fontResolver: (String) -> FontFamily? = { null },
    displayFont: FontFamily? = null,
): AnnotatedString {
    val builder = AnnotatedString.Builder(source)
    var lineStart = 0
    // Iterate lines without copying the string.
    while (lineStart <= source.length) {
        val lineEnd = source.indexOf('\n', lineStart).let { if (it == -1) source.length else it }
        styleLine(builder, source, lineStart, lineEnd, styleSet, baseColor, displayFont)
        if (lineEnd == source.length) break
        lineStart = lineEnd + 1
    }
    // Inline tags can span lines, so they are applied on the whole text.
    val markerColor = baseColor.copy(alpha = 0.3f)
    colorTagRegex.findAll(source).forEach { m ->
        val color = parseHexColor(m.groupValues[1]) ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        builder.addStyle(SpanStyle(color = Color(color)), content.range.first, content.range.last + 1)
        builder.addStyle(SpanStyle(color = markerColor, fontSize = 0.6.em), m.range.first, content.range.first)
        builder.addStyle(SpanStyle(color = markerColor, fontSize = 0.6.em), content.range.last + 1, m.range.last + 1)
    }
    fontTagRegex.findAll(source).forEach { m ->
        val family = fontResolver(m.groupValues[1]) ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        builder.addStyle(SpanStyle(fontFamily = family), content.range.first, content.range.last + 1)
        builder.addStyle(SpanStyle(color = markerColor, fontSize = 0.6.em), m.range.first, content.range.first)
        builder.addStyle(SpanStyle(color = markerColor, fontSize = 0.6.em), content.range.last + 1, m.range.last + 1)
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
    displayFont: FontFamily? = null,
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
                fontFamily = displayFont,
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
