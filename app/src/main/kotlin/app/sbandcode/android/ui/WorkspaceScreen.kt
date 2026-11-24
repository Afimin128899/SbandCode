package app.sbandcode.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        Column(modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Section(tr("Проект", "Project"))
            if (brain.open != null) ToolLink(brain, "changes", Icons.Outlined.Description,
                tr("Правки агента", "Agent changes"), tr("Список файлов и diff текущего чата", "Files and diff for the current chat"))
            ToolLink(brain, "git", Icons.Outlined.Code, "Git",
                tr("Статус, история, ветки и коммиты", "Status, history, branches and commits"))
            ToolLink(brain, "preview", Icons.Outlined.Language, tr("Веб-предпросмотр", "Web preview"),
                tr("Открыть локальный сайт", "Open a local site"))
            ToolLink(brain, "profiles", Icons.Outlined.Folder, tr("Профили проектов", "Project profiles"),
                tr("Папки, модели и разрешения", "Folders, models and permissions"))
            Section(tr("Среда", "Environment"))
            ToolLink(brain, "linux", Icons.Outlined.Terminal, "Linux",
                if (brain.linux.installed) tr("Параметры и терминал Alpine", "Alpine settings and terminal")
                else tr("Установить Alpine", "Install Alpine"))
            ToolLink(brain, "tools", Icons.Outlined.Extension, tr("Скиллы и MCP", "Skills and MCP"),
                tr("Управление расширениями агента", "Manage agent extensions"))
        }
    }
}

@Composable
private fun ToolLink(brain: Brain, route: String, icon: ImageVector, title: String, detail: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
            .clickable { brain.screens.add(route) },
    )
}
