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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.IntOffset
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
    private var windowActive by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        setContent {
            val systemMotion by rememberSystemMotion()
            SbandTheme {
                CompositionLocalProvider(LocalGlassMotion provides (systemMotion && windowActive)) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                        Box(Modifier.fillMaxSize()) {
                            GlassBackdrop(Modifier.matchParentSize())
                            Root(brain)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        windowActive = true
        brain.foreground = true
        brain.checkFiles()
    }

    override fun onPause() {
        windowActive = false
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
    val motion = LocalGlassMotion.current
    val showTabs = (top == null || top == "workspace" || top == "settings") &&
        WindowInsets.ime.getBottom(LocalDensity.current) == 0 && !drawer.isOpen
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f).then(
            if (showTabs) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
        )) {
            AnimatedContent(top, transitionSpec = {
                if (!motion) EnterTransition.None togetherWith ExitTransition.None
                else (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(190)) { -it / 14 })
            }, label = "screen") { screen ->
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
    val motion = LocalGlassMotion.current
    val tabs = listOf(
        Triple(null, tr("Чат", "Chat"), Icons.Outlined.ChatBubbleOutline),
        Triple("workspace", tr("Инструменты", "Tools"), Icons.Outlined.Build),
        Triple("settings", tr("Настройки", "Settings"), Icons.Outlined.Settings),
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val tabShape = RoundedCornerShape(21.dp)
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier.widthIn(max = 510.dp).fillMaxWidth().blackGlass(RoundedCornerShape(27.dp), prominent = true).padding(5.dp),
        ) {
            val tabWidth = maxWidth / tabs.size
            val x by animateDpAsState(
                tabWidth * selectedIndex,
                animationSpec = if (motion) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else snap(),
                label = "selected tab",
            )
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset { IntOffset(x.roundToPx(), 0) }.width(tabWidth).fillMaxHeight().padding(horizontal = 1.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF50565E), Color(0xFF282C31))), tabShape)
                    .border(1.dp, Brush.verticalGradient(listOf(Color(0x73CBD5E0), Color(0x22BBC4CF))), tabShape))
            }
            Row(Modifier.selectableGroup().fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for ((route, label, icon) in tabs) {
                    val active = route == selected
                    val ink = if (active) scheme.onSurface else scheme.onSurfaceVariant
                    val lift by animateDpAsState(
                        if (active) (-1).dp else 1.dp,
                        animationSpec = if (motion) tween(190) else snap(), label = "tab icon",
                    )
                    Column(
                        Modifier.weight(1f).clip(tabShape).selectable(active, role = Role.Tab) { onSelect(route) }
                            .heightIn(min = 53.dp).padding(horizontal = 3.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Icon(icon, null, Modifier.offset { IntOffset(0, lift.roundToPx()) }.size(19.dp), tint = ink)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
                    }
                }
            }
        }
    }
}
