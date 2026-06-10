package com.stefanoneve.ultimatenotes.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Wraps the current selection with markdown markers (e.g. ** for bold).
 * With an empty selection the markers are inserted around the cursor.
 */
fun TextFieldValue.wrapSelection(prefix: String, suffix: String = prefix): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
    val cursor =
        if (selected.isEmpty()) start + prefix.length
        else end + prefix.length + suffix.length
    return copy(text = newText, selection = TextRange(cursor))
}

/**
 * Toggles a line prefix ("# ", "- ", "- [ ] ", "> ") on every line touched by
 * the selection. Existing block prefixes are swapped out.
 */
fun TextFieldValue.toggleLinePrefix(prefix: String): TextFieldValue {
    val knownPrefixes = listOf("### ", "## ", "# ", "- [x] ", "- [ ] ", "- ", "* ", "> ")
    val lines = text.split("\n").toMutableList()

    // Locate the line range covered by the selection.
    var charCount = 0
    var firstLine = 0
    var lastLine = 0
    for ((index, line) in lines.withIndex()) {
        val lineEnd = charCount + line.length
        if (selection.min in charCount..lineEnd) firstLine = index
        if (selection.max in charCount..lineEnd) lastLine = index
        charCount = lineEnd + 1
    }

    val allHaveIt = (firstLine..lastLine).all { lines[it].startsWith(prefix) }
    for (i in firstLine..lastLine) {
        var line = lines[i]
        val existing = knownPrefixes.firstOrNull { line.startsWith(it) }
        if (existing != null) line = line.removePrefix(existing)
        if (!allHaveIt) line = prefix + line
        lines[i] = line
    }
    val newText = lines.joinToString("\n")
    val delta = newText.length - text.length
    val cursor = (selection.max + delta).coerceIn(0, newText.length)
    return copy(text = newText, selection = TextRange(cursor))
}
