package com.warwickbuilds.tasks.core

/** Text plus a selection, for the editor toolbar. Selection ends are clamped to the text. */
data class EditState(val text: String, val start: Int, val end: Int = start) {
    val from: Int get() = minOf(start, end).coerceIn(0, text.length)
    val to: Int get() = maxOf(start, end).coerceIn(0, text.length)
}

/** The editor toolbar's actions, kept free of Android so they can be unit tested. */
object MarkdownEditing {

    /** Wraps the selection in [marker] (bold `**`, italic `_`, code `` ` ``), or unwraps it if it already is. */
    fun wrap(state: EditState, marker: String): EditState {
        val (text, from, to) = Triple(state.text, state.from, state.to)
        val before = text.substring(0, from)
        val selected = text.substring(from, to)
        val after = text.substring(to)
        if (before.endsWith(marker) && after.startsWith(marker)) {
            val newText = before.dropLast(marker.length) + selected + after.drop(marker.length)
            return EditState(newText, from - marker.length, to - marker.length)
        }
        val newText = before + marker + selected + marker + after
        return EditState(newText, from + marker.length, to + marker.length)
    }

    /** Wraps the selection as a link and selects the URL placeholder. */
    fun link(state: EditState): EditState {
        val (text, from, to) = Triple(state.text, state.from, state.to)
        val label = text.substring(from, to).ifEmpty { "link" }
        val url = "https://"
        val insert = "[$label]($url)"
        val newText = text.substring(0, from) + insert + text.substring(to)
        val urlStart = from + label.length + 3
        return EditState(newText, urlStart, urlStart + url.length)
    }

    /**
     * Adds [prefix] ("## ", "- ", "- [ ] ") to the start of each selected line, or removes it
     * if every selected line already has it. A list or heading prefix replaces another one.
     */
    fun prefixLines(state: EditState, prefix: String): EditState {
        val text = state.text
        val lineStart = text.lastIndexOf('\n', state.from - 1).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', state.to).let { if (it < 0) text.length else it }
        val lines = text.substring(lineStart, lineEnd).split("\n")
        val removing = lines.all { it.startsWith(prefix) }
        var delta = 0
        var firstDelta = 0
        val changed = lines.mapIndexed { i, line ->
            val updated = if (removing) {
                line.removePrefix(prefix)
            } else {
                prefix + existingPrefix.replace(line, "")
            }
            if (i == 0) firstDelta = updated.length - line.length
            delta += updated.length - line.length
            updated
        }.joinToString("\n")
        val newText = text.substring(0, lineStart) + changed + text.substring(lineEnd)
        val newFrom = (state.from + firstDelta).coerceIn(lineStart, newText.length)
        val newTo = (state.to + delta).coerceIn(newFrom, newText.length)
        return EditState(newText, newFrom, newTo)
    }

    private val existingPrefix = Regex("""^(#{1,6} |[-*+] \[[ xX]] |[-*+] |\d+[.)] )""")

    /**
     * When Enter is pressed at the end of a list line, carries the list on ("- ", "- [ ] ", "2. ").
     * On an empty list line, ends the list instead. Returns null when nothing special applies.
     */
    fun continueList(previous: String, current: EditState): EditState? {
        val text = current.text
        val cursor = current.from
        if (current.from != current.to || text.length != previous.length + 1) return null
        if (cursor == 0 || text[cursor - 1] != '\n') return null
        val lineStart = text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
        val line = text.substring(lineStart, cursor - 1)
        val m = listLine.find(line) ?: return null
        val indent = m.groupValues[1]
        val bullet = m.groupValues[2]
        val body = line.substring(m.range.last + 1)
        if (body.isBlank()) {
            // Empty item: drop the marker and the new line.
            val newText = text.substring(0, lineStart) + text.substring(cursor)
            return EditState(newText, lineStart)
        }
        val next = when {
            bullet.endsWith("]") -> indent + bullet.take(1) + " [ ] "
            bullet.first().isDigit() -> {
                val n = bullet.takeWhile { it.isDigit() }.toInt()
                indent + (n + 1) + bullet.dropWhile { it.isDigit() }.take(1) + " "
            }
            else -> "$indent$bullet "
        }
        val newText = text.substring(0, cursor) + next + text.substring(cursor)
        return EditState(newText, cursor + next.length)
    }

    private val listLine = Regex("""^(\s*)([-*+] \[[ xX]]|[-*+]|\d+[.)]) """)
}
