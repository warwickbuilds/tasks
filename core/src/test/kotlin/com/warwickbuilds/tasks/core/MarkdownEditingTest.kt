package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownEditingTest {
    @Test fun wrapsAndUnwrapsSelection() {
        val bold = MarkdownEditing.wrap(EditState("make this bold", 10, 14), "**")
        assertEquals(EditState("make this **bold**", 12, 16), bold)
        assertEquals(EditState("make this bold", 10, 14), MarkdownEditing.wrap(bold, "**"))
    }

    @Test fun wrapWithNoSelectionPutsCursorInside() {
        assertEquals(EditState("a __", 3, 3), MarkdownEditing.wrap(EditState("a ", 2), "_"))
    }

    @Test fun linkSelectsTheUrl() {
        val r = MarkdownEditing.link(EditState("see docs", 4, 8))
        assertEquals("see [docs](https://)", r.text)
        assertEquals("https://", r.text.substring(r.start, r.end))
    }

    @Test fun prefixesAndUnprefixesLines() {
        val r = MarkdownEditing.prefixLines(EditState("one\ntwo\nthree", 1, 6), "- [ ] ")
        assertEquals("- [ ] one\n- [ ] two\nthree", r.text)
        assertEquals("one\ntwo\nthree", MarkdownEditing.prefixLines(r, "- [ ] ").text)
    }

    @Test fun prefixReplacesAnotherListMarker() {
        assertEquals("## Title", MarkdownEditing.prefixLines(EditState("- Title", 3), "## ").text)
        assertEquals("- [ ] milk", MarkdownEditing.prefixLines(EditState("- milk", 0), "- [ ] ").text)
    }

    @Test fun enterContinuesLists() {
        val prev = "- [x] milk"
        val typed = EditState("- [x] milk\n", 11)
        assertEquals(EditState("- [x] milk\n- [ ] ", 17), MarkdownEditing.continueList(prev, typed))
        assertEquals(EditState("1. a\n2. ", 8), MarkdownEditing.continueList("1. a", EditState("1. a\n", 5)))
        assertEquals(EditState("  - a\n  - ", 10), MarkdownEditing.continueList("  - a", EditState("  - a\n", 6)))
    }

    @Test fun enterOnEmptyItemEndsTheList() {
        assertEquals(EditState("- a\n", 4), MarkdownEditing.continueList("- a\n- ", EditState("- a\n- \n", 7)))
    }

    @Test fun plainTextIsLeftAlone() {
        assertNull(MarkdownEditing.continueList("hello", EditState("hello\n", 6)))
    }
}
