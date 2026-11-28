package com.warwickbuilds.tasks.core

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

/** A run of text with one set of styles. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val link: String? = null,
)

/** Markdown flattened into blocks that both Compose and the Glance widget can draw. */
sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock

    /** A list item. [line] is the 0-based source line, used to tick a checklist item. */
    data class Item(
        val spans: List<MdSpan>,
        val depth: Int,
        val marker: Marker,
        val line: Int,
    ) : MdBlock

    sealed interface Marker {
        data object Bullet : Marker
        data class Number(val n: Int) : Marker
        data class Check(val checked: Boolean) : Marker
    }
}

fun List<MdSpan>.plainText(): String = joinToString("") { it.text }

object Markdown {
    private val parser: Parser = Parser.builder()
        .extensions(listOf(TaskListItemsExtension.create(), StrikethroughExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS)
        .build()

    fun parse(markdown: String): List<MdBlock> {
        if (markdown.isBlank()) return emptyList()
        val out = mutableListOf<MdBlock>()
        var node = parser.parse(markdown).firstChild
        while (node != null) {
            block(node, depth = 0, out = out)
            node = node.next
        }
        return out
    }

    private fun block(node: Node, depth: Int, out: MutableList<MdBlock>) {
        when (node) {
            is Heading -> out += MdBlock.Heading(node.level, inlines(node))
            is Paragraph -> inlines(node).takeIf { it.isNotEmpty() }?.let { out += MdBlock.Paragraph(it) }
            is BlockQuote -> {
                val spans = mutableListOf<MdSpan>()
                children(node).forEachIndexed { i, child ->
                    if (i > 0) spans += MdSpan("\n")
                    spans += inlines(child)
                }
                out += MdBlock.Quote(spans)
            }
            is FencedCodeBlock -> out += MdBlock.Code(node.literal.trimEnd('\n'))
            is IndentedCodeBlock -> out += MdBlock.Code(node.literal.trimEnd('\n'))
            is HtmlBlock -> out += MdBlock.Paragraph(listOf(MdSpan(node.literal.trimEnd('\n'))))
            is ThematicBreak -> out += MdBlock.Rule
            is BulletList -> children(node).filterIsInstance<ListItem>().forEach {
                listItem(it, depth, MdBlock.Marker.Bullet, out)
            }
            is OrderedList -> {
                var n = node.markerStartNumber ?: 1
                children(node).filterIsInstance<ListItem>().forEach {
                    listItem(it, depth, MdBlock.Marker.Number(n++), out)
                }
            }
            else -> Unit
        }
    }

    private fun listItem(item: ListItem, depth: Int, defaultMarker: MdBlock.Marker, out: MutableList<MdBlock>) {
        val check = children(item).filterIsInstance<TaskListItemMarker>().firstOrNull()
        val marker = check?.let { MdBlock.Marker.Check(it.isChecked) } ?: defaultMarker
        val line = item.sourceSpans.firstOrNull()?.lineIndex ?: -1
        val spans = mutableListOf<MdSpan>()
        val nested = mutableListOf<Node>()
        for (child in children(item)) {
            when (child) {
                is TaskListItemMarker -> Unit
                is BulletList, is OrderedList -> nested += child
                else -> {
                    if (spans.isNotEmpty()) spans += MdSpan(" ")
                    spans += inlines(child)
                }
            }
        }
        out += MdBlock.Item(spans, depth, marker, line)
        nested.forEach { block(it, depth + 1, out) }
    }

    private fun inlines(node: Node): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        fun walk(n: Node, style: MdSpan) {
            when (n) {
                is Text -> spans += style.copy(text = n.literal)
                is Code -> spans += style.copy(text = n.literal, code = true)
                is SoftLineBreak -> spans += style.copy(text = " ")
                is HardLineBreak -> spans += style.copy(text = "\n")
                is HtmlInline -> spans += style.copy(text = n.literal)
                is Emphasis -> children(n).forEach { walk(it, style.copy(italic = true)) }
                is StrongEmphasis -> children(n).forEach { walk(it, style.copy(bold = true)) }
                is Strikethrough -> children(n).forEach { walk(it, style.copy(strike = true)) }
                is Link -> children(n).forEach { walk(it, style.copy(link = n.destination)) }
                is Image -> children(n).forEach { walk(it, style) }
                is Paragraph -> children(n).forEach { walk(it, style) }
                else -> children(n).forEach { walk(it, style) }
            }
        }
        children(node).forEach { walk(it, MdSpan("")) }
        return merge(spans)
    }

    private fun merge(spans: List<MdSpan>): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        for (s in spans) {
            if (s.text.isEmpty()) continue
            val last = out.lastOrNull()
            if (last != null && last.copy(text = "") == s.copy(text = "")) {
                out[out.size - 1] = last.copy(text = last.text + s.text)
            } else {
                out += s
            }
        }
        return out
    }

    private fun children(node: Node): List<Node> {
        val list = mutableListOf<Node>()
        var c = node.firstChild
        while (c != null) {
            list += c
            c = c.next
        }
        return list
    }

    private val checkbox = Regex("""^(\s*(?:[-*+]|\d+[.)])\s+)\[([ xX])]""")

    /** Ticks or unticks the checklist item on source [line]. Returns [markdown] unchanged if there isn't one. */
    fun toggleCheck(markdown: String, line: Int): String {
        val lines = markdown.split("\n").toMutableList()
        if (line !in lines.indices) return markdown
        val m = checkbox.find(lines[line]) ?: return markdown
        val ticked = m.groupValues[2] != " "
        lines[line] = lines[line].replaceRange(m.range, "${m.groupValues[1]}[${if (ticked) " " else "x"}]")
        return lines.joinToString("\n")
    }

    /** A one-line plain-text preview for list rows. */
    fun preview(markdown: String, maxLength: Int = 160): String {
        val text = parse(markdown).mapNotNull { block ->
            when (block) {
                is MdBlock.Heading -> block.spans.plainText()
                is MdBlock.Paragraph -> block.spans.plainText()
                is MdBlock.Quote -> block.spans.plainText()
                is MdBlock.Item -> block.spans.plainText()
                is MdBlock.Code -> block.text
                MdBlock.Rule -> null
            }?.replace('\n', ' ')?.trim()?.takeIf { it.isNotEmpty() }
        }.joinToString(" · ")
        return if (text.length > maxLength) text.take(maxLength - 1).trimEnd() + "…" else text
    }
}
