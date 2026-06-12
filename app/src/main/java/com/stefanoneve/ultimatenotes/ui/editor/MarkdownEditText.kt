package com.stefanoneve.ultimatenotes.ui.editor

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.TextPaint
import android.util.TypedValue
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.model.StyleSet

/**
 * Inline span that paints text with an arbitrary [Typeface] (TypefaceSpan
 * with a Typeface argument requires API 28; this works from minSdk).
 */
private class FontSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = typeface
    }
}

/**
 * Bridge used by the format bar to manipulate the native editor: wraps
 * markdown markers or inline tags around the current selection.
 */
class MarkdownEditController {
    internal var editText: AppCompatEditText? = null

    private fun current(): TextFieldValue? {
        val edit = editText ?: return null
        return TextFieldValue(
            edit.text?.toString().orEmpty(),
            TextRange(
                edit.selectionStart.coerceAtLeast(0),
                edit.selectionEnd.coerceAtLeast(0),
            ),
        )
    }

    private fun apply(value: TextFieldValue) {
        val edit = editText ?: return
        edit.text?.replace(0, edit.text?.length ?: 0, value.text)
        edit.setSelection(
            value.selection.start.coerceIn(0, value.text.length),
            value.selection.end.coerceIn(0, value.text.length),
        )
    }

    /**
     * Word-processor-style toggle:
     *  - selection wrapped in the markers → unwrap;
     *  - selection present → wrap;
     *  - no selection, cursor right before the closing marker → step out
     *    ("turn the style off" and keep typing normally);
     *  - no selection → open an empty pair with the cursor inside.
     */
    fun wrap(prefix: String, suffix: String = prefix) {
        val value = current() ?: return
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        if (start == end) {
            if (text.startsWith(suffix, start)) {
                // Step out of the style.
                editText?.setSelection(
                    (start + suffix.length).coerceAtMost(text.length),
                )
                return
            }
            apply(value.wrapSelection(prefix, suffix))
            return
        }
        // Unwrap if the selection (or its surroundings) already carries the markers.
        val selected = text.substring(start, end)
        when {
            selected.startsWith(prefix) && selected.endsWith(suffix) &&
                selected.length >= prefix.length + suffix.length -> {
                val inner = selected.substring(
                    prefix.length, selected.length - suffix.length,
                )
                val newText = text.substring(0, start) + inner + text.substring(end)
                apply(
                    TextFieldValue(
                        newText,
                        TextRange(start, start + inner.length),
                    ),
                )
            }
            start >= prefix.length &&
                text.regionMatches(start - prefix.length, prefix, 0, prefix.length) &&
                text.regionMatches(end, suffix, 0, suffix.length) -> {
                val newText = text.substring(0, start - prefix.length) +
                    selected + text.substring(end + suffix.length)
                apply(
                    TextFieldValue(
                        newText,
                        TextRange(start - prefix.length, end - prefix.length),
                    ),
                )
            }
            else -> apply(value.wrapSelection(prefix, suffix))
        }
    }

    fun toggleLinePrefix(prefix: String) {
        current()?.let { apply(it.toggleLinePrefix(prefix)) }
    }

    fun hasSelection(): Boolean =
        editText?.let { it.selectionStart != it.selectionEnd } == true
}

private class EditorState(
    var onTextChanged: (String) -> Unit,
    var onReceiveImage: (Uri) -> Unit,
    var styleSet: StyleSet,
    var baseColor: Int,
    var fontManager: FontManager,
    var displayTypeface: Typeface,
    var roleColors: List<Long>,
) {
    var selfChange = false
    val appliedSpans = mutableListOf<Any>()
}

/**
 * Native markdown editor. An EditText is used instead of a Compose text field
 * so that Samsung Keyboard stickers / AI drawing assist sketches arrive via
 * the IME commitContent API (ViewCompat.setOnReceiveContentListener declares
 * the image MIME types in EditorInfo.contentMimeTypes — without it the
 * keyboard shows "cannot paste here"). Markdown is styled live with spans.
 */
@Composable
fun MarkdownTextEditor(
    text: String,
    onTextChanged: (String) -> Unit,
    styleSet: StyleSet,
    styleId: String,
    sizeOverride: Float? = null,
    baseColor: Int,
    baseTypeface: Typeface,
    displayTypeface: Typeface,
    fontManager: FontManager,
    roleColors: List<Long> = emptyList(),
    controller: MarkdownEditController,
    onReceiveImage: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember {
        EditorState(
            onTextChanged, onReceiveImage, styleSet, baseColor, fontManager,
            displayTypeface, roleColors,
        )
    }
    state.onTextChanged = onTextChanged
    state.onReceiveImage = onReceiveImage
    state.styleSet = styleSet
    state.baseColor = baseColor
    state.fontManager = fontManager
    state.displayTypeface = displayTypeface
    state.roleColors = roleColors

    AndroidView(
        modifier = modifier,
        factory = { context -> createEditor(context, state, controller) },
        update = { edit ->
            val def = styleSet.byId(styleId)
            val baseSize = sizeOverride ?: def.fontSize
            if (edit.textSize != spToPx(edit.context, baseSize)) {
                edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize)
            }
            if (edit.currentTextColor != baseColor) edit.setTextColor(baseColor)
            if (edit.typeface != baseTypeface) edit.typeface = baseTypeface
            val currentText = edit.text?.toString().orEmpty()
            if (currentText != text) {
                // External change (undo/redo): sync preserving the cursor.
                state.selfChange = true
                val sel = edit.selectionStart
                edit.setText(text)
                edit.setSelection(sel.coerceIn(0, text.length))
                state.selfChange = false
                edit.text?.let { applyMarkdownSpans(it, state, edit) }
            }
        },
    )
}

private fun spToPx(context: Context, sp: Float): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics,
    )

private fun createEditor(
    context: Context,
    state: EditorState,
    controller: MarkdownEditController,
): AppCompatEditText {
    val edit = AppCompatEditText(context)
    edit.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
    edit.background = null
    edit.setPadding(8, 8, 8, 8)
    edit.inputType = InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    controller.editText = edit

    // Receive images from the keyboard (stickers, AI drawing assist),
    // clipboard paste and drag & drop.
    ViewCompat.setOnReceiveContentListener(
        edit,
        arrayOf("image/*"),
    ) { _, payload ->
        val split = payload.partition { item -> item.uri != null }
        split.first?.let { imagePayload ->
            val clip = imagePayload.clip
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let(state.onReceiveImage)
            }
        }
        split.second
    }

    edit.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            if (s == null) return
            applyMarkdownSpans(s, state, edit)
            if (!state.selfChange) state.onTextChanged(s.toString())
        }
    })

    edit.isFocusable = true
    edit.isFocusableInTouchMode = true

    fun showKeyboard() {
        edit.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
    }
    edit.post {
        edit.setSelection(edit.text?.length ?: 0)
        showKeyboard()
    }
    // The IME sometimes ignores the first request while the Compose focus
    // system is still settling; retry shortly after.
    edit.postDelayed({ if (!edit.hasWindowFocus() || !edit.isFocused) showKeyboard() }, 250)
    return edit
}

/** Re-applies live markdown styling spans owned by the editor. */
private fun applyMarkdownSpans(
    editable: Editable,
    state: EditorState,
    edit: AppCompatEditText,
) {
    state.appliedSpans.forEach { editable.removeSpan(it) }
    state.appliedSpans.clear()

    fun span(what: Any, start: Int, end: Int) {
        if (start in 0..end && end <= editable.length) {
            editable.setSpan(what, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            state.appliedSpans.add(what)
        }
    }

    val base = state.baseColor
    // Markers stay editable but nearly disappear: tiny and very faint.
    val dim = (base and 0x00FFFFFF) or (0x30 shl 24)
    fun markerSpans(s: Int, e: Int) {
        span(ForegroundColorSpan(dim), s, e)
        span(RelativeSizeSpan(0.45f), s, e)
    }
    val text = editable.toString()
    val baseSize = state.styleSet.byId("body").fontSize

    var lineStart = 0
    while (lineStart <= text.length) {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val headerLevel = when {
            line.startsWith("### ") -> 3
            line.startsWith("## ") -> 2
            line.startsWith("# ") -> 1
            else -> 0
        }
        if (headerLevel > 0) {
            val def = state.styleSet.byId("title$headerLevel")
            span(
                AbsoluteSizeSpan(spToPx(edit.context, def.fontSize).toInt()),
                lineStart, lineEnd,
            )
            span(StyleSpan(Typeface.BOLD), lineStart, lineEnd)
            span(FontSpan(state.displayTypeface), lineStart, lineEnd)
            markerSpans(lineStart, lineStart + headerLevel + 1)
        }
        if (line.startsWith("> ")) {
            span(StyleSpan(Typeface.ITALIC), lineStart, lineEnd)
            markerSpans(lineStart, lineStart + 2)
        }
        if (line.startsWith("- [ ] ") || line.startsWith("- [x] ")) {
            span(ForegroundColorSpan(dim), lineStart, lineStart + 6)
            if (line.startsWith("- [x] ")) {
                span(StrikethroughSpan(), lineStart + 6, lineEnd)
            }
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            span(ForegroundColorSpan(dim), lineStart, lineStart + 2)
        }

        fun spanAll(regex: Regex, makeSpans: () -> List<Any>, markerLen: Int) {
            regex.findAll(line).forEach { m ->
                val s = lineStart + m.range.first
                val e = lineStart + m.range.last + 1
                makeSpans().forEach { span(it, s, e) }
                markerSpans(s, s + markerLen)
                markerSpans(e - markerLen, e)
            }
        }
        spanAll(boldRegex, { listOf(StyleSpan(Typeface.BOLD)) }, 2)
        spanAll(italicRegex, { listOf(StyleSpan(Typeface.ITALIC)) }, 1)
        spanAll(strikeRegex, { listOf(StrikethroughSpan()) }, 2)
        codeRegex.findAll(line).forEach { m ->
            val s = lineStart + m.range.first
            val e = lineStart + m.range.last + 1
            span(FontSpan(Typeface.MONOSPACE), s, e)
            span(BackgroundColorSpan((base and 0x00FFFFFF) or (0x14 shl 24)), s, e)
        }

        if (lineEnd == text.length) break
        lineStart = lineEnd + 1
    }

    // Inline color / font / size tags (may span multiple lines).
    colorTagRegex.findAll(text).forEach { m ->
        val color = parseColorToken(m.groupValues[1], state.roleColors) ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        span(ForegroundColorSpan(color.toInt()), content.range.first, content.range.last + 1)
        markerSpans(m.range.first, content.range.first)
        markerSpans(content.range.last + 1, m.range.last + 1)
    }
    fontTagRegex.findAll(text).forEach { m ->
        val content = m.groups[2] ?: return@forEach
        val typeface = state.fontManager.typefaceOf(m.groupValues[1])
        span(FontSpan(typeface), content.range.first, content.range.last + 1)
        markerSpans(m.range.first, content.range.first)
        markerSpans(content.range.last + 1, m.range.last + 1)
    }
    sizeTagRegex.findAll(text).forEach { m ->
        val size = m.groupValues[1].toFloatOrNull() ?: return@forEach
        val content = m.groups[2] ?: return@forEach
        span(
            AbsoluteSizeSpan(spToPx(edit.context, size.coerceIn(6f, 120f)).toInt()),
            content.range.first, content.range.last + 1,
        )
        markerSpans(m.range.first, content.range.first)
        markerSpans(content.range.last + 1, m.range.last + 1)
    }
}
