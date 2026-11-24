package app.sbandcode.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.sbandcode.android.Brain
import app.sbandcode.android.engine.ChatNotifications
import app.sbandcode.android.tr
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val brain: Brain by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        setContent {
            SbandTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) { Root(brain) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        brain.foreground = true
        brain.checkFiles()
    }

    override fun onPause() {
        brain.foreground = false
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        if (intent?.action == ChatNotifications.OPEN_CHAT) {
            val id = intent.data?.lastPathSegment?.takeIf { it.matches(Regex("ses_[a-zA-Z0-9_-]+")) } ?: return
            brain.screens.clear()
            brain.openSession(id)
            return
        }
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val uri = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        lifecycleScope.launch {
            val attachment = uri?.takeIf { it.scheme == "content" }?.let {
                if (intent.type?.startsWith("image/") == true) brain.importPicture(it)
                else brain.importSharedFile(it)
            }
            if (text.isNotBlank() || attachment != null) brain.incomingShare = text to listOfNotNull(attachment)
            else if (uri != null) {
                brain.screens.clear()
                brain.notice = tr("Не удалось открыть вложение", "Could not open attachment")
            }
        }
    }
}

@Composable
private fun Root(brain: Brain) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val top = brain.screens.lastOrNull()
    BackHandler(top != null) { brain.screens.removeAt(brain.screens.lastIndex) }
    BackHandler(top == null && drawer.isOpen) { scope.launch { drawer.close() } }
    val saved = rememberSaveableStateHolder()
    val showTabs = (top == null || top == "workspace" || top == "settings") &&
        WindowInsets.ime.getBottom(LocalDensity.current) == 0 && !drawer.isOpen
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f).then(
            if (showTabs) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
        )) {
            AnimatedContent(top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { screen ->
                val back = { if (brain.screens.isNotEmpty()) brain.screens.removeAt(brain.screens.lastIndex) }
                when {
                    screen == null -> saved.SaveableStateProvider("chat") {
                        ModalNavigationDrawer(
                            drawerState = drawer,
                            drawerContent = { Drawer(brain) { scope.launch { drawer.close() } } },
                        ) {
                            ChatScreen(brain, openDrawer = { scope.launch { drawer.open() } })
                        }
                    }
                    screen == "workspace" -> WorkspaceScreen(brain, back)
                    screen == "custom-api" -> CustomApiScreen(brain, back)
                    screen == "providers" -> ProvidersScreen(brain, back)
                    screen.startsWith("provider:") -> ProviderScreen(brain, screen.removePrefix("provider:"), back)
                    screen == "config" -> ConfigScreen(brain, back)
                    screen == "settings" -> SettingsScreen(brain, back)
                    screen == "limits" -> LimitsScreen(brain, back)
                    screen == "tools" -> ToolsScreen(brain, back)
                    screen == "linux" -> LinuxScreen(brain, back)
                    screen == "local" -> LocalScreen(brain, back)
                    screen == "search" -> ChatSearchScreen(brain, back)
                    screen == "profiles" -> ProfilesScreen(brain, back)
                    screen == "preview" -> PreviewScreen(brain, back)
                    screen == "git" -> GitScreen(brain, back)
                    screen == "changes" -> ChangesScreen(brain, back)
                }
            }
        }
        if (showTabs) {
            MainDock(top) { route ->
                if (top != route) {
                    brain.screens.clear()
                    if (route != null) brain.screens.add(route)
                }
            }
        }
    }
    if (brain.incomingShare != null) AlertDialog(
        onDismissRequest = { brain.incomingShare = null },
        title = { Text(tr("Поделиться в SbandCode", "Share to SbandCode")) },
        text = { Text(tr("Куда добавить текст или вложение?", "Where should the shared content go?")) },
        confirmButton = { TextButton({ brain.acceptShare(true) }) { Text(tr("Новый чат", "New chat")) } },
        dismissButton = {
            if (brain.open != null) TextButton({ brain.acceptShare(false) }) { Text(tr("Текущий чат", "Current chat")) }
            else TextButton({ brain.incomingShare = null }) { Text(tr("Отмена", "Cancel")) }
        },
    )
}


@Composable
private fun MainDock(selected: String?, onSelect: (String?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 3.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 510.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerLow,
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Row(Modifier.selectableGroup().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((route, label, icon) in listOf(
                    Triple(null, tr("Чат", "Chat"), Icons.Outlined.ChatBubbleOutline),
                    Triple("workspace", tr("Инструменты", "Tools"), Icons.Outlined.Build),
                    Triple("settings", tr("Настройки", "Settings"), Icons.Outlined.Settings),
                )) {
                    val active = route == selected
                    val color by animateColorAsState(
                        if (active) scheme.surfaceContainerHighest else scheme.surfaceContainerLow, label = "tab",
                    )
                    val ink = if (active) scheme.onSurface else scheme.onSurfaceVariant
                    val shape = RoundedCornerShape(19.dp)
                    Surface(color = color, shape = shape,
                        modifier = Modifier.weight(1f).clip(shape).selectable(active, role = Role.Tab) { onSelect(route) }) {
                        Column(Modifier.heightIn(min = 52.dp).padding(horizontal = 3.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                            Icon(icon, null, Modifier.size(18.dp), tint = ink)
                            Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
                        }
                    }
                }
            }
        }
    }
}
