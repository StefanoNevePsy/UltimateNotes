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
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
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
val colorTagRegex = Regex("""\{c:(#[0-9a-fA-F]{6,8}|@\d{1,2})\}(.+?)\{/c\}""", RegexOption.DOT_MATCHES_ALL)
val fontTagRegex = Regex("""\{f:([\w.:\- ]+)\}(.+?)\{/f\}""", RegexOption.DOT_MATCHES_ALL)
val sizeTagRegex = Regex("""\{s:(\d{1,3}(?:\.\d+)?)\}(.+?)\{/s\}""", RegexOption.DOT_MATCHES_ALL)

/** Ordered-list prefix at the start of a line: "1. ", "12) " … */
val numberedListRegex = Regex("""^\d{1,3}[.)]\s""")

/** One nesting level of a list, encoded as leading spaces in the text. */
const val LIST_INDENT_UNIT = "    "

/** Structure of a list line: leading indent + the bullet/number marker. */
data class ListInfo(val indent: String, val marker: String) {
    val prefixLength: Int get() = indent.length + marker.length
}

/** Parses a line as a (possibly nested) list item, or null if it isn't one. */
fun parseListLine(line: String): ListInfo? {
    val indent = line.takeWhile { it == ' ' }
    val rest = line.substring(indent.length)
    val marker = when {
        rest.startsWith("- [ ] ") || rest.startsWith("- [x] ") -> rest.substring(0, 6)
        rest.startsWith("- ") || rest.startsWith("* ") -> rest.substring(0, 2)
        else -> numberedListRegex.find(rest)?.value
    } ?: return null
    return ListInfo(indent, marker)
}

/**
 * Markdown markers are kept in the source but visually collapsed in the
 * rendered (non-editing) text, so blocks read like a word processor output.
 */
private val HiddenMarker = SpanStyle(color = Color.Transparent, fontSize = 0.1.em)

/** Parses #RRGGBB / #AARRGGBB into a packed ARGB Long. */
fun parseHexColor(hex: String): Long? = runCatching {
    val clean = hex.removePrefix("#")
    when (clean.length) {
        6 -> 0xFF000000L or clean.toLong(16)
        8 -> clean.toULong(16).toLong()
        else -> null
    }
}.getOrNull()

/** Parses a color-tag token: "#RRGGBB" fixed, or "@N" theme slot. */
fun parseColorToken(token: String, roleColors: List<Long>): Long? =
    if (token.startsWith("@")) {
        token.drop(1).toIntOrNull()?.let { n ->
            if (roleColors.isEmpty()) null
            else roleColors[((n - 1).coerceAtLeast(0)) % roleColors.size]
        }
    } else parseHexColor(token)

fun styleMarkdown(
    source: String,
    styleSet: StyleSet,
    baseColor: Color,
    fontResolver: (String) -> FontFamily? = { null },
    displayFont: FontFamily? = null,
    roleColors: List<Long> = emptyList(),
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
    colorTagRegex.findAll(source).forEach { m ->
        val color = parseColorToken(m.groupValues[1], roleColors) ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        builder.addStyle(SpanStyle(color = Color(color)), content.range.first, content.range.last + 1)
        builder.addStyle(HiddenMarker, m.range.first, content.range.first)
        builder.addStyle(HiddenMarker, content.range.last + 1, m.range.last + 1)
    }
    fontTagRegex.findAll(source).forEach { m ->
        val family = fontResolver(m.groupValues[1]) ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        builder.addStyle(SpanStyle(fontFamily = family), content.range.first, content.range.last + 1)
        builder.addStyle(HiddenMarker, m.range.first, content.range.first)
        builder.addStyle(HiddenMarker, content.range.last + 1, m.range.last + 1)
    }
    sizeTagRegex.findAll(source).forEach { m ->
        val size = m.groupValues[1].toFloatOrNull() ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        builder.addStyle(
            SpanStyle(fontSize = size.coerceIn(6f, 120f).sp),
            content.range.first, content.range.last + 1,
        )
        builder.addStyle(HiddenMarker, m.range.first, content.range.first)
        builder.addStyle(HiddenMarker, content.range.last + 1, m.range.last + 1)
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
    val hidden = HiddenMarker

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
        span(hidden, 0, headerLevel + 1)
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
        span(hidden, 0, 2)
    }
    // List markers (bullets, numbers, checkboxes) are content, not syntax:
    // render them at full text color so they stay readable on dark themes.
    val listMarker = SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)
    parseListLine(line)?.let { info ->
        val mStart = info.indent.length
        val mEnd = mStart + info.marker.length
        span(listMarker, mStart, mEnd)
        if (info.marker == "- [x] ") {
            span(
                SpanStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = baseColor.copy(alpha = 0.5f),
                ),
                mEnd,
                line.length,
            )
        }
        // Hanging indent: wrapped lines align with the text after the marker
        // (the leading spaces already indent the nesting level). The style
        // must cover the trailing newline too, otherwise Compose splits it
        // into a stray paragraph and leaves a big gap.
        val bodySize = styleSet.byId("body").fontSize
        val hang = info.prefixLength * bodySize * 0.6f
        val pEnd = if (end < source.length) end + 1 else end
        builder.addStyle(
            ParagraphStyle(textIndent = TextIndent(firstLine = 0.sp, restLine = hang.sp)),
            start,
            pEnd,
        )
    }

    boldRegex.findAll(line).forEach { m ->
        span(SpanStyle(fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
        span(hidden, m.range.first, m.range.first + 2)
        span(hidden, m.range.last - 1, m.range.last + 1)
    }
    italicRegex.findAll(line).forEach { m ->
        span(SpanStyle(fontStyle = FontStyle.Italic), m.range.first, m.range.last + 1)
        span(hidden, m.range.first, m.range.first + 1)
        span(hidden, m.range.last, m.range.last + 1)
    }
    strikeRegex.findAll(line).forEach { m ->
        span(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            m.range.first,
            m.range.last + 1,
        )
        span(hidden, m.range.first, m.range.first + 2)
        span(hidden, m.range.last - 1, m.range.last + 1)
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
        span(hidden, m.range.first, m.range.first + 1)
        span(hidden, m.range.last, m.range.last + 1)
    }
}

/** Base text style for a text block, derived from its paragraph style. */
fun baseTextStyle(
    styleSet: StyleSet,
    styleId: String,
    fontFamily: FontFamily,
    color: Color,
    sizeOverride: Float? = null,
): TextStyle {
    val def = styleSet.byId(styleId)
    val size = sizeOverride ?: def.fontSize
    return TextStyle(
        fontSize = size.sp,
        fontWeight = FontWeight(def.fontWeight),
        fontFamily = fontFamily,
        color = color,
        lineHeight = (size * 1.3f).sp,
        // Without this, splitting a list into per-line paragraphs (for the
        // hanging indent) adds font padding above/below each one, leaving big
        // gaps between bullets. Disable it so spacing matches the editor.
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        // Trim the half-leading at each paragraph's top/bottom edge. Since
        // every list item is its own paragraph (for the hanging indent),
        // without this the gaps between bullets stack up into big blank space.
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
}
