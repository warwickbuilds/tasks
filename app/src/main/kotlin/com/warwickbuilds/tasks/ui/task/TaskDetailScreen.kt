package com.warwickbuilds.tasks.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.core.DueLabels
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.tasksApp
import com.warwickbuilds.tasks.ui.components.ConfirmDialog
import com.warwickbuilds.tasks.ui.components.MarkdownView
import com.warwickbuilds.tasks.ui.components.TagPill
import com.warwickbuilds.tasks.ui.components.rememberToday
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A task, with its Markdown description drawn out. Checklist items can be ticked here. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val app = LocalContext.current.tasksApp
    val repo = app.repository
    val taskFlow = remember(taskId) { repo.tasks.map { list -> list.firstOrNull { it.id == taskId } } }
    var loaded by remember { mutableStateOf(false) }
    val task by taskFlow.collectAsStateWithLifecycle(initialValue = null)
    val tags by repo.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = rememberToday()
    var confirmDelete by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val leave = {
        if (!leaving) {
            leaving = true
            onBack()
        }
    }

    LaunchedEffect(task) {
        if (task != null) loaded = true else if (loaded) leave() // deleted elsewhere
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                navigationIcon = {
                    IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    val t = task
                    if (t != null) {
                        IconButton(onClick = { app.appScope.launch { repo.setPinned(t.id, !t.pinned) } }) {
                            Icon(
                                if (t.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (t.pinned) "Unpin" else "Pin to top",
                                tint = if (t.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        val t = task ?: return@Scaffold
        val taskTags = tags.filter { it.id in t.tagIds }.sortedWith(Grouping.tagOrder)
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(t.title, style = MaterialTheme.typography.headlineSmall)
            if (t.dueDate != null || taskTags.isNotEmpty() || t.isDone) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    t.dueDate?.let {
                        Text(
                            "Due ${DueLabels.long(it, today)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (it.isBefore(today) && !t.isDone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                        )
                    }
                    taskTags.forEach { TagPill(it) }
                }
            }
            if (t.description.isNotBlank()) {
                MarkdownView(
                    t.description,
                    onToggle = { line -> app.appScope.launch { repo.toggleCheck(t.id, line) } },
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    "No description. Tap edit to add notes, a checklist or links.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (t.isDone) {
                OutlinedButton(
                    onClick = { app.appScope.launch { repo.setDone(t.id, false) } },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mark not done") }
            } else {
                Button(
                    onClick = {
                        app.appScope.launch { repo.setDone(t.id, true) }
                        leave()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("  Mark done")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this task?",
            text = "It's gone for good once deleted.",
            confirm = "Delete",
            onConfirm = {
                confirmDelete = false
                leave()
                app.appScope.launch { repo.delete(taskId) }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
