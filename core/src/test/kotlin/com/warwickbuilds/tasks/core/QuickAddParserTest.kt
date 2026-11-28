package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddParserTest {
    private val tags = StarterTags.all

    @Test fun firstWordMatches() {
        val r = QuickAddParser.parse("#books Dune", tags)
        assertEquals("Dune", r.title)
        assertEquals(setOf("books"), r.tagIds)
        assertEquals(emptyList<String>(), r.newTagNames)
    }

    @Test fun tagsAnywhereAndCaseInsensitive() {
        val r = QuickAddParser.parse("Watch #MOVIES Arrival #today", tags)
        assertEquals("Watch Arrival", r.title)
        assertEquals(setOf("movies", "today"), r.tagIds)
    }

    @Test fun wholeNameAndPrefixMatch() {
        assertEquals(setOf("tv"), QuickAddParser.parse("Severance #tvshows", tags).tagIds)
        assertEquals(setOf("projects"), QuickAddParser.parse("Shed #proj", tags).tagIds)
        assertEquals(setOf("tv"), QuickAddParser.parse("Severance #tv", tags).tagIds)
    }

    @Test fun unknownTagsBecomeNewTags() {
        val r = QuickAddParser.parse("Milk #groceries #date-night #Groceries", tags)
        assertEquals("Milk", r.title)
        assertEquals(listOf("Groceries", "Date night"), r.newTagNames)
    }

    @Test fun hashInsideAWordIsNotATag() {
        val r = QuickAddParser.parse("Learn C# today", tags)
        assertEquals("Learn C# today", r.title)
        assertEquals(emptySet<String>(), r.tagIds)
    }

    @Test fun shortPrefixesDoNotMatch() {
        val r = QuickAddParser.parse("x #pr", tags)
        assertEquals(listOf("Pr"), r.newTagNames)
    }
}
