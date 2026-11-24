package app.sbandcode.android.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedButton
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
    val openNotifications = {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    Page(tr("Настройки", "Settings"), back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
            Section(tr("Модели и подключение", "Models and connections"))
            for ((destination, label) in listOf(
                "providers" to tr("Провайдеры и ключи API", "Providers and API keys"),
                "local" to tr("Локальные модели", "Local models"),
                "limits" to tr("Лимиты и использование", "Limits and usage"),
            )) {
                TextButton({ brain.screens.add(destination) }, Modifier.padding(start = 12.dp)) { Text(label) }
            }

            Section(tr("Уведомления", "Notifications"))
            TextButton({
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else openNotifications()
            }, Modifier.padding(start = 12.dp)) {
                Text(tr("Уведомления о завершении и разрешениях", "Completion and permission notifications"))
            }

            Section(tr("Правила для модели", "Rules for the model"))
            Text(tr("Общий AGENTS.md opencode: что модель должна помнить в каждом чате.",
                "opencode's global AGENTS.md: what the model keeps in mind in every chat."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            rules?.let { r ->
                OutlinedTextField(r, { rules = it }, minLines = 3, maxLines = 12,
                    placeholder = { Text(tr("Например: отвечай кратко и по-русски", "For example: answer briefly")) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                TextButton({ scope.launch { runCatching { brain.saveRules(r) } } }, Modifier.padding(start = 12.dp)) {
                    Text(tr("Сохранить правила", "Save the rules"))
                }
            }

            Section(tr("Файлы", "Files"))
            var folders by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clickable { folders = true }.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Папка для новых чатов", "Folder for new chats"))
                    Text(brain.folderLabel(brain.folder().absolutePath), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (folders) FolderDialog(brain) { folders = false }
            Section(tr("Данные", "Data"))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ brain.screens.add("config") }) { Text(tr("Настройки opencode (opencode.json)", "opencode settings (opencode.json)")) }
                OutlinedButton({ brain.restart() }) { Text(tr("Перезапустить opencode", "Restart opencode")) }
                OutlinedButton({ wipe = true }, enabled = brain.sessions.isNotEmpty()) { Text(tr("Удалить все чаты", "Delete every chat")) }
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
