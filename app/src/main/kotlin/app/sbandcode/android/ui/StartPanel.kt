package app.sbandcode.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sbandcode.android.tr

@Composable
fun StartPanel(
    onPrompt: (String) -> Unit,
    onFiles: () -> Unit,
    onProviders: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Box(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().blackGlass(RoundedCornerShape(32.dp), prominent = true)
                    .heightIn(min = 168.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                LiquidRibbons(Modifier.matchParentSize())
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Logo(52)
                    Text(tr("С чего начнём?", "Where do we start?"), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (onProviders != null) {
            item {
                StartAction(Icons.Outlined.Code, tr("Подключить модель", "Connect a model"),
                    Modifier.widthIn(max = 520.dp).fillMaxWidth(), onProviders)
            }
        } else {
            item {
                Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StartAction(Icons.Outlined.Code, tr("Разобрать код", "Explain code"), Modifier.weight(1f)) {
                        onPrompt(tr("Объясни, как работает этот код:\n", "Explain how this code works:\n"))
                    }
                    StartAction(Icons.Outlined.BugReport, tr("Найти ошибку", "Debug an error"), Modifier.weight(1f)) {
                        onPrompt(tr("Помоги разобраться с этой ошибкой:\n", "Help me debug this error:\n"))
                    }
                }
            }
        }
        item {
            Row(Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StartAction(Icons.Outlined.AttachFile, tr("Добавить файл", "Attach a file"), Modifier.weight(1f), onFiles)
                StartAction(Icons.Outlined.Extension, tr("Установить скилл", "Install a skill"), Modifier.weight(1f)) {
                    onPrompt("/skill ")
                }
            }
        }
    }
}

@Composable
private fun StartAction(icon: ImageVector, title: String, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val motion = LocalGlassMotion.current
    val scale by animateFloatAsState(
        if (pressed && motion) 0.97f else 1f,
        animationSpec = if (motion) spring() else snap(), label = "shortcut press",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale).blackGlass(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        interactionSource = source,
    ) {
        Column(Modifier.heightIn(min = 76.dp).padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
