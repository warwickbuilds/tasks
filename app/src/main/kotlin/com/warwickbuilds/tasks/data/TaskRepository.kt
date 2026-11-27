package com.warwickbuilds.tasks.data

import androidx.room.withTransaction
import com.warwickbuilds.tasks.core.Backup
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.core.QuickAddParser
import com.warwickbuilds.tasks.core.StarterTags
import com.warwickbuilds.tasks.core.Tag
import com.warwickbuilds.tasks.core.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Every read and write of tasks and tags goes through here, for the app and the widget.
 * [onChange] runs after each write (the app uses it to refresh the home-screen widget).
 */
class TaskRepository(
    private val db: TasksDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onChange: suspend () -> Unit = {},
) {
    private val dao = db.dao()

    val tasks: Flow<List<Task>> = combine(dao.observeTasks(), dao.observeLinks()) { tasks, links ->
        val byTask = links.groupBy({ it.taskId }, { it.tagId })
        tasks.map { it.toTask(byTask[it.id].orEmpty().toSet()) }
    }

    val tags: Flow<List<Tag>> = dao.observeTags().map { list -> list.map { it.toTag() }.sortedWith(Grouping.tagOrder) }

    suspend fun allTasks(): List<Task> {
        val byTask = dao.links().groupBy({ it.taskId }, { it.tagId })
        return dao.tasks().map { it.toTask(byTask[it.id].orEmpty().toSet()) }
    }

    suspend fun allTags(): List<Tag> = dao.tags().map { it.toTag() }.sortedWith(Grouping.tagOrder)

    suspend fun task(id: String): Task? = dao.task(id)?.toTask(dao.tagIdsFor(id).toSet())

    /**
     * Adds a task from a typed line. `#tags` in [text] are applied (new ones are created),
     * plus any [extraTagIds] (the group you're looking at). Returns null if there's no title left.
     */
    suspend fun quickAdd(text: String, extraTagIds: Set<String> = emptySet(), description: String = "", dueDate: LocalDate? = null): Task? {
        val parsed = QuickAddParser.parse(text, allTags())
        if (parsed.title.isBlank()) return null
        val now = clock()
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = parsed.title,
            description = description,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now,
            tagIds = parsed.tagIds + extraTagIds,
        )
        return write {
            val newIds = parsed.newTagNames.map { addTagInTx(it).id }
            val saved = task.copy(tagIds = task.tagIds + newIds)
            saveInTx(saved)
            saved
        }
    }

    /** Creates or replaces a task, with its tags. `#tags` typed in the title are applied too. */
    suspend fun save(task: Task): Task {
        val parsed = QuickAddParser.parse(task.title, allTags())
        val title = parsed.title.ifBlank { task.title.trim() }
        return write {
            val newIds = parsed.newTagNames.map { addTagInTx(it).id }
            val saved = task.copy(title = title, tagIds = task.tagIds + parsed.tagIds + newIds, updatedAt = clock())
            saveInTx(saved)
            saved
        }
    }

    private suspend fun saveInTx(task: Task) {
        dao.upsertTask(task.toEntity())
        dao.clearLinks(task.id)
        val known = dao.tags().map { it.id }.toSet()
        dao.insertLinks(task.tagIds.filter { it in known }.map { TaskTagEntity(task.id, it) })
        if (task.pinned) dao.unpinOthers(task.id, task.updatedAt)
    }

    suspend fun setDone(id: String, done: Boolean) = write {
        val now = clock()
        dao.setCompleted(id, if (done) now else null, now)
        // A finished task doesn't stay pinned.
        if (done) dao.setPinned(id, false, now)
    }

    /** Pins [id] to the top (unpinning any other), or unpins it. */
    suspend fun setPinned(id: String, pinned: Boolean) = write {
        val now = clock()
        dao.setPinned(id, pinned, now)
        if (pinned) dao.unpinOthers(id, now)
    }

    suspend fun setDescription(id: String, description: String) = write { dao.setDescription(id, description, clock()) }

    suspend fun toggleCheck(id: String, line: Int) {
        val current = dao.task(id) ?: return
        val updated = com.warwickbuilds.tasks.core.Markdown.toggleCheck(current.description, line)
        if (updated != current.description) setDescription(id, updated)
    }

    /** Deletes a task and returns it, so it can be put back with [restore]. */
    suspend fun delete(id: String): Task? {
        val task = task(id) ?: return null
        write { dao.deleteTask(id) }
        return task
    }

    suspend fun restore(task: Task) = write { saveInTx(task) }

    suspend fun setTag(taskId: String, tagId: String, on: Boolean) = write {
        if (on) dao.insertLinks(listOf(TaskTagEntity(taskId, tagId))) else dao.removeLink(taskId, tagId)
    }

    /** Takes the Today tag off every task, for a fresh day. Tasks due today still show in Today. */
    suspend fun clearToday() = write { dao.removeTagFromAll(StarterTags.TODAY_ID) }

    suspend fun deleteDone() = write { dao.deleteDone() }

    /** Adds a tag, or returns the existing one with the same name. */
    suspend fun addTag(name: String): Tag = write { addTagInTx(name) }

    private suspend fun addTagInTx(name: String): Tag {
        val clean = name.trim().replace(Regex("""\s+"""), " ")
        val existing = dao.tags().map { it.toTag() }
        existing.firstOrNull { QuickAddParser.slug(it.name) == QuickAddParser.slug(clean) }?.let { return it }
        val tag = Tag(
            id = UUID.randomUUID().toString(),
            name = clean,
            color = existing.size % TAG_PALETTE_SIZE,
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
        )
        dao.upsertTag(tag.toEntity())
        return tag
    }

    suspend fun updateTag(tag: Tag) = write { dao.upsertTag(tag.toEntity()) }

    /** Deletes a tag (not its tasks). Today can't be deleted. */
    suspend fun deleteTag(id: String) {
        if (id == StarterTags.TODAY_ID) return
        write { dao.deleteTag(id) }
    }

    /** Moves a tag one place left (-1) or right (+1) in the chip row. */
    suspend fun moveTag(id: String, delta: Int) = write {
        val tags = dao.tags().map { it.toTag() }.sortedWith(Grouping.tagOrder).toMutableList()
        val from = tags.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in tags.indices) return@write
        val moved = tags.removeAt(from)
        tags.add(to, moved)
        dao.upsertTags(tags.mapIndexed { i, t -> t.copy(sortOrder = i).toEntity() })
    }

    suspend fun exportJson(): String = Backup.encode(allTasks(), allTags(), Instant.ofEpochMilli(clock()).toString())

    /** Adds a backup's tasks to the ones on the phone. Returns how many were added and updated. */
    suspend fun importJson(text: String): Pair<Int, Int> {
        val (incomingTasks, incomingTags) = Backup.decode(text)
        return write {
            val existingTasks = allTasks()
            val result = Backup.merge(existingTasks, allTags(), incomingTasks, incomingTags)
            dao.upsertTags(result.tags.map { it.toEntity() })
            val before = existingTasks.associateBy { it.id }
            result.tasks.filter { before[it.id] != it }.forEach { saveInTxNoPin(it) }
            result.added to result.updated
        }
    }

    private suspend fun saveInTxNoPin(task: Task) {
        dao.upsertTask(task.toEntity())
        dao.clearLinks(task.id)
        dao.insertLinks(task.tagIds.map { TaskTagEntity(task.id, it) })
    }

    private suspend fun <T> write(block: suspend () -> T): T {
        val result = db.withTransaction { block() }
        runCatching { onChange() }
        return result
    }

    companion object {
        const val TAG_PALETTE_SIZE = 8
    }
}
