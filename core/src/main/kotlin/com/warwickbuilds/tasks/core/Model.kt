package com.warwickbuilds.tasks.core

import java.time.LocalDate

/** One task. Only the title is required; everything else is optional. */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val dueDate: LocalDate? = null,
    val pinned: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val tagIds: Set<String> = emptySet(),
) {
    val isDone: Boolean get() = completedAt != null
}

/** A tag is how tasks are grouped. [color] is an index into the app's tag palette. */
data class Tag(
    val id: String,
    val name: String,
    val color: Int,
    val sortOrder: Int,
)

object StarterTags {
    /** The Today tag has a fixed id: the widget, `#today` and "Clear Today" rely on it. */
    const val TODAY_ID = "today"

    val all: List<Tag> = listOf(
        Tag(TODAY_ID, "Today", color = 0, sortOrder = 0),
        Tag("movies", "Movies to watch", color = 1, sortOrder = 1),
        Tag("books", "Books to read", color = 2, sortOrder = 2),
        Tag("tv", "TV shows", color = 3, sortOrder = 3),
        Tag("projects", "Projects", color = 4, sortOrder = 4),
    )
}
