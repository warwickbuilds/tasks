package com.warwickbuilds.tasks.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.core.StarterTags
import com.warwickbuilds.tasks.core.Tag
import com.warwickbuilds.tasks.tasksApp
import com.warwickbuilds.tasks.ui.components.ConfirmDialog
import com.warwickbuilds.tasks.ui.components.NameDialog
import com.warwickbuilds.tasks.ui.components.TagPill
import com.warwickbuilds.tasks.ui.theme.allTagTones
import kotlinx.coroutines.launch

/** Add, rename, recolour, reorder and delete tags. Today can be recoloured and moved, not renamed or deleted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.tasksApp
    val repo = app.repository
    val tags by repo.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    var renaming by remember { mutableStateOf<Tag?>(null) }
    var colouring by remember { mutableStateOf<Tag?>(null) }
    var deleting by remember { mutableStateOf<Tag?>(null) }
    var adding by remember { mutableStateOf(false) }
    fun write(block: suspend () -> Unit) {
        app.appScope.launch { block() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New tag") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Text(
                    "Tags group your tasks. They show as chips on the main screen, in this order, after All and Due. Type #name when adding a task to tag it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            itemsIndexed(tags, key = { _, t -> t.id }) { index, tag ->
                val isToday = tag.id == StarterTags.TODAY_ID
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clickable { colouring = tag }
                            .semantics { contentDescription = "Change colour of ${tag.name}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(20.dp).background(allTagTones()[Math.floorMod(tag.color, allTagTones().size)].content, CircleShape))
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        TagPill(tag)
                        if (isToday) {
                            Text(
                                "Shows on the home-screen widget, with tasks due today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    IconButton(onClick = { write { repo.moveTag(tag.id, -1) } }, enabled = index > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${tag.name} earlier")
                    }
                    IconButton(onClick = { write { repo.moveTag(tag.id, 1) } }, enabled = index < tags.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${tag.name} later")
                    }
                    if (!isToday) {
                        IconButton(onClick = { renaming = tag }) { Icon(Icons.Default.Edit, contentDescription = "Rename ${tag.name}") }
                        IconButton(onClick = { deleting = tag }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${tag.name}") }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 68.dp))
            }
        }
    }

    if (adding) {
        NameDialog("New tag", "", "Add", onConfirm = { name ->
            adding = false
            write { repo.addTag(name) }
        }, onDismiss = { adding = false })
    }
    renaming?.let { tag ->
        NameDialog("Rename tag", tag.name, "Save", onConfirm = { name ->
            renaming = null
            write { repo.updateTag(tag.copy(name = name)) }
        }, onDismiss = { renaming = null })
    }
    deleting?.let { tag ->
        ConfirmDialog(
            title = "Delete ${tag.name}?",
            text = "The tag comes off its tasks. The tasks themselves stay.",
            confirm = "Delete",
            onConfirm = {
                deleting = null
                write { repo.deleteTag(tag.id) }
            },
            onDismiss = { deleting = null },
        )
    }
    colouring?.let { tag -> ColourDialog(tag, onPick = { c ->
        colouring = null
        write { repo.updateTag(tag.copy(color = c)) }
    }, onDismiss = { colouring = null }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColourDialog(tag: Tag, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val tones = allTagTones()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colour for ${tag.name}") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tones.forEachIndexed { i, tone ->
                    val selected = Math.floorMod(tag.color, tones.size) == i
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(tone.container, CircleShape)
                            .then(if (selected) Modifier.border(3.dp, tone.content, CircleShape) else Modifier)
                            .clickable { onPick(i) }
                            .semantics { contentDescription = "Colour ${i + 1}" + if (selected) ", selected" else "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = tone.content)
                        else Box(Modifier.size(14.dp).background(tone.content, CircleShape))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
