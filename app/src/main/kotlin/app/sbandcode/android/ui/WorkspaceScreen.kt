package app.sbandcode.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.tr

@Composable
fun WorkspaceScreen(brain: Brain, back: () -> Unit) {
    Page(tr("Инструменты", "Tools"), back) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Text(tr("Всё для работы с проектом: файлы, история изменений и среда запуска.",
                "Work with your project files, change history and runtime."),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            Section(tr("Текущий проект", "Current project"))
            Column(Modifier.padding(horizontal = 12.dp)) { FolderChip(brain) }
            ActionRow(tr("Изменённые файлы", "Changed files"),
                if (brain.open != null) tr("Посмотреть правки агента в этом чате", "Review the agent's changes in this chat")
                else tr("Будут доступны после начала чата", "Available once you start a chat"),
                Icons.Outlined.Code, { brain.screens.add("changes") }, enabled = brain.open != null)
            ActionRow(tr("История и версии Git", "Git history and versions"),
                tr("Проверить статус, переключить ветку или создать коммит", "Check status, switch branches or create a commit"),
                Icons.Outlined.AccountTree, { brain.screens.add("git") })
            ActionRow(tr("Открыть сайт проекта", "Open project website"),
                tr("Предпросмотр сайта, запущенного на телефоне", "Preview a website running on your phone"),
                Icons.Outlined.Language, { brain.screens.add("preview") })
            ActionRow(tr("Профили проектов", "Project profiles"),
                tr("Сохранённые папки, модели и разрешения", "Saved folders, models and permissions"),
                Icons.Outlined.FolderOpen, { brain.screens.add("profiles") })
            Section(tr("Среда и расширения", "Environment and extensions"))
            ActionRow(tr("Linux и терминал", "Linux and terminal"),
                if (brain.linux.installed) tr("Команды и пакеты Alpine Linux", "Alpine Linux commands and packages")
                else tr("Установить Alpine для запуска команд", "Install Alpine to run commands"),
                Icons.Outlined.Terminal, { brain.screens.add("linux") })
            ActionRow(tr("Навыки и инструменты MCP", "Skills and MCP tools"),
                tr("Дополнительные инструкции и подключения для агента", "Extra instructions and connections for the agent"),
                Icons.Outlined.Extension, { brain.screens.add("tools") })
            Spacer(Modifier.height(24.dp))
        }
    }
}
