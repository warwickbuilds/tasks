package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val tags = StarterTags.all
    private val tasks = listOf(
        Task("1", "Dune", "- [ ] buy", LocalDate.of(2026, 12, 1), pinned = true, createdAt = 1, updatedAt = 2, tagIds = setOf("books", "today")),
        Task("2", "Tap", createdAt = 3, completedAt = 9),
    )

    @Test fun roundTrips() {
        val json = Backup.encode(tasks, tags, "2026-11-27T06:00:00Z")
        val (t, g) = Backup.decode(json)
        assertEquals(tasks, t)
        assertEquals(tags, g)
    }

    @Test fun rejectsOtherFiles() {
        listOf("{}", "nope", """{"app":"cards","format":1,"exported_at":"x","tags":[],"tasks":[]}""").forEach {
            try {
                Backup.decode(it)
                throw AssertionError("should fail: $it")
            } catch (e: BackupFormatException) {
                // expected
            }
        }
    }

    @Test fun rejectsNewerFormat() {
        try {
            Backup.decode("""{"app":"tasks","format":99,"exported_at":"x","tags":[],"tasks":[]}""")
            throw AssertionError("should fail")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }

    @Test fun mergeAddsKeepsNewestAndMapsTagsByName() {
        val existing = listOf(Task("1", "Old title", createdAt = 1, updatedAt = 5), Task("p", "Pinned here", pinned = true, createdAt = 1))
        val incomingTags = listOf(Tag("x9", "books to read", 3, 0), Tag("new", "Gardening", 2, 1))
        val incoming = listOf(
            Task("1", "Newer title", createdAt = 1, updatedAt = 6),
            Task("3", "Seeds", createdAt = 2, pinned = true, tagIds = setOf("x9", "new")),
        )
        val r = Backup.merge(existing, tags, incoming, incomingTags)
        assertEquals(1, r.added)
        assertEquals(1, r.updated)
        val byId = r.tasks.associateBy { it.id }
        assertEquals("Newer title", byId["1"]!!.title)
        assertEquals(setOf("books", "new"), byId["3"]!!.tagIds)
        assertEquals(listOf("p"), r.tasks.filter { it.pinned }.map { it.id })
        assertEquals(tags.size + 1, r.tags.size)
        assertEquals(tags.maxOf { it.sortOrder } + 1, r.tags.first { it.id == "new" }.sortOrder)
    }

    @Test fun mergeIgnoresOlderCopies() {
        val existing = listOf(Task("1", "Keep", createdAt = 1, updatedAt = 9))
        val r = Backup.merge(existing, tags, listOf(Task("1", "Stale", createdAt = 1, updatedAt = 2)), emptyList())
        assertEquals("Keep", r.tasks.single().title)
        assertEquals(0, r.updated)
    }
}
