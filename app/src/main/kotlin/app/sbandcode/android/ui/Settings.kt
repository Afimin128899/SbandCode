package app.sbandcode.android.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sbandcode.android.BuildConfig
import app.sbandcode.android.Brain
import kotlinx.coroutines.CancellationException
import app.sbandcode.android.tr

@Composable
fun SettingsScreen(brain: Brain, back: () -> Unit) {
    var rules by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { rules = withContext(Dispatchers.IO) { brain.rulesFile.takeIf { it.exists() }?.readText().orEmpty() } }
    var wipe by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var transferring by remember { mutableStateOf(false) }
    var transferMessage by remember { mutableStateOf<String?>(null) }
    fun transfer(action: suspend () -> String) {
        if (transferring) return
        transferring = true
        transferMessage = null
        scope.launch {
            try {
                transferMessage = action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                transferMessage = e.message ?: tr("Операция не завершилась", "The operation did not complete")
            } finally {
                transferring = false
            }
        }
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { file ->
        if (file != null) transfer {
            val count = brain.exportChatBackup(file)
            tr("Чатов сохранено: $count", "Chats backed up: $count")
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { file ->
        if (file != null) transfer {
            val result = brain.importChatBackup(file)
            tr("Восстановлено: ${result.added}. Уже были в приложении: ${result.skipped}.",
                "Restored: ${result.added}. Already in the app: ${result.skipped}.")
        }
    }
    val openNotifications = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    Page(tr("Настройки", "Settings"), back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
            SettingsGroup(tr("Модели и подключение", "Models and connections")) {
                SettingsLink(Icons.Outlined.Key, tr("Свой API и модели", "Custom API and models"),
                    tr("URL, ключ и список моделей", "URL, key and model list")) { brain.screens.add("custom-api") }
                SettingsLink(Icons.Outlined.Hub, tr("Провайдеры и ключи API", "Providers and API keys"),
                    divider = true) { brain.screens.add("providers") }
                SettingsLink(Icons.Outlined.Storage, tr("Локальные модели", "Local models"),
                    divider = true) { brain.screens.add("local") }
                SettingsLink(Icons.Outlined.Speed, tr("Лимиты и использование", "Limits and usage"),
                    divider = true) { brain.screens.add("limits") }
            }

            var folders by remember { mutableStateOf(false) }
            var showRules by remember { mutableStateOf(false) }
            SettingsGroup(tr("Чат", "Chat")) {
                SettingsLink(Icons.Outlined.Notifications, tr("Уведомления", "Notifications"),
                    tr("Завершение ответа и запросы разрешений", "Completed replies and permission requests")) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else openNotifications()
                }
                SettingsLink(Icons.Outlined.Folder, tr("Папка для новых чатов", "Folder for new chats"),
                    brain.folderLabel(brain.folder().absolutePath), divider = true) { folders = true }
                SettingsLink(Icons.Outlined.EditNote, tr("Правила для модели", "Model instructions"),
                    "AGENTS.md", divider = true) { showRules = !showRules }
                AnimatedVisibility(showRules) {
                    rules?.let { r ->
                        Column(Modifier.padding(horizontal = 14.dp)) {
                            OutlinedTextField(r, { rules = it }, minLines = 3, maxLines = 10,
                                placeholder = { Text(tr("Например: отвечай кратко и по-русски", "For example: answer briefly")) },
                                modifier = Modifier.fillMaxWidth())
                            TextButton({
                                transfer {
                                    brain.saveRules(r)
                                    tr("Правила сохранены", "Instructions saved")
                                }
                            }, enabled = brain.ready && !transferring) { Text(tr("Сохранить правила", "Save instructions")) }
                        }
                    }
                }
            }
            if (folders) FolderDialog(brain) { folders = false }

            SettingsGroup(tr("Данные", "Data")) {
                SettingsLink(Icons.Outlined.FileDownload, tr("Сохранить все чаты", "Back up all chats"),
                    tr("Экспорт в JSON", "Export as JSON"), enabled = brain.ready && !transferring && brain.sessions.isNotEmpty()) {
                    backup.launch("SbandCode-chats.json")
                }
                SettingsLink(Icons.Outlined.FileUpload, tr("Восстановить из копии", "Restore from backup"),
                    divider = true, enabled = brain.ready && !transferring) {
                    restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }

            SettingsGroup("OpenCode") {
                SettingsLink(Icons.Outlined.Code, "opencode.json") { brain.screens.add("config") }
                SettingsLink(Icons.Outlined.Refresh, tr("Перезапустить OpenCode", "Restart OpenCode"), divider = true) {
                    brain.restart()
                }
                SettingsLink(Icons.Outlined.Delete, tr("Удалить все чаты", "Delete all chats"),
                    divider = true, enabled = brain.sessions.isNotEmpty() && !transferring) { wipe = true }
            }
            if (transferring || transferMessage != null) {
                Text(if (transferring) tr("Выполняется…", "Working…") else transferMessage.orEmpty(),
                    Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Section(tr("О приложении", "About"))
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Logo(44)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("SbandCode ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Afimin128899", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(tr("Внутри работает настоящий opencode 2.2.5 (MIT), собранный под Android на официальном Bun. Ключи и чаты хранятся только на телефоне.",
                    "Inside runs real opencode 2.2.5 (MIT), built for Android on the official Bun. Keys and chats stay on the phone."),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tr("Лицензия MIT", "MIT license"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton({ uri.openUri("https://github.com/Afimin128899/SbandCode") }) { Text("GitHub") }
                    TextButton({ uri.openUri("https://opencode.ai") }) { Text("opencode.ai") }
                    TextButton({ uri.openUri("https://models.dev") }) { Text("models.dev") }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (wipe) AlertDialog(
        onDismissRequest = { wipe = false },
        title = { Text(tr("Удалить все чаты?", "Delete every chat?")) },
        text = { Text(tr("Вернуть их будет нельзя.", "They can't be brought back.")) },
        confirmButton = { TextButton({ brain.sessions.toList().forEach { brain.delete(it) }; wipe = false }) { Text(tr("Удалить", "Delete")) } },
        dismissButton = { TextButton({ wipe = false }) { Text(tr("Отмена", "Cancel")) } },
    )
}


@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Section(title)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).blackGlass(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsLink(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    divider: Boolean = false,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f)
    if (divider) HorizontalDivider(Modifier.padding(start = 53.dp))
    ListItem(
        headlineContent = { Text(title, color = ink, style = MaterialTheme.typography.titleSmall) },
        supportingContent = detail?.let { { Text(it, color = if (enabled) scheme.onSurfaceVariant else ink) } },
        leadingContent = { Icon(icon, null, Modifier.size(21.dp), tint = ink) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(19.dp), tint = if (enabled) scheme.onSurfaceVariant else ink)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, onClick = action),
    )
}
