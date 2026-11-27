package com.warwickbuilds.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_tag")
    fun observeLinks(): Flow<List<TaskTagEntity>>

    @Query("SELECT * FROM tag")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM task")
    suspend fun tasks(): List<TaskEntity>

    @Query("SELECT * FROM task_tag")
    suspend fun links(): List<TaskTagEntity>

    @Query("SELECT * FROM tag")
    suspend fun tags(): List<TagEntity>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun task(id: String): TaskEntity?

    @Query("SELECT tagId FROM task_tag WHERE taskId = :id")
    suspend fun tagIdsFor(id: String): List<String>

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Upsert
    suspend fun upsertTag(tag: TagEntity)

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<TaskTagEntity>)

    @Query("DELETE FROM task_tag WHERE taskId = :taskId")
    suspend fun clearLinks(taskId: String)

    @Query("DELETE FROM task_tag WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun removeLink(taskId: String, tagId: String)

    @Query("DELETE FROM task_tag WHERE tagId = :tagId")
    suspend fun removeTagFromAll(tagId: String)

    @Query("UPDATE task SET pinned = 0, updatedAt = :now WHERE pinned = 1 AND id != :keepId")
    suspend fun unpinOthers(keepId: String, now: Long)

    @Query("UPDATE task SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE task SET completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completedAt: Long?, now: Long)

    @Query("UPDATE task SET description = :description, updatedAt = :now WHERE id = :id")
    suspend fun setDescription(id: String, description: String, now: Long)

    @Query("DELETE FROM task WHERE completedAt IS NOT NULL")
    suspend fun deleteDone()
}
