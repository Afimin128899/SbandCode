package app.sbandcode.android.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    var advanced by rememberSaveable { mutableStateOf(false) }
    var savingRules by remember { mutableStateOf(false) }
    var rulesMessage by remember { mutableStateOf<String?>(null) }
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
            Text(tr("Подключите модель, выберите рабочую папку и настройте агента под себя.",
                "Connect a model, choose a working folder and make the agent your own."),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            Section(tr("Модели и подключение", "Models and connections"))
            ActionRow(tr("Подписки и ключи API", "Subscriptions and API keys"),
                tr("OpenAI, Claude, OpenCode и другие провайдеры", "OpenAI, Claude, OpenCode and other providers"),
                Icons.Outlined.CloudQueue, { brain.screens.add("providers") })
            ActionRow(tr("Свой API и модели", "Custom API and models"),
                tr("Подключение по адресу сервера и ключу", "Connect with a server address and key"),
                Icons.Outlined.Api, { brain.screens.add("custom-api") })
            ActionRow(tr("Модели на телефоне", "On-device models"),
                tr("Загрузка и запуск локальных моделей", "Download and run local models"),
                Icons.Outlined.Memory, { brain.screens.add("local") })
            ActionRow(tr("Лимиты и использование", "Limits and usage"),
                tr("Расход токенов и доступные лимиты", "Token usage and available limits"),
                Icons.Outlined.DataUsage, { brain.screens.add("limits") })

            Section(tr("Работа с проектом", "Working with a project"))
            var folders by remember { mutableStateOf(false) }
            ActionRow(tr("Папка для новых чатов", "Folder for new chats"),
                brain.folderLabel(brain.folder().absolutePath), Icons.Outlined.FolderOpen, { folders = true })
            if (folders) FolderDialog(brain) { folders = false }
            Section(tr("Уведомления", "Notifications"))
            ActionRow(tr("Уведомления агента", "Agent notifications"),
                tr("Когда задача завершена или нужно разрешение", "When a task finishes or permission is needed"),
                Icons.Outlined.NotificationsNone, {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else openNotifications()
            })

            Section(tr("Правила для модели", "Rules for the model"))
            Text(tr("Как отвечать и работать с кодом во всех чатах. Сохраняется в общем AGENTS.md.",
                "How to respond and work with code in every chat. Saved in the global AGENTS.md."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            rules?.let { r ->
                OutlinedTextField(r, { rules = it; rulesMessage = null }, enabled = !savingRules, minLines = 3, maxLines = 12,
                    label = { Text(tr("Инструкции для агента", "Agent instructions")) },
                    placeholder = { Text(tr("Например: отвечай кратко и по-русски", "For example: answer briefly")) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                Button({
                    savingRules = true
                    scope.launch {
                        try {
                            brain.saveRules(r)
                            rulesMessage = tr("Правила сохранены", "Rules saved")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            rulesMessage = tr("Не удалось сохранить правила: ", "Could not save rules: ") + e.message.orEmpty()
                        } finally {
                            savingRules = false
                        }
                    }
                }, enabled = !savingRules, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(if (savingRules) tr("Сохранение…", "Saving…") else tr("Сохранить правила", "Save rules"))
                }
                rulesMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }
            }

            Section(tr("Чаты и резервная копия", "Chats and backup"))
            ActionRow(tr("Сохранить все чаты", "Back up all chats"),
                tr("Экспорт в файл на телефоне", "Export to a file on your phone"),
                Icons.Outlined.Download, { backup.launch("SbandCode-chats.json") },
                enabled = brain.ready && !transferring && brain.sessions.isNotEmpty())
            ActionRow(tr("Восстановить чаты", "Restore chats"),
                tr("Добавить чаты из файла резервной копии", "Add chats from a backup file"),
                Icons.Outlined.Upload, { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                enabled = brain.ready && !transferring)
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transferring) Text(tr("Обработка чатов…", "Processing chats…"))
                transferMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }

            TextButton({ advanced = !advanced }, Modifier.padding(horizontal = 12.dp, vertical = 12.dp).semantics {
                stateDescription = if (advanced) tr("Развёрнуто", "Expanded") else tr("Свёрнуто", "Collapsed")
            }) {
                Text(tr("Дополнительные настройки", "Advanced settings"))
                Icon(if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            AnimatedVisibility(advanced) {
                Column {
                    ActionRow(tr("Конфигурация opencode", "opencode configuration"),
                        tr("Редактировать opencode.json вручную", "Edit opencode.json manually"),
                        Icons.Outlined.Settings, { brain.screens.add("config") })
                    ActionRow(tr("Перезапустить движок", "Restart the engine"),
                        tr("Прервёт выполняющиеся задачи", "Interrupts running tasks"),
                        Icons.Outlined.RestartAlt, { brain.restart() })
                    ActionRow(tr("Удалить все чаты", "Delete all chats"),
                        tr("Безвозвратно. Сначала сохраните резервную копию", "Permanent. Save a backup first"),
                        Icons.Outlined.DeleteOutline, { wipe = true }, enabled = brain.sessions.isNotEmpty(), destructive = true)
                }
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
                Column {
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
        confirmButton = { TextButton({ brain.sessions.toList().forEach { brain.delete(it) }; wipe = false }) {
            Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton({ wipe = false }) { Text(tr("Отмена", "Cancel")) } },
    )
}
