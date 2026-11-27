package com.warwickbuilds.tasks.ui.list

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.core.Group
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.core.Markdown
import com.warwickbuilds.tasks.core.StarterTags
import com.warwickbuilds.tasks.core.Tag
import com.warwickbuilds.tasks.core.Task
import com.warwickbuilds.tasks.tasksApp
import com.warwickbuilds.tasks.ui.components.ConfirmDialog
import com.warwickbuilds.tasks.ui.components.DueText
import com.warwickbuilds.tasks.ui.components.MarkdownView
import com.warwickbuilds.tasks.ui.components.NameDialog
import com.warwickbuilds.tasks.ui.components.TagPickerDialog
import com.warwickbuilds.tasks.ui.components.TaskCheck
import com.warwickbuilds.tasks.ui.components.rememberToday
import com.warwickbuilds.tasks.ui.theme.tagTone
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A request to open the quick-add sheet, e.g. from the widget's "+" (with the Today tag). */
data class QuickAddRequest(val tagId: String?, val nonce: Long = System.nanoTime())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    quickAddRequest: QuickAddRequest?,
    onQuickAddHandled: () -> Unit,
    onOpenTask: (String) -> Unit,
    onNewTaskFull: (title: String, tagId: String?) -> Unit,
    onOpenSettings: () -> Unit,
    onManageTags: () -> Unit,
) {
    val app = LocalContext.current.tasksApp
    val repo = app.repository
    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = null)
    val tags by repo.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = rememberToday()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val groups = remember(tags) { Grouping.groups(tags) }
    val initialPage = remember { groups.indexOf(Group.fromKey(app.settings.lastGroup)).coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = initialPage) { groups.size }
    val currentGroup = groups.getOrElse(pager.currentPage) { Group.All }

    // Tags load a moment after the first frame; jump to the remembered group once they're in.
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(groups) {
        if (!restored && tags.isNotEmpty()) {
            val page = groups.indexOf(Group.fromKey(app.settings.lastGroup))
            if (page > 0) pager.scrollToPage(page)
            restored = true
        }
    }
    LaunchedEffect(currentGroup) { if (restored) app.settings.lastGroup = currentGroup.key }

    var quickAddFor by remember { mutableStateOf<QuickAddRequest?>(null) }
    LaunchedEffect(quickAddRequest) {
        if (quickAddRequest != null) {
            quickAddRequest.tagId?.let { id ->
                val page = groups.indexOf(Group.ByTag(id))
                if (page >= 0) pager.scrollToPage(page)
            }
            quickAddFor = quickAddRequest
            onQuickAddHandled()
        }
    }

    var menuTask by remember { mutableStateOf<Task?>(null) }
    var tagsForTask by remember { mutableStateOf<Task?>(null) }
    var confirmClearToday by remember { mutableStateOf(false) }
    var addingTag by remember { mutableStateOf(false) }

    fun runWrite(block: suspend () -> Unit) {
        app.appScope.launch { block() }
    }

    fun deleteWithUndo(task: Task) {
        scope.launch {
            val removed = repo.delete(task.id) ?: return@launch
            val result = snackbar.showSnackbar("Task deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) repo.restore(removed)
        }
    }

    fun completeWithUndo(task: Task) {
        runWrite { repo.setDone(task.id, !task.isDone) }
        if (!task.isDone) {
            scope.launch {
                val result = snackbar.showSnackbar("Done", actionLabel = "Undo", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    repo.setDone(task.id, false)
                    if (task.pinned) repo.setPinned(task.id, true)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { quickAddFor = QuickAddRequest((currentGroup as? Group.ByTag)?.tagId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tasks", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
            }

            ChipRow(
                groups = groups,
                tags = tags,
                selected = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                onClearToday = { confirmClearToday = true },
                onManageTags = onManageTags,
                onAddTag = { addingTag = true },
            )

            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { groups.getOrNull(it)?.key ?: it }) { page ->
                val group = groups.getOrElse(page) { Group.All }
                val all = tasks
                if (all != null) {
                    GroupPage(
                        group = group,
                        tasks = all,
                        tags = tags,
                        today = today,
                        onOpen = { onOpenTask(it.id) },
                        onToggleDone = { completeWithUndo(it) },
                        onLongPress = { menuTask = it },
                        onToggleCheck = { task, line -> runWrite { repo.toggleCheck(task.id, line) } },
                        onDeleteDone = { runWrite { repo.deleteDone() } },
                    )
                }
            }
        }
    }

    menuTask?.let { task ->
        TaskMenu(
            task = task,
            onDismiss = { menuTask = null },
            onPin = { runWrite { repo.setPinned(task.id, !task.pinned) } },
            onTags = { tagsForTask = task },
            onDelete = { deleteWithUndo(task) },
        )
    }

    tagsForTask?.let { t ->
        val live = tasks?.firstOrNull { it.id == t.id } ?: t
        TagPickerDialog(
            tags = tags,
            selected = live.tagIds,
            onToggle = { tagId, on -> runWrite { repo.setTag(t.id, tagId, on) } },
            onAddTag = { name -> runWrite { repo.setTag(t.id, repo.addTag(name).id, true) } },
            onDismiss = { tagsForTask = null },
        )
    }

    quickAddFor?.let { request ->
        QuickAddSheet(
            tags = tags,
            groupTagId = request.tagId,
            onAdd = { text -> runWrite { repo.quickAdd(text, setOfNotNull(request.tagId)) } },
            onMore = { text ->
                quickAddFor = null
                onNewTaskFull(text, request.tagId)
            },
            onDismiss = { quickAddFor = null },
        )
    }

    if (confirmClearToday) {
        ConfirmDialog(
            title = "Clear Today?",
            text = "This takes the Today tag off every task, for a fresh start. The tasks themselves stay. Tasks due today still show here.",
            confirm = "Clear Today",
            onConfirm = {
                confirmClearToday = false
                runWrite { repo.clearToday() }
            },
            onDismiss = { confirmClearToday = false },
        )
    }

    if (addingTag) {
        NameDialog(
            title = "New tag",
            initial = "",
            confirm = "Add",
            onConfirm = { name ->
                addingTag = false
                scope.launch {
                    val tag = repo.addTag(name)
                    val page = Grouping.groups(repo.allTags()).indexOf(Group.ByTag(tag.id))
                    if (page >= 0) {
                        // Wait for the chip row to catch up before moving to it.
                        kotlinx.coroutines.delay(150)
                        pager.animateScrollToPage(page)
                    }
                }
            },
            onDismiss = { addingTag = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChipRow(
    groups: List<Group>,
    tags: List<Tag>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClearToday: () -> Unit,
    onManageTags: () -> Unit,
    onAddTag: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { if (selected in groups.indices) listState.animateScrollToItem(selected) }
    val byId = tags.associateBy { it.id }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        itemsIndexed(groups, key = { _, g -> g.key }) { index, group ->
            val label = when (group) {
                Group.All -> "All"
                Group.Due -> "Due"
                is Group.ByTag -> byId[group.tagId]?.name ?: ""
            }
            var menu by remember { mutableStateOf(false) }
            Box {
                GroupChip(
                    label = label,
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    onLongClick = if (group is Group.ByTag) ({ menu = true }) else null,
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (group is Group.ByTag && group.tagId == StarterTags.TODAY_ID) {
                        DropdownMenuItem(text = { Text("Clear Today") }, onClick = {
                            menu = false
                            onClearToday()
                        })
                    }
                    DropdownMenuItem(text = { Text("Edit tags") }, onClick = {
                        menu = false
                        onManageTags()
                    })
                }
            }
        }
        item(key = "add-tag") {
            Box(
                Modifier
                    .height(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .combinedClickable(onClickLabel = "Add a tag", onClick = onAddTag)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add a tag", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .height(36.dp)
            .background(if (selected) colors.primary else colors.surfaceContainerLowest, shape)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.outlineVariant, shape))
            .combinedClickable(
                role = Role.Tab,
                onClick = onClick,
                onLongClickLabel = if (onLongClick != null) "Tag options" else null,
                onLongClick = onLongClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onPrimary else colors.onSurfaceVariant)
    }
}

@Composable
private fun GroupPage(
    group: Group,
    tasks: List<Task>,
    tags: List<Tag>,
    today: LocalDate,
    onOpen: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    onLongPress: (Task) -> Unit,
    onToggleCheck: (Task, Int) -> Unit,
    onDeleteDone: () -> Unit,
) {
    val content = remember(tasks, group, today) { Grouping.content(tasks, group, today) }
    var showDone by rememberSaveable(group.key) { mutableStateOf(false) }
    val tagsById = remember(tags) { tags.associateBy { it.id } }
    val pinned = content.open.firstOrNull { it.pinned }
    val rest = content.open.filter { it.id != pinned?.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
    ) {
        if (content.open.isEmpty() && content.done.isEmpty()) {
            item(key = "empty") { EmptyState(group, tagsById) }
        }
        if (pinned != null) {
            item(key = "pinned-${pinned.id}") {
                PinnedCard(pinned, today, onOpen, onToggleDone, onLongPress, onToggleCheck)
            }
        }
        items(rest, key = { it.id }) { task ->
            TaskRow(task, group, tagsById, today, onOpen, onToggleDone, onLongPress)
        }
        if (content.done.isNotEmpty()) {
            item(key = "done-header") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDone = !showDone }) {
                        Icon(if (showDone) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (showDone) "Hide done" else "Done", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    if (showDone) {
                        TextButton(onClick = onDeleteDone) { Text("Delete done", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (showDone) {
                items(content.done, key = { "done-${it.id}" }) { task ->
                    TaskRow(task, group, tagsById, today, onOpen, onToggleDone, onLongPress)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(group: Group, tags: Map<String, Tag>) {
    val text = when {
        group == Group.All -> "Nothing to do. Tap New task to add one."
        group == Group.Due -> "No due dates. Add one to a task and it shows here, soonest first."
        group is Group.ByTag && group.tagId == StarterTags.TODAY_ID ->
            "Nothing for today. Add #today to a task, or give it today's date, and it shows here and on your widget."
        group is Group.ByTag -> "Nothing in ${tags[group.tagId]?.name ?: "this tag"} yet. New tasks added here get the tag."
        else -> ""
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 32.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedCard(
    task: Task,
    today: LocalDate,
    onOpen: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    onLongPress: (Task) -> Unit,
    onToggleCheck: (Task, Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val blocks = remember(task.description) { Markdown.parse(task.description).size }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.primaryContainer, RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { onOpen(task) }, onLongClick = { onLongPress(task) }, onLongClickLabel = "Task options")
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PushPin, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("PINNED", style = MaterialTheme.typography.labelMedium, color = colors.primary, modifier = Modifier.weight(1f))
            task.dueDate?.let { DueText(it, today) }
            TaskCheck(done = task.isDone, title = task.title, onToggle = { onToggleDone(task) })
        }
        Text(task.title, style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer, modifier = Modifier.padding(end = 10.dp))
        if (task.description.isNotBlank()) {
            MarkdownView(
                task.description,
                maxBlocks = 6,
                compact = true,
                color = colors.onPrimaryContainer,
                onToggle = { line -> onToggleCheck(task, line) },
                modifier = Modifier.padding(end = 10.dp),
            )
            if (blocks > 6) {
                Text("More…", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: Task,
    group: Group,
    tags: Map<String, Tag>,
    today: LocalDate,
    onOpen: (Task) -> Unit,
    onToggleDone: (Task) -> Unit,
    onLongPress: (Task) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val preview = remember(task.description) { Markdown.preview(task.description) }
    val shownTags = task.tagIds
        .filter { !(group is Group.ByTag && group.tagId == it) }
        .mapNotNull { tags[it] }
        .sortedWith(Grouping.tagOrder)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .combinedClickable(onClick = { onOpen(task) }, onLongClick = { onLongPress(task) }, onLongClickLabel = "Task options")
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TaskCheck(done = task.isDone, title = task.title, onToggle = { onToggleDone(task) })
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isDone) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.isNotEmpty() && !task.isDone) {
                    Text(preview, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (shownTags.isNotEmpty() && !task.isDone) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
                        shownTags.take(3).forEach { tag ->
                            Text(tag.name, style = MaterialTheme.typography.labelMedium, color = tagTone(tag.color).content, maxLines = 1)
                        }
                    }
                }
            }
            if (task.pinned) {
                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = colors.primary, modifier = Modifier.padding(start = 8.dp).size(16.dp))
            }
            task.dueDate?.takeIf { !task.isDone }?.let { DueText(it, today, Modifier.padding(start = 8.dp, end = 4.dp)) }
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 48.dp))
    }
}

@Composable
private fun TaskMenu(task: Task, onDismiss: () -> Unit, onPin: () -> Unit, onTags: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                MenuRow(Icons.Default.PushPin, if (task.pinned) "Unpin" else "Pin to top") {
                    onDismiss()
                    onPin()
                }
                MenuRow(Icons.AutoMirrored.Filled.Label, "Tags") {
                    onDismiss()
                    onTags()
                }
                MenuRow(Icons.Default.DeleteOutline, "Delete") {
                    onDismiss()
                    onDelete()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
