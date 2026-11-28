package com.warwickbuilds.tasks

import androidx.test.core.app.ApplicationProvider
import com.warwickbuilds.tasks.core.Group
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.core.StarterTags
import com.warwickbuilds.tasks.data.TaskRepository
import com.warwickbuilds.tasks.data.TasksDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class TaskRepositoryTest {
    private lateinit var db: TasksDatabase
    private lateinit var repo: TaskRepository
    private var now = 1_000L
    private var changes = 0

    @Before fun setUp() {
        db = TasksDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        repo = TaskRepository(db, clock = { now++ }, onChange = { changes++ })
    }

    @After fun tearDown() = db.close()

    @Test fun newDatabaseHasStarterTags() = runTest {
        assertEquals(StarterTags.all.map { it.name }, repo.allTags().map { it.name })
    }

    @Test fun quickAddAppliesTypedAndGroupTags() = runTest {
        val task = repo.quickAdd("#books Dune #scifi", extraTagIds = setOf(StarterTags.TODAY_ID))!!
        assertEquals("Dune", task.title)
        val scifi = repo.allTags().first { it.name == "Scifi" }
        assertEquals(setOf("books", StarterTags.TODAY_ID, scifi.id), repo.task(task.id)!!.tagIds)
        assertTrue(changes > 0)
        assertNull(repo.quickAdd("#books"))
    }

    @Test fun onlyOneTaskIsPinned() = runTest {
        val a = repo.quickAdd("A")!!
        val b = repo.quickAdd("B")!!
        repo.setPinned(a.id, true)
        repo.setPinned(b.id, true)
        assertEquals(listOf(b.id), repo.allTasks().filter { it.pinned }.map { it.id })
        repo.save(repo.task(a.id)!!.copy(pinned = true))
        assertEquals(listOf(a.id), repo.allTasks().filter { it.pinned }.map { it.id })
    }

    @Test fun doneUnpinsAndDeleteCanBeUndone() = runTest {
        val a = repo.quickAdd("A #today")!!
        repo.setPinned(a.id, true)
        repo.setDone(a.id, true)
        val done = repo.task(a.id)!!
        assertNotNull(done.completedAt)
        assertEquals(false, done.pinned)
        val removed = repo.delete(a.id)!!
        assertNull(repo.task(a.id))
        repo.restore(removed)
        assertEquals(setOf(StarterTags.TODAY_ID), repo.task(a.id)!!.tagIds)
    }

    @Test fun clearTodayKeepsTasks() = runTest {
        repo.quickAdd("A #today")
        repo.quickAdd("B", dueDate = LocalDate.now())
        repo.clearToday()
        val tasks = repo.allTasks()
        assertEquals(2, tasks.size)
        val todayGroup = Grouping.content(tasks, Group.ByTag(StarterTags.TODAY_ID), LocalDate.now()).open
        assertEquals(listOf("B"), todayGroup.map { it.title })
    }

    @Test fun checklistToggleSaves() = runTest {
        val a = repo.quickAdd("A", description = "- [ ] one\n- [ ] two")!!
        repo.toggleCheck(a.id, 1)
        assertEquals("- [ ] one\n- [x] two", repo.task(a.id)!!.description)
    }

    @Test fun deletingATagLeavesItsTasks() = runTest {
        val a = repo.quickAdd("A #books")!!
        repo.deleteTag("books")
        repo.deleteTag(StarterTags.TODAY_ID) // not allowed
        assertEquals(emptySet<String>(), repo.task(a.id)!!.tagIds)
        assertTrue(repo.allTags().any { it.id == StarterTags.TODAY_ID })
    }

    @Test fun moveTagReorders() = runTest {
        repo.moveTag("projects", -1)
        assertEquals(listOf("today", "movies", "books", "projects", "tv"), repo.allTags().map { it.id })
    }

    @Test fun exportImportRoundTrip() = runTest {
        repo.quickAdd("A #books", description = "**x**")
        repo.quickAdd("B #gardening", dueDate = LocalDate.of(2026, 12, 1))
        val json = repo.exportJson()

        val db2 = TasksDatabase.build(ApplicationProvider.getApplicationContext(), inMemory = true)
        val repo2 = TaskRepository(db2)
        val (added, updated) = repo2.importJson(json)
        assertEquals(2 to 0, added to updated)
        assertEquals(repo.allTasks().sortedBy { it.id }, repo2.allTasks().sortedBy { it.id })
        assertEquals(0 to 0, repo2.importJson(json))
        db2.close()
    }
}
