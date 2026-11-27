package com.warwickbuilds.tasks

import android.app.Application
import android.content.Context
import com.warwickbuilds.tasks.data.TaskRepository
import com.warwickbuilds.tasks.data.TasksDatabase
import com.warwickbuilds.tasks.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the one database and repository the app and the widget share. */
class TasksApplication : Application() {
    /** For writes that must finish even if the screen that started them closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: TaskRepository by lazy {
        TaskRepository(
            db = TasksDatabase.build(this),
            onChange = { TodayWidget.refresh(this) },
        )
    }

    val settings: Settings by lazy { Settings(this) }
}

val Context.tasksApp: TasksApplication get() = applicationContext as TasksApplication
