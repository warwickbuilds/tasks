package com.warwickbuilds.tasks.core

import java.time.LocalDate

/** What the chip row switches between: two built-in queries, then one group per tag. */
sealed interface Group {
    val key: String

    data object All : Group {
        override val key = "all"
    }

    data object Due : Group {
        override val key = "due"
    }

    data class ByTag(val tagId: String) : Group {
        override val key = "tag:$tagId"
    }

    companion object {
        fun fromKey(key: String?): Group = when {
            key == Due.key -> Due
            key != null && key.startsWith("tag:") -> ByTag(key.removePrefix("tag:"))
            else -> All
        }
    }
}

data class GroupContent(val open: List<Task>, val done: List<Task>)

object Grouping {

    /** The groups in chip order: All, Due, then the tags in their own order (Today first by default). */
    fun groups(tags: List<Tag>): List<Group> =
        listOf(Group.All, Group.Due) + tags.sortedWith(tagOrder).map { Group.ByTag(it.id) }

    val tagOrder: Comparator<Tag> = compareBy<Tag> { it.sortOrder }.thenBy { it.name.lowercase() }

    /**
     * Whether [task] shows in [group]. Today also picks up open tasks that are due today
     * or overdue, without needing the tag.
     */
    fun belongs(task: Task, group: Group, today: LocalDate): Boolean = when (group) {
        Group.All -> true
        Group.Due -> task.dueDate != null
        is Group.ByTag -> group.tagId in task.tagIds ||
            (group.tagId == StarterTags.TODAY_ID && !task.isDone && isDueByToday(task, today))
    }

    fun isDueByToday(task: Task, today: LocalDate): Boolean =
        task.dueDate?.let { !it.isAfter(today) } ?: false

    fun content(tasks: List<Task>, group: Group, today: LocalDate): GroupContent {
        val members = tasks.filter { belongs(it, group, today) }
        val open = members.filter { !it.isDone }.sortedWith(openOrder(group))
        val done = members.filter { it.isDone }.sortedByDescending { it.completedAt }
        return GroupContent(open, done)
    }

    /** Pinned first. Due sorts soonest first; everything else shows the newest first. */
    fun openOrder(group: Group): Comparator<Task> {
        val pinnedFirst = compareByDescending<Task> { it.pinned }
        return if (group == Group.Due) {
            pinnedFirst.thenBy { it.dueDate }.thenByDescending { it.createdAt }
        } else {
            pinnedFirst.thenByDescending { it.createdAt }
        }
    }

    /**
     * What the home-screen widget lists: the pinned task first (even if it isn't tagged
     * Today), then the open tasks in Today.
     */
    fun widgetTasks(tasks: List<Task>, today: LocalDate): List<Task> {
        val pinned = tasks.firstOrNull { it.pinned && !it.isDone }
        val todayTasks = content(tasks, Group.ByTag(StarterTags.TODAY_ID), today).open
        return listOfNotNull(pinned) + todayTasks.filter { it.id != pinned?.id }
    }
}
