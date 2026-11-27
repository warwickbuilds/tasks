package com.warwickbuilds.tasks.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * The JSON backup file. It's plain, readable JSON on purpose: it's your data, in a file
 * you choose where to keep. Bump [FORMAT] (and handle the old one in [Backup.decode]) when
 * the shape changes.
 */
@Serializable
data class BackupFile(
    val app: String = APP,
    val format: Int = FORMAT,
    @SerialName("exported_at") val exportedAt: String,
    val tags: List<BackupTag>,
    val tasks: List<BackupTask>,
) {
    companion object {
        const val APP = "tasks"
        const val FORMAT = 1
    }
}

@Serializable
data class BackupTag(val id: String, val name: String, val color: Int = 0, val sortOrder: Int = 0)

@Serializable
data class BackupTask(
    val id: String,
    val title: String,
    val description: String = "",
    val dueDate: String? = null,
    val pinned: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val tagIds: List<String> = emptyList(),
)

class BackupFormatException(message: String) : Exception(message)

data class MergeResult(val tasks: List<Task>, val tags: List<Tag>, val added: Int, val updated: Int)

object Backup {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(tasks: List<Task>, tags: List<Tag>, exportedAt: String): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            exportedAt = exportedAt,
            tags = tags.map { BackupTag(it.id, it.name, it.color, it.sortOrder) },
            tasks = tasks.map {
                BackupTask(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    dueDate = it.dueDate?.toString(),
                    pinned = it.pinned,
                    completedAt = it.completedAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    tagIds = it.tagIds.sorted(),
                )
            },
        ),
    )

    /** Reads a backup. Throws [BackupFormatException] if it isn't one this version understands. */
    fun decode(text: String): Pair<List<Task>, List<Tag>> {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupFormatException("This file isn't a Tasks backup.")
        }
        if (file.app != BackupFile.APP) throw BackupFormatException("This file isn't a Tasks backup.")
        if (file.format > BackupFile.FORMAT) {
            throw BackupFormatException("This backup was made by a newer version of Tasks. Update the app first.")
        }
        val tags = file.tags.filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .map { Tag(it.id, it.name.trim(), it.color, it.sortOrder) }
        val tagIds = tags.map { it.id }.toSet()
        val tasks = file.tasks.filter { it.id.isNotBlank() && it.title.isNotBlank() }.map {
            Task(
                id = it.id,
                title = it.title.trim(),
                description = it.description,
                dueDate = it.dueDate?.let { d -> runCatching { LocalDate.parse(d) }.getOrNull() },
                pinned = it.pinned,
                completedAt = it.completedAt,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                tagIds = it.tagIds.filter { id -> id in tagIds }.toSet(),
            )
        }
        return tasks to tags
    }

    /**
     * Adds a backup to what's already on the phone. Nothing is deleted. Tags are matched by
     * id, then by name; tasks by id, keeping whichever copy was edited last. At most one task
     * stays pinned (the one already pinned on the phone wins).
     */
    fun merge(
        existingTasks: List<Task>,
        existingTags: List<Tag>,
        incomingTasks: List<Task>,
        incomingTags: List<Tag>,
    ): MergeResult {
        val tags = existingTags.toMutableList()
        val tagIdMap = mutableMapOf<String, String>()
        var nextOrder = (tags.maxOfOrNull { it.sortOrder } ?: -1) + 1
        for (tag in incomingTags.sortedWith(Grouping.tagOrder)) {
            val match = tags.firstOrNull { it.id == tag.id }
                ?: tags.firstOrNull { QuickAddParser.slug(it.name) == QuickAddParser.slug(tag.name) }
            if (match != null) {
                tagIdMap[tag.id] = match.id
            } else {
                tags += tag.copy(sortOrder = nextOrder++)
                tagIdMap[tag.id] = tag.id
            }
        }

        val byId = existingTasks.associateBy { it.id }.toMutableMap()
        var added = 0
        var updated = 0
        for (task in incomingTasks) {
            val mapped = task.copy(tagIds = task.tagIds.mapNotNull { tagIdMap[it] }.toSet())
            val current = byId[task.id]
            when {
                current == null -> {
                    byId[task.id] = mapped
                    added++
                }
                mapped.updatedAt > current.updatedAt -> {
                    byId[task.id] = mapped
                    updated++
                }
            }
        }

        val keepPinned = existingTasks.firstOrNull { it.pinned }?.id
            ?: byId.values.filter { it.pinned }.maxByOrNull { it.updatedAt }?.id
        val tasks = byId.values.map { if (it.pinned && it.id != keepPinned) it.copy(pinned = false) else it }
        return MergeResult(tasks, tags, added, updated)
    }
}
