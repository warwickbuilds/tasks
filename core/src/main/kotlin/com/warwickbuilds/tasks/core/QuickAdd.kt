package com.warwickbuilds.tasks.core

/** The result of reading `#tags` out of a typed title, e.g. "#books Dune". */
data class QuickAdd(
    val title: String,
    val tagIds: Set<String>,
    val newTagNames: List<String>,
)

object QuickAddParser {
    private val tagToken = Regex("""(?<=^|\s)#([\p{L}\p{N}_-]+)""")
    private val spaces = Regex("""\s+""")

    fun slug(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Pulls `#word` tokens out of [input]. Each token matches an existing tag by its whole
     * name ("#tvshows"), its first word ("#books", "#movies") or, from three letters, the
     * start of its name ("#proj"). Anything else becomes a new tag.
     */
    fun parse(input: String, tags: List<Tag>): QuickAdd {
        val ids = linkedSetOf<String>()
        val newNames = mutableListOf<String>()
        for (match in tagToken.findAll(input)) {
            val token = match.groupValues[1]
            val tag = matchTag(token, tags)
            when {
                tag != null -> ids += tag.id
                slug(token).isEmpty() -> Unit
                newNames.none { slug(it) == slug(token) } -> newNames += newTagName(token)
            }
        }
        val title = tagToken.replace(input, "").replace(spaces, " ").trim()
        return QuickAdd(title, ids, newNames)
    }

    fun matchTag(token: String, tags: List<Tag>): Tag? {
        val s = slug(token)
        if (s.isEmpty()) return null
        val ordered = tags.sortedWith(Grouping.tagOrder)
        return ordered.firstOrNull { slug(it.name) == s }
            ?: ordered.firstOrNull { slug(it.name.trim().substringBefore(' ')) == s }
            ?: ordered.firstOrNull { s.length >= 3 && slug(it.name).startsWith(s) }
    }

    /** "#date-night" becomes "Date night". */
    fun newTagName(token: String): String {
        val words = token.replace('_', ' ').replace('-', ' ').replace(spaces, " ").trim()
        return words.replaceFirstChar { it.titlecase() }
    }
}
