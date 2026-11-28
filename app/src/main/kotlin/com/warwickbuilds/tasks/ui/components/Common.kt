package com.warwickbuilds.tasks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.warwickbuilds.tasks.core.DueLabels
import com.warwickbuilds.tasks.core.DueTone
import com.warwickbuilds.tasks.core.Tag
import com.warwickbuilds.tasks.ui.theme.tagTone
import java.time.LocalDate

/** The round tick-off button on each task. */
@Composable
fun TaskCheck(done: Boolean, title: String, onToggle: () -> Unit, modifier: Modifier = Modifier, size: Int = 24) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(48.dp)
            .clickable(role = Role.Checkbox, onClickLabel = if (done) "Mark not done" else "Mark done", onClick = onToggle)
            .semantics { contentDescription = (if (done) "Done: " else "Not done: ") + title },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size.dp)
                .background(if (done) colors.primary else Color.Transparent, CircleShape)
                .border(2.dp, if (done) colors.primary else colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, null, tint = colors.onPrimary, modifier = Modifier.size((size - 8).dp))
        }
    }
}

/** The quiet due-date label; overdue gets a soft warning colour. */
@Composable
fun DueText(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val label = DueLabels.label(date, today)
    val colors = MaterialTheme.colorScheme
    Text(
        label.text,
        style = MaterialTheme.typography.labelMedium,
        color = when (label.tone) {
            DueTone.Overdue -> colors.error
            DueTone.Today -> colors.primary
            DueTone.Upcoming -> colors.onSurfaceVariant
        },
        modifier = modifier,
    )
}

/** A small rounded tag label. */
@Composable
fun TagPill(tag: Tag, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val tone = tagTone(tag.color)
    Box(
        modifier
            .background(tone.container, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(tag.name, style = MaterialTheme.typography.labelMedium, color = tone.content)
    }
}

/** Pick a task's tags, or add a new one. */
@Composable
fun TagPickerDialog(
    tags: List<Tag>,
    selected: Set<String>,
    onToggle: (tagId: String, on: Boolean) -> Unit,
    onAddTag: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(tags, key = { it.id }) { tag ->
                        val on = tag.id in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Checkbox) { onToggle(tag.id, !on) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.padding(end = 8.dp))
                            TagPill(tag)
                        }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New tag") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newName.isNotBlank()) onAddTag(newName.trim())
                        newName = ""
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newName.isNotBlank()) onAddTag(newName.trim())
                onDismiss()
            }) { Text("Done") }
        },
    )
}

/** A simple one-field dialog, for naming a tag. */
@Composable
fun NameDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name.trim()) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
