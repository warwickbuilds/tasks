package com.warwickbuilds.tasks.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warwickbuilds.tasks.ui.list.QuickAddRequest
import com.warwickbuilds.tasks.ui.list.TaskListScreen
import com.warwickbuilds.tasks.ui.settings.SettingsScreen
import com.warwickbuilds.tasks.ui.tags.TagsScreen
import com.warwickbuilds.tasks.ui.task.TaskDetailScreen
import com.warwickbuilds.tasks.ui.task.TaskEditScreen

private object Routes {
    const val LIST = "list"
    const val TASK = "task/{id}"
    const val EDIT = "edit?id={id}&title={title}&tag={tag}"
    const val TAGS = "tags"
    const val SETTINGS = "settings"

    fun task(id: String) = "task/${Uri.encode(id)}"
    fun edit(id: String? = null, title: String = "", tag: String? = null): String =
        "edit?id=${Uri.encode(id ?: "")}&title=${Uri.encode(title)}&tag=${Uri.encode(tag ?: "")}"
}

@Composable
fun TasksNavHost(
    openTaskId: String?,
    onOpenTaskHandled: () -> Unit,
    quickAdd: QuickAddRequest?,
    onQuickAddHandled: () -> Unit,
) {
    val nav = rememberNavController()

    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            nav.popBackStack(Routes.LIST, inclusive = false)
            nav.navigate(Routes.task(openTaskId))
            onOpenTaskHandled()
        }
    }
    LaunchedEffect(quickAdd) {
        if (quickAdd != null) nav.popBackStack(Routes.LIST, inclusive = false)
    }

    NavHost(
        navController = nav,
        startDestination = Routes.LIST,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.LIST) {
            TaskListScreen(
                quickAddRequest = quickAdd,
                onQuickAddHandled = onQuickAddHandled,
                onOpenTask = { nav.navigate(Routes.task(it)) },
                onNewTaskFull = { title, tag -> nav.navigate(Routes.edit(title = title, tag = tag)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onManageTags = { nav.navigate(Routes.TAGS) },
            )
        }
        composable(Routes.TASK, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TaskDetailScreen(
                taskId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id = id)) },
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("tag") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val args = entry.arguments
            val id = args?.getString("id").orEmpty().ifEmpty { null }
            TaskEditScreen(
                taskId = id,
                initialTitle = args?.getString("title").orEmpty(),
                initialTagId = args?.getString("tag").orEmpty().ifEmpty { null },
                onClose = { savedId ->
                    nav.popBackStack()
                    // A new task opens straight away so you can see it.
                    if (id == null && savedId != null) nav.navigate(Routes.task(savedId))
                },
            )
        }
        composable(Routes.TAGS) { TagsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, onManageTags = { nav.navigate(Routes.TAGS) })
        }
    }
}
