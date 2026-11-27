package com.warwickbuilds.tasks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.ui.TasksNavHost
import com.warwickbuilds.tasks.ui.list.QuickAddRequest
import com.warwickbuilds.tasks.ui.theme.TasksTheme

class MainActivity : ComponentActivity() {
    /** Set by the widget: open a task, or open quick add (with the Today tag). */
    private var openTaskId by mutableStateOf<String?>(null)
    private var quickAdd by mutableStateOf<QuickAddRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val wallpaper by tasksApp.settings.wallpaperColours.collectAsStateWithLifecycle()
            TasksTheme(wallpaperColours = wallpaper) {
                TasksNavHost(
                    openTaskId = openTaskId,
                    onOpenTaskHandled = { openTaskId = null },
                    quickAdd = quickAdd,
                    onQuickAddHandled = { quickAdd = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_TASK -> openTaskId = intent.getStringExtra(EXTRA_TASK_ID)
            ACTION_QUICK_ADD -> quickAdd = QuickAddRequest(intent.getStringExtra(EXTRA_TAG_ID))
        }
    }

    companion object {
        const val ACTION_OPEN_TASK = "com.warwickbuilds.tasks.OPEN_TASK"
        const val ACTION_QUICK_ADD = "com.warwickbuilds.tasks.QUICK_ADD"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TAG_ID = "tag_id"

        fun openTaskIntent(context: Context, taskId: String): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_TASK)
                .setData(android.net.Uri.parse("tasks://task/$taskId"))
                .putExtra(EXTRA_TASK_ID, taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun quickAddIntent(context: Context, tagId: String?): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_QUICK_ADD)
                .putExtra(EXTRA_TAG_ID, tagId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun openAppIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
