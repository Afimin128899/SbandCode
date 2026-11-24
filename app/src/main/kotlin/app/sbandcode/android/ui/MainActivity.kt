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
    AnimatedContent(top, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { screen ->
        val back = { if (brain.screens.isNotEmpty()) brain.screens.removeAt(brain.screens.lastIndex) }
        when {
            screen == null -> ModalNavigationDrawer(
                drawerState = drawer,
                drawerContent = { Drawer(brain) { scope.launch { drawer.close() } } },
            ) {
                ChatScreen(brain, openDrawer = { scope.launch { drawer.open() } })
            }
            screen == "providers" -> ProvidersScreen(brain, back)
            screen.startsWith("provider:") -> ProviderScreen(brain, screen.removePrefix("provider:"), back)
            screen == "config" -> ConfigScreen(brain, back)
            screen == "settings" -> SettingsScreen(brain, back)
            screen == "limits" -> LimitsScreen(brain, back)
            screen == "tools" -> ToolsScreen(brain, back)
            screen == "linux" -> LinuxScreen(brain, back)
            screen == "account" -> AccountScreen(brain, back)
            screen == "local" -> LocalScreen(brain, back)
            screen == "search" -> ChatSearchScreen(brain, back)
            screen == "profiles" -> ProfilesScreen(brain, back)
            screen == "preview" -> PreviewScreen(brain, back)
            screen == "git" -> GitScreen(brain, back)
            screen == "changes" -> ChangesScreen(brain, back)
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
