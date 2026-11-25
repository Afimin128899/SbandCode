package app.sbandcode.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sbandcode.android.Brain
import app.sbandcode.android.tr

@Composable
fun WorkspaceScreen(brain: Brain, back: () -> Unit) {
    Page(tr("Инструменты", "Tools"), back) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Section(tr("Проект", "Project"))
            if (brain.open != null) ToolLink(brain, "changes",
                tr("Правки агента", "Agent changes"), tr("Список файлов и diff текущего чата", "Files and diff for the current chat"))
            ToolLink(brain, "git", "Git",
                tr("Статус, история, ветки и коммиты", "Status, history, branches and commits"))
            ToolLink(brain, "preview", tr("Веб-предпросмотр", "Web preview"),
                tr("Открыть локальный сайт", "Open a local site"))
            ToolLink(brain, "profiles", tr("Профили проектов", "Project profiles"),
                tr("Папки, модели и разрешения", "Folders, models and permissions"))
            Section(tr("Среда", "Environment"))
            ToolLink(brain, "linux", "Linux",
                if (brain.linux.installed) tr("Параметры и терминал Alpine", "Alpine settings and terminal")
                else tr("Установить Alpine", "Install Alpine"))
            ToolLink(brain, "tools", tr("Скиллы и MCP", "Skills and MCP"),
                tr("Управление расширениями агента", "Manage agent extensions"))
        }
    }
}

@Composable
private fun ToolLink(brain: Brain, route: String, title: String, detail: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth().clickable { brain.screens.add(route) },
    )
    HorizontalDivider()
}
