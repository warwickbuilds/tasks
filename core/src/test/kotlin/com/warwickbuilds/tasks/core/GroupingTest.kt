package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupingTest {
    private val today = LocalDate.of(2026, 11, 27)
    private fun task(id: String, created: Long, vararg tags: String, due: LocalDate? = null, pinned: Boolean = false, done: Long? = null) =
        Task(id, "Task $id", createdAt = created, tagIds = tags.toSet(), dueDate = due, pinned = pinned, completedAt = done)

    @Test fun chipOrderIsAllDueThenTagsBySortOrder() {
        val groups = Grouping.groups(listOf(Tag("b", "B", 0, 2), Tag("a", "A", 0, 1), StarterTags.all[0]))
        assertEquals(listOf(Group.All, Group.Due, Group.ByTag("today"), Group.ByTag("a"), Group.ByTag("b")), groups)
    }

    @Test fun allShowsNewestFirstWithPinnedOnTop() {
        val tasks = listOf(task("old", 1), task("new", 3), task("pin", 2, pinned = true), task("done", 4, done = 10))
        val content = Grouping.content(tasks, Group.All, today)
        assertEquals(listOf("pin", "new", "old"), content.open.map { it.id })
        assertEquals(listOf("done"), content.done.map { it.id })
    }

    @Test fun dueShowsOnlyDatedTasksSoonestFirst() {
        val tasks = listOf(
            task("none", 1),
            task("later", 2, due = today.plusDays(5)),
            task("soon", 3, due = today.plusDays(1)),
            task("late", 4, due = today.minusDays(2)),
        )
        assertEquals(listOf("late", "soon", "later"), Grouping.content(tasks, Group.Due, today).open.map { it.id })
    }

    @Test fun todayIncludesTaggedAndDueOrOverdueTasks() {
        val tasks = listOf(
            task("tagged", 1, "today"),
            task("dueToday", 2, due = today),
            task("overdue", 3, due = today.minusDays(1)),
            task("tomorrow", 4, due = today.plusDays(1)),
            task("other", 5, "books"),
        )
        val ids = Grouping.content(tasks, Group.ByTag(StarterTags.TODAY_ID), today).open.map { it.id }.toSet()
        assertEquals(setOf("tagged", "dueToday", "overdue"), ids)
    }

    @Test fun doneDueTasksDoNotLingerInToday() {
        val t = task("x", 1, due = today, done = 5)
        assertFalse(Grouping.belongs(t, Group.ByTag(StarterTags.TODAY_ID), today))
        assertTrue(Grouping.belongs(t.copy(tagIds = setOf("today")), Group.ByTag(StarterTags.TODAY_ID), today))
    }

    @Test fun tagGroupsOnlyShowTheirTag() {
        val tasks = listOf(task("a", 1, "books"), task("b", 2, "movies"), task("c", 3, "books", "movies"))
        assertEquals(listOf("c", "a"), Grouping.content(tasks, Group.ByTag("books"), today).open.map { it.id })
    }

    @Test fun widgetPutsPinnedTaskFirstEvenWhenNotInToday() {
        val tasks = listOf(task("t1", 1, "today"), task("t2", 2, "today"), task("pin", 0, "books", pinned = true))
        assertEquals(listOf("pin", "t2", "t1"), Grouping.widgetTasks(tasks, today).map { it.id })
    }

    @Test fun widgetDoesNotRepeatAPinnedTodayTask() {
        val tasks = listOf(task("t1", 1, "today"), task("pin", 0, "today", pinned = true))
        assertEquals(listOf("pin", "t1"), Grouping.widgetTasks(tasks, today).map { it.id })
    }

    @Test fun groupKeysRoundTrip() {
        listOf(Group.All, Group.Due, Group.ByTag("books")).forEach { assertEquals(it, Group.fromKey(it.key)) }
        assertEquals(Group.All, Group.fromKey(null))
        assertEquals(Group.All, Group.fromKey("nonsense"))
    }
}
