package com.warwickbuilds.tasks.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.warwickbuilds.tasks.core.Tag
import com.warwickbuilds.tasks.core.Task
import java.time.LocalDate

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    /** ISO date ("2026-11-28"), date only, or null. */
    val dueDate: String?,
    val pinned: Boolean,
    val completedAt: Long?,
    /** Reserved for drag-to-reorder (phase 2). */
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int,
    val sortOrder: Int,
)

@Entity(
    tableName = "task_tag",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class TaskTagEntity(
    val taskId: String,
    val tagId: String,
)

fun TaskEntity.toTask(tagIds: Set<String>) = Task(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    pinned = pinned,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tagIds = tagIds,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate?.toString(),
    pinned = pinned,
    completedAt = completedAt,
    sortOrder = 0,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TagEntity.toTag() = Tag(id, name, color, sortOrder)
fun Tag.toEntity() = TagEntity(id, name, color, sortOrder)
