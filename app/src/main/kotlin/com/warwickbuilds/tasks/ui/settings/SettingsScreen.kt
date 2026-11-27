package com.warwickbuilds.tasks.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warwickbuilds.tasks.core.BackupFormatException
import com.warwickbuilds.tasks.lib.UpdateChecker
import com.warwickbuilds.tasks.tasksApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val GITHUB = "https://github.com/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onManageTags: () -> Unit) {
    val context = LocalContext.current
    val app = context.tasksApp
    val repo = app.repository
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val wallpaper by app.settings.wallpaperColours.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    val version = UpdateChecker.installedVersion
    val github = GITHUB + UpdateChecker.repo

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
                "Exported. Keep the file somewhere safe."
            }.getOrElse { "Couldn't save the file." }
            snackbar.showSnackbar(message)
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                val (added, updated) = repo.importJson(text)
                when {
                    added == 0 && updated == 0 -> "Nothing new in that backup."
                    else -> "Imported: $added new, $updated updated."
                }
            } catch (e: BackupFormatException) {
                e.message ?: "That file isn't a Tasks backup."
            } catch (e: Exception) {
                "Couldn't read that file."
            }
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group("Organise") {
                SettingRow(Icons.AutoMirrored.Filled.Label, "Tags", "Add, rename, recolour and reorder", onClick = onManageTags)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        Icons.Default.Palette,
                        "Match wallpaper colours",
                        "Use your phone's Material You colours instead of Calm green",
                        right = { Switch(checked = wallpaper, onCheckedChange = null) },
                        role = Role.Switch,
                        onClick = { app.settings.setWallpaperColours(!wallpaper) },
                    )
                }
            }

            Group("Backup") {
                Body(
                    "Your tasks live only on this phone. Export them to a JSON file you keep (Downloads, a USB drive, anywhere), " +
                        "and import it on this or another phone. Importing adds to what's here; it never deletes anything.",
                )
                SettingRow(Icons.Default.Upload, "Export tasks", "Save a JSON backup file", onClick = { exporter.launch("tasks-backup-${LocalDate.now()}.json") })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Default.Download, "Import tasks", "Add tasks from a backup file", onClick = {
                    importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                })
            }

            Group("Updates") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Version $version", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tasks never checks the network in the background. Tap below to check GitHub Releases for a newer APK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (val result = update) {
                        UpdateChecker.Result.Current -> Text("You have the latest version.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        UpdateChecker.Result.Unavailable -> Text("Couldn't reach GitHub. Check your connection and try again.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        else -> Unit
                    }
                    val available = (update as? UpdateChecker.Result.Update)?.update
                    if (available != null) {
                        Text(
                            "Version ${available.version} is available. Download the APK and open it to install over this one. Your tasks stay in place, but exporting a backup first is a good idea.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = { openLink(context, available.downloadUrl) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${available.version}")
                        }
                        OutlinedButton(onClick = { openLink(context, available.pageUrl) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Release notes")
                        }
                    } else {
                        Button(
                            onClick = {
                                checking = true
                                scope.launch {
                                    update = UpdateChecker.check()
                                    checking = false
                                }
                            },
                            enabled = !checking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (checking) "Checking…" else "Check for updates")
                        }
                    }
                }
            }

            Group("About") {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tasks $version", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A simple, open-source to-do list for Android (MIT License). Your tasks stay on this phone: " +
                                "no account, no cloud sync, no ads, no analytics and no tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(Icons.Default.Code, "GitHub repository", "github.com/${UpdateChecker.repo}") { openLink(context, github) }
                LinkRow(Icons.Default.NewReleases, "Releases and changelog", null) { openLink(context, "$github/releases") }
                LinkRow(Icons.Default.Shield, "Privacy", "Nothing is collected") { openLink(context, "$github/blob/main/PRIVACY.md") }
                LinkRow(Icons.Default.Gavel, "Licence", "MIT") { openLink(context, "$github/blob/main/LICENSE") }
                LinkRow(Icons.Default.BugReport, "Report a problem", "Open an issue on GitHub") { openLink(context, "$github/issues") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(20.dp)),
        ) { content() }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    right: @Composable () -> Unit = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
    role: Role = Role.Button,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        right()
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    SettingRow(icon, title, subtitle, right = {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }, onClick = onClick)
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser installed; nothing else to do.
    }
}
