package app.sbandcode.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.tr

@Composable
fun WorkspaceScreen(brain: Brain, back: () -> Unit) {
    Page(tr("Инструменты", "Tools"), back) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Section(tr("Проект", "Project"))
            if (brain.open != null) ToolLink(brain, "changes", Icons.Outlined.Description,
                tr("Правки агента", "Agent changes"), tr("Список файлов и diff текущего чата", "Files and diff for the current chat"))
            ToolLink(brain, "git", Icons.Outlined.AccountTree, "Git",
                tr("Коммиты, ветки и отправка на GitHub", "Commits, branches and push to GitHub"))
            ToolLink(brain, "preview", Icons.Outlined.Language, tr("Веб-предпросмотр", "Web preview"),
                tr("Открыть локальный сайт", "Open a local site"))
            ToolLink(brain, "profiles", Icons.Outlined.FolderOpen, tr("Профили проектов", "Project profiles"),
                tr("Папки, модели и разрешения", "Folders, models and permissions"))
            Section(tr("Среда", "Environment"))
            ToolLink(brain, "linux", Icons.Outlined.Terminal, "Linux",
                if (brain.linux.installed) tr("Параметры и терминал Alpine", "Alpine settings and terminal")
                else tr("Установить Alpine", "Install Alpine"))
            ToolLink(brain, "tools", Icons.Outlined.Code, tr("Скиллы и MCP", "Skills and MCP"),
                tr("Управление расширениями агента", "Manage agent extensions"))
        }
    }
}

@Composable
private fun ToolLink(brain: Brain, route: String, icon: ImageVector, title: String, detail: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .graphitePanel().clickable { brain.screens.add(route) },
    )
}
