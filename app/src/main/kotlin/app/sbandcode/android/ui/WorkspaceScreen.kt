package app.sbandcode.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.tr

@Composable
fun WorkspaceScreen(brain: Brain, back: () -> Unit) {
    Page(tr("Инструменты", "Tools"), back) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val session = brain.session()
            val projectFolder = brain.folderLabel(session?.directory ?: brain.folder().absolutePath)
            ToolTile(
                Icons.Outlined.Folder,
                projectFolder,
                tr("Профили проектов и разрешения", "Project profiles and permissions"),
                Modifier.fillMaxWidth(),
                highlighted = true,
            ) { brain.screens.add("profiles") }
            if (brain.open != null) ToolTile(
                Icons.Outlined.Description, tr("Правки агента", "Agent changes"),
                tr("Список файлов и diff текущего чата", "Files and diff for the current chat"),
                Modifier.fillMaxWidth(),
            ) { brain.screens.add("changes") }

            Text(tr("Проект", "Project"), Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile(Icons.Outlined.Code, "Git", tr("Ветки и коммиты", "Branches and commits"), Modifier.weight(1f)) {
                    brain.screens.add("git")
                }
                ToolTile(Icons.Outlined.Language, tr("Веб-предпросмотр", "Web preview"),
                    tr("Открыть локальный сайт", "Open a local site"), Modifier.weight(1f)) {
                    brain.screens.add("preview")
                }
            }

            Text(tr("Среда", "Environment"), Modifier.padding(top = 5.dp), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile(Icons.Outlined.Terminal, "Linux",
                    if (brain.linux.installed) tr("Терминал Alpine", "Alpine terminal") else tr("Установить Alpine", "Install Alpine"),
                    Modifier.weight(1f)) { brain.screens.add("linux") }
                ToolTile(Icons.Outlined.Extension, tr("Скиллы и MCP", "Skills and MCP"),
                    tr("Расширения агента", "Agent extensions"), Modifier.weight(1f)) { brain.screens.add("tools") }
            }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalGlassMotion.current
    val scale by animateFloatAsState(
        if (motion && pressed) 0.975f else 1f,
        animationSpec = if (motion) spring() else snap(), label = "tool press",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale).blackGlass(RoundedCornerShape(24.dp), highlighted),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        interactionSource = source,
    ) {
        Column(Modifier.heightIn(min = 123.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(11.dp), color = scheme.surfaceContainerHighest) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(19.dp)) }
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
            }
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(detail, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
