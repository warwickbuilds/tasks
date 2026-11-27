package com.warwickbuilds.tasks.ui.task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.core.DueLabels
import com.warwickbuilds.tasks.core.EditState
import com.warwickbuilds.tasks.core.Grouping
import com.warwickbuilds.tasks.core.MarkdownEditing
import com.warwickbuilds.tasks.core.Task
import com.warwickbuilds.tasks.tasksApp
import com.warwickbuilds.tasks.ui.components.ConfirmDialog
import com.warwickbuilds.tasks.ui.components.MarkdownView
import com.warwickbuilds.tasks.ui.components.TagPickerDialog
import com.warwickbuilds.tasks.ui.components.rememberToday
import com.warwickbuilds.tasks.ui.theme.MonoStyle
import com.warwickbuilds.tasks.ui.theme.tagTone
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Create ([taskId] null) or edit a task: title, Markdown description, due date, pin and tags. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(taskId: String?, initialTitle: String, initialTagId: String?, onClose: (savedId: String?) -> Unit) {
    val app = LocalContext.current.tasksApp
    val repo = app.repository
    val today = rememberToday()

    var original by remember { mutableStateOf<Task?>(null) }
    var ready by rememberSaveable { mutableStateOf(taskId == null) }
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialTitle, TextRange(initialTitle.length))) }
    var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var dueEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var tagIds by rememberSaveable { mutableStateOf(initialTagId?.let { listOf(it) } ?: emptyList()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTags by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val tags by repo.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(taskId) {
        if (taskId != null) {
            val t = repo.task(taskId)
            original = t
            if (t != null && !ready) {
                title = TextFieldValue(t.title, TextRange(t.title.length))
                description = TextFieldValue(t.description)
                dueEpochDay = t.dueDate?.toEpochDay()
                pinned = t.pinned
                tagIds = t.tagIds.toList()
                ready = true
            }
        }
    }

    val due = dueEpochDay?.let { LocalDate.ofEpochDay(it) }
    val changed = ready && (
        original?.let {
            it.title != title.text || it.description != description.text || it.dueDate != due ||
                it.pinned != pinned || it.tagIds != tagIds.toSet()
        } ?: (title.text.isNotBlank() || description.text.isNotBlank())
        )

    fun save() {
        if (title.text.isBlank()) return
        val now = System.currentTimeMillis()
        val base = original ?: Task(id = UUID.randomUUID().toString(), title = "", createdAt = now)
        val task = base.copy(title = title.text.trim(), description = description.text.trimEnd(), dueDate = due, pinned = pinned, tagIds = tagIds.toSet())
        app.appScope.launch { repo.save(task) }
        onClose(task.id)
    }

    fun close() {
        if (changed) confirmDiscard = true else onClose(null)
    }

    BackHandler(enabled = changed) { confirmDiscard = true }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { close() }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                Spacer(Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !preview,
                        onClick = { preview = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = {},
                    ) { Text("Write") }
                    SegmentedButton(
                        selected = preview,
                        onClick = { preview = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = {},
                    ) { Text("Preview") }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { save() }, enabled = title.text.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("Save") }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val titleFocus = remember { FocusRequester() }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.copy(text = it.text.replace("\n", " ")) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .focusRequester(titleFocus)
                        .semantics { contentDescription = "Title" },
                    decorationBox = { inner ->
                        if (title.text.isEmpty()) {
                            Text("Title", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        inner()
                    },
                )
                if (taskId == null) LaunchedEffect(Unit) { titleFocus.requestFocus() }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(
                        icon = Icons.Default.CalendarMonth,
                        label = due?.let { DueLabels.long(it, today) } ?: "Due date",
                        filled = false,
                        onClick = { pickingDate = true },
                        onClear = if (due != null) ({ dueEpochDay = null }) else null,
                    )
                    MetaChip(
                        icon = Icons.Default.PushPin,
                        label = if (pinned) "Pinned" else "Pin",
                        filled = pinned,
                        onClick = { pinned = !pinned },
                    )
                    tags.filter { it.id in tagIds }.sortedWith(Grouping.tagOrder).forEach { tag ->
                        val tone = tagTone(tag.color)
                        Box(
                            Modifier
                                .height(36.dp)
                                .background(tone.container, RoundedCornerShape(10.dp))
                                .clickable { pickingTags = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(tag.name, style = MaterialTheme.typography.labelLarge, color = tone.content) }
                    }
                    Box(
                        Modifier
                            .height(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { pickingTags = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("+ Tag", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                Spacer(Modifier.height(4.dp))
                if (preview) {
                    if (description.text.isBlank()) {
                        Text("Nothing to preview yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        MarkdownView(description.text, onToggle = { line ->
                            val toggled = com.warwickbuilds.tasks.core.Markdown.toggleCheck(description.text, line)
                            description = description.copy(text = toggled)
                        })
                    }
                } else {
                    Text("Description · Markdown", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = description,
                        onValueChange = { next ->
                            val continued = MarkdownEditing.continueList(description.text, next.toEditState())
                            description = continued?.toTextFieldValue() ?: next
                        },
                        textStyle = MonoStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .semantics { contentDescription = "Description" },
                        decorationBox = { inner ->
                            if (description.text.isEmpty()) {
                                Text("Notes, a - [ ] checklist, links…", style = MonoStyle, color = MaterialTheme.colorScheme.outline)
                            }
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (!preview) {
                Toolbar { action -> description = action(description.toEditState()).toTextFieldValue() }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    pickingDate = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dueEpochDay = null
                    pickingDate = false
                }) { Text("No date") }
            },
        ) { DatePicker(state = state) }
    }

    if (pickingTags) {
        TagPickerDialog(
            tags = tags,
            selected = tagIds.toSet(),
            onToggle = { id, on -> tagIds = if (on) (tagIds + id).distinct() else tagIds - id },
            onAddTag = { name ->
                scope.launch {
                    val tag = repo.addTag(name)
                    tagIds = (tagIds + tag.id).distinct()
                }
            },
            onDismiss = { pickingTags = false },
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your edits to this task won't be saved.",
            confirm = "Discard",
            onConfirm = {
                confirmDiscard = false
                onClose(null)
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun MetaChip(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .height(36.dp)
            .background(if (filled) colors.primaryContainer else colors.surfaceContainerLowest, shape)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.outlineVariant, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = if (onClear != null) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (filled) colors.primary else colors.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (filled) colors.onPrimaryContainer else colors.onSurfaceVariant)
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove due date", modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun Toolbar(apply: ((EditState) -> EditState) -> Unit) {
    val items: List<Triple<ImageVector, String, (EditState) -> EditState>> = listOf(
        Triple(Icons.Default.Title, "Heading", { s -> MarkdownEditing.prefixLines(s, "## ") }),
        Triple(Icons.Default.FormatBold, "Bold", { s -> MarkdownEditing.wrap(s, "**") }),
        Triple(Icons.Default.FormatItalic, "Italic", { s -> MarkdownEditing.wrap(s, "_") }),
        Triple(Icons.Default.CheckBox, "Checklist item", { s -> MarkdownEditing.prefixLines(s, "- [ ] ") }),
        Triple(Icons.AutoMirrored.Filled.FormatListBulleted, "Bulleted list", { s -> MarkdownEditing.prefixLines(s, "- ") }),
        Triple(Icons.Default.Link, "Link", { s -> MarkdownEditing.link(s) }),
        Triple(Icons.Default.Code, "Code", { s -> MarkdownEditing.wrap(s, "`") }),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { (icon, label, action) ->
            IconButton(onClick = { apply(action) }) { Icon(icon, contentDescription = label) }
        }
    }
}

private fun TextFieldValue.toEditState() = EditState(text, selection.start, selection.end)

private fun EditState.toTextFieldValue() = TextFieldValue(text, TextRange(start, end))
