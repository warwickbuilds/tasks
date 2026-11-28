package com.warwickbuilds.tasks.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.warwickbuilds.tasks.MainActivity
import com.warwickbuilds.tasks.R
import com.warwickbuilds.tasks.core.DueLabels
import com.warwickbuilds.tasks.core.DueTone
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.core.StarterTags
import com.warwickbuilds.tasks.core.Task
import com.warwickbuilds.tasks.tasksApp
import com.warwickbuilds.tasks.ui.theme.CalmDark
import com.warwickbuilds.tasks.ui.theme.CalmLight
import java.time.LocalDate

private val CalmWidgetColors = ColorProviders(light = CalmLight, dark = CalmDark)

/**
 * The home-screen widget: tasks in Today (tagged, or due today or earlier), with the
 * pinned task first. Tick to complete, tap a title to open it, "+" to add one to Today.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.tasksApp
        val repo = app.repository
        val initial = repo.allTasks()
        val wallpaper = app.settings.wallpaperColours.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        provideContent {
            val tasks by repo.tasks.collectAsState(initial = initial)
            val shown = Grouping.widgetTasks(tasks, LocalDate.now())
            if (wallpaper) {
                GlanceTheme { Content(context, shown) }
            } else {
                GlanceTheme(colors = CalmWidgetColors) { Content(context, shown) }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            TodayWidget().updateAll(context)
        }
    }
}

@Composable
private fun Content(context: Context, tasks: List<Task>) {
    val today = LocalDate.now()
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Today",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            )
            Box(
                GlanceModifier
                    .size(40.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(MainActivity.quickAddIntent(context, StarterTags.TODAY_ID)))
                    .semantics { contentDescription = "Add a task for today" },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(20.dp),
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        if (tasks.isEmpty()) {
            Text(
                "Nothing for today.",
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                    TaskLine(context, task, today)
                }
            }
        }
    }
}

@Composable
private fun TaskLine(context: Context, task: Task, today: LocalDate) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckBox(
            checked = false,
            onCheckedChange = actionRunCallback<CompleteTaskAction>(actionParametersOf(CompleteTaskAction.TaskId to task.id)),
            modifier = GlanceModifier.semantics { contentDescription = "Complete ${task.title}" },
        )
        Column(
            GlanceModifier
                .defaultWeight()
                .padding(vertical = 6.dp)
                .clickable(actionStartActivity(MainActivity.openTaskIntent(context, task.id))),
        ) {
            Text(
                task.title,
                maxLines = 2,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface),
            )
            val due = task.dueDate?.let { DueLabels.label(it, today) }
            val note = listOfNotNull(if (task.pinned) "Pinned" else null, due?.text).joinToString(" · ")
            if (note.isNotEmpty()) {
                Text(
                    note,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = if (due?.tone == DueTone.Overdue) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                    ),
                )
            }
        }
    }
}

/** Marks a task done from the widget. */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskId] ?: return
        context.tasksApp.repository.setDone(id, true)
    }

    companion object {
        val TaskId = ActionParameters.Key<String>("task_id")
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
