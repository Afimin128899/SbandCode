package app.sbandcode.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.tr

/** The folder a chat works in. A new chat's can be changed; an open one shows where it works. */
@Composable
fun FolderChip(brain: Brain) {
    var dialog by remember { mutableStateOf(false) }
    val session = brain.session()
    val label = if (session != null) brain.folderLabel(session.directory) else brain.folderLabel(brain.folder().absolutePath)
    val description = tr("Рабочая папка: $label", "Working folder: $label")
    Surface(color = Color.Transparent, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).semantics(mergeDescendants = true) {
            contentDescription = description
        }.then(if (session == null) Modifier.clickable(
            role = Role.Button, onClickLabel = tr("Выбрать папку", "Choose folder"),
        ) { dialog = true } else Modifier)) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp).widthIn(max = 220.dp))
            if (session == null) Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
    if (dialog) FolderDialog(brain) { dialog = false }
}

@Composable
fun FolderDialog(brain: Brain, close: () -> Unit) {
    val context = LocalContext.current
    val phone = brain.phone.absolutePath
    var choice by remember {
        mutableStateOf(when (brain.prefs.folder) { "" -> "app"; phone -> "phone"; else -> "own" })
    }
    var path by remember { mutableStateOf(brain.prefs.folder.takeIf { it.isNotEmpty() && it != phone } ?: "$phone/SbandCode") }
    fun askFiles() {
        if (Build.VERSION.SDK_INT >= 30) runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + context.packageName)))
        }.onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Outlined.Folder, null) },
        title = { Text(tr("Где работает агент", "Where the agent works")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("В этой папке модель читает, создаёт и меняет файлы и запускает команды. Для новых чатов.",
                    "In this folder the model reads, creates and changes files and runs commands. For new chats."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.selectableGroup()) {
                    for ((v, label) in listOf(
                        "app" to tr("Папка приложения (видна только через adb)", "App folder (reachable only through adb)"),
                        "phone" to tr("Вся память телефона", "The whole phone storage"),
                        "own" to tr("Своя папка", "A folder of your own"),
                    )) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = choice == v, role = Role.RadioButton, onClick = { choice = v },
                    ), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(choice == v, onClick = null)
                        Text(label, Modifier.weight(1f))
                    }
                }
                if (choice == "own") OutlinedTextField(path, { path = it.trim() }, singleLine = true,
                    label = { Text(tr("Путь к папке", "Folder path")) }, modifier = Modifier.fillMaxWidth())
                if (choice != "app" && !brain.filesGranted) {
                    Text(tr("Нужно разрешение «Доступ ко всем файлам».", "This needs the “Access all files” permission."),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp))
                    TextButton(::askFiles) { Text(tr("Разрешить", "Allow")) }
                }
            }
        },
        confirmButton = {
            TextButton({
                val folder = when (choice) { "phone" -> phone; "own" -> path; else -> "" }
                brain.update { it.copy(folder = folder) }
                if (folder.isNotEmpty() && !brain.filesGranted) askFiles()
                close()
            }) { Text(tr("Готово", "Done")) }
        },
        dismissButton = { TextButton(close) { Text(tr("Отмена", "Cancel")) } },
    )
}
