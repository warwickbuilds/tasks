package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    private val sample = """
        ## Before Friday
        - [x] Book the cabin
        - [ ] Check **tyre** pressure

        ## Pack
        - Wetsuits
          - Fins
        1. First
        2. Second

        Key is in the _lockbox_. See [map](https://example.com) and `14 Dune Rd`. ~~old~~
    """.trimIndent()

    @Test fun parsesBlocks() {
        val blocks = Markdown.parse(sample)
        assertEquals(MdBlock.Heading(2, listOf(MdSpan("Before Friday"))), blocks[0])
        val cabin = blocks[1] as MdBlock.Item
        assertEquals(MdBlock.Marker.Check(true), cabin.marker)
        assertEquals(1, cabin.line)
        val tyre = blocks[2] as MdBlock.Item
        assertEquals(MdBlock.Marker.Check(false), tyre.marker)
        assertEquals(listOf(MdSpan("Check "), MdSpan("tyre", bold = true), MdSpan(" pressure")), tyre.spans)
        val fins = blocks.filterIsInstance<MdBlock.Item>().first { it.spans.plainText() == "Fins" }
        assertEquals(1, fins.depth)
        assertEquals(MdBlock.Marker.Bullet, fins.marker)
        val second = blocks.filterIsInstance<MdBlock.Item>().first { it.spans.plainText() == "Second" }
        assertEquals(MdBlock.Marker.Number(2), second.marker)
        val para = blocks.last() as MdBlock.Paragraph
        assertTrue(para.spans.contains(MdSpan("lockbox", italic = true)))
        assertTrue(para.spans.contains(MdSpan("map", link = "https://example.com")))
        assertTrue(para.spans.contains(MdSpan("14 Dune Rd", code = true)))
        assertTrue(para.spans.contains(MdSpan("old", strike = true)))
    }

    @Test fun togglesChecklistLines() {
        val once = Markdown.toggleCheck(sample, 2)
        assertTrue(once.lines()[2].startsWith("- [x] Check"))
        val twice = Markdown.toggleCheck(once, 1)
        assertTrue(twice.lines()[1].startsWith("- [ ] Book"))
        assertEquals(sample, Markdown.toggleCheck(sample, 0))
        assertEquals(sample, Markdown.toggleCheck(sample, 99))
    }

    @Test fun toggledLineMatchesParsedLine() {
        val item = Markdown.parse(sample).filterIsInstance<MdBlock.Item>()[1]
        val toggled = Markdown.parse(Markdown.toggleCheck(sample, item.line)).filterIsInstance<MdBlock.Item>()[1]
        assertEquals(MdBlock.Marker.Check(true), toggled.marker)
    }

    @Test fun previewIsOneLine() {
        assertEquals("Washer kit is in the garage", Markdown.preview("Washer kit is in the **garage**"))
        assertEquals("A · B · C", Markdown.preview("# A\n\n- B\n- [ ] C"))
        assertEquals("", Markdown.preview("   "))
        assertTrue(Markdown.preview("x".repeat(500)).length <= 160)
    }
}
