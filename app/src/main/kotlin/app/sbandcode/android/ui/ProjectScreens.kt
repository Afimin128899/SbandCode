package app.sbandcode.android.ui

import android.annotation.SuppressLint
import android.graphics.Color
import java.util.concurrent.atomic.AtomicReference
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.sbandcode.android.Brain
import app.sbandcode.android.data.ProjectProfile
import app.sbandcode.android.data.str
import app.sbandcode.android.engine.ProjectGit
import app.sbandcode.android.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

@Composable
fun ChatSearchScreen(brain: Brain, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var results by remember { mutableStateOf<List<Brain.SearchHit>?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    val projects = brain.sessions.mapNotNull { it.directory }.distinct().sorted()

    LaunchedEffect(submitted) {
        val search = submitted ?: return@LaunchedEffect
        results = null
        problem = null
        try {
            results = brain.searchChats(search.first, search.second)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problem = e.message ?: e.toString()
        }
    }

    Page(tr("Поиск по чатам", "Search chats"), back) { mod ->
        Column(mod.navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(tr("Текст или заголовок", "Text or title")) })
            Choice(tr("Проект", "Project"), projects.map { it to brain.folderLabel(it) }, filter) { filter = it }
            Button({ submitted = query.trim() to filter }, enabled = query.isNotBlank() && brain.ready) {
                Text(tr("Найти", "Search"))
            }
            problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (submitted != null && results == null && problem == null) CircularProgressIndicator()
            if (results?.isEmpty() == true) Text(tr("Совпадений нет", "No matches"))
            LazyColumn(Modifier.weight(1f)) {
                items(results.orEmpty()) { hit ->
                    TextButton({ brain.openHit(hit) }, Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(hit.session.title, style = MaterialTheme.typography.titleSmall)
                            Text(hit.excerpt, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ProfilesScreen(brain: Brain, back: () -> Unit) {
    var editor by remember { mutableStateOf<ProjectProfile?>(null) }
    var folders by remember { mutableStateOf(false) }
    val current = brain.folder().absolutePath
    Page(tr("Профили проектов", "Project profiles"), back) { mod ->
        LazyColumn(mod.navigationBarsPadding().imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text(brain.folderLabel(current))
                Row {
                    TextButton({ folders = true }) { Text(tr("Выбрать папку", "Choose folder")) }
                    TextButton({
                        editor = brain.prefs.profiles.firstOrNull { it.directory == current }
                            ?: ProjectProfile(current, brain.currentModel()?.ref, null, "ask")
                    }) { Text(tr("Настроить профиль", "Configure profile")) }
                }
            }
            items(brain.prefs.profiles, key = { it.directory }) { profile ->
                HorizontalDivider()
                Text(brain.folderLabel(profile.directory), style = MaterialTheme.typography.titleMedium)
                Text(profile.model?.let { brain.model(it)?.name ?: it } ?: tr("Модель по умолчанию", "Default model"))
                Row {
                    TextButton({ editor = profile }) { Text(tr("Изменить", "Edit")) }
                    TextButton({
                        if (profile.directory == brain.engine.workspace.absolutePath || brain.filesGranted) {
                            brain.update { it.copy(folder = profile.directory) }
                            if (brain.folder().absolutePath == profile.directory) {
                                brain.openSession(null)
                                back()
                            }
                        } else folders = true
                    }) { Text(tr("Новый чат здесь", "New chat here")) }
                    TextButton({ brain.removeProfile(profile.directory) }) { Text(tr("Удалить", "Remove")) }
                }
            }
        }
    }
    if (folders) FolderDialog(brain) { folders = false }
    editor?.let { initial ->
        var profile by remember(initial) { mutableStateOf(initial) }
        AlertDialog(
            onDismissRequest = { editor = null },
            title = { Text(brain.folderLabel(profile.directory)) },
            text = {
                Column {
                    Choice(tr("Модель", "Model"), brain.models.map { it.ref to it.name }, profile.model) { profile = profile.copy(model = it) }
                    Choice(tr("Режим", "Mode"), listOf("build" to tr("Агент", "Agent"), "chat" to tr("Чат", "Chat")), profile.agent) {
                        profile = profile.copy(agent = it)
                    }
                    val rules = listOf("ask" to tr("Спрашивать", "Ask"), "deny" to tr("Запрещать", "Deny"))
                    Choice(tr("Команды", "Commands"), rules, profile.shell) { profile = profile.copy(shell = it) }
                    Choice(tr("Чтение файлов", "Read files"), rules, profile.read) { profile = profile.copy(read = it) }
                    Choice(tr("Изменение файлов", "Edit files"), rules, profile.edit) { profile = profile.copy(edit = it) }
                }
            },
            confirmButton = { TextButton({ brain.saveProfile(profile); editor = null }) { Text(tr("Сохранить", "Save")) } },
            dismissButton = { TextButton({ editor = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}

@Composable
private fun Choice(label: String, choices: List<Pair<String, String>>, selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val default = tr("По умолчанию", "Default")
    TextButton({ expanded = true }) {
        Text("$label · ${choices.firstOrNull { it.first == selected }?.second ?: selected ?: default}")
    }
    DropdownMenu(expanded, { expanded = false }) {
        DropdownMenuItem(text = { Text(default) }, onClick = { onSelect(null); expanded = false })
        for ((id, name) in choices) DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
    }
}

@Composable
fun GitScreen(brain: Brain, back: () -> Unit) {
    val directory = brain.session()?.directory?.let(::File) ?: brain.folder()
    val git = remember(directory) { ProjectGit(directory, brain.linux) }
    val selected = remember(directory) { mutableStateListOf<String>() }
    var status by remember { mutableStateOf<ProjectGit.Status?>(null) }
    var commit by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    var branch by remember { mutableStateOf<String?>(null) }
    var suggest by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf<String?>(null) }
    var destination by remember(git) { mutableStateOf<ProjectGit.GitHubDestination?>(null) }
    var send by remember(git) { mutableStateOf<ProjectGit.GitHubDestination?>(null) }
    var pushed by remember(git) { mutableStateOf<String?>(null) }
    var diff by remember(git) { mutableStateOf<String?>(null) }
    var checkpointFiles by remember(git) { mutableStateOf<List<String>?>(null) }
    var restore by remember(git) { mutableStateOf<List<String>?>(null) }
    var showClone by remember { mutableStateOf(false) }
    var cloneUrl by rememberSaveable { mutableStateOf("") }
    var showCloneFolder by remember { mutableStateOf(false) }
    var prTarget by remember(git) { mutableStateOf<ProjectGit.GitHubDestination?>(null) }
    var prBase by remember { mutableStateOf("") }
    var prTitle by remember { mutableStateOf("") }
    var prBody by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val agentRunning = brain.sessions.any { it.directory == directory.absolutePath && brain.isRunning(it.id) }
    val scope = rememberCoroutineScope()

    fun work(action: suspend () -> Unit = {}) {
        if (pending) return
        pending = true
        problem = null
        pushed = null
        scope.launch {
            try {
                login = brain.github.login()
                action()
                status = git.status()
                destination = git.githubDestination()
                selected.retainAll(status?.changes.orEmpty().map { it.path }.toSet())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problem = e.message ?: e.toString()
            } finally {
                pending = false
            }
        }
    }
    LaunchedEffect(git) { work() }

    Page("Git", back) { mod ->
        LazyColumn(mod.navigationBarsPadding().imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Text(directory.absolutePath, style = MaterialTheme.typography.labelMedium)
                TextButton({ showClone = true }, enabled = !pending) { Text(tr("Клонировать из GitHub", "Clone from GitHub")) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton({ work() }, enabled = !pending) { Text(tr("Обновить", "Refresh")) }
                    TextButton({ brain.screens.add("github") }, enabled = !pending) {
                        Text(login?.let { "@$it" } ?: tr("Войти в GitHub", "Sign in to GitHub"))
                    }
                }
                destination?.let { target ->
                    Column(Modifier.fillMaxWidth().graphitePanel().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(target.repository, style = MaterialTheme.typography.titleMedium)
                        Text(target.branch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button({ send = target }, enabled = login != null && status?.history?.isNotBlank() == true && !pending && !agentRunning) {
                            Text(tr("Отправить ветку", "Push branch"))
                        }
                        TextButton({
                            work {
                                prBase = git.defaultBase()
                                val summary = git.pullRequestSummary(prBase)
                                prTitle = summary.lineSequence().firstOrNull().orEmpty()
                                prBody = summary.lineSequence().filter(String::isNotBlank).joinToString("\n") { "- $it" }.take(1800)
                                prTarget = target
                            }
                        }, enabled = !pending && target.branch.isNotEmpty()) { Text(tr("Черновик pull request", "Pull request draft")) }
                    }
                }
                if (status != null && destination == null) Text(
                    tr("Для отправки выберите локальную ветку и настройте один GitHub-адрес origin.",
                        "Select a local branch and configure a single GitHub origin to push."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (pending) CircularProgressIndicator()
                pushed?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                status?.let { value ->
                    Choice(tr("Ветка", "Branch"), value.branches.map { it to it }, value.branch) {
                        if (it != null && it != value.branch && !pending && !agentRunning) branch = it
                    }
                    TextButton({ work { git.checkpoint(); checkpointFiles = emptyList() } },
                        enabled = !pending && !agentRunning && checkpointFiles == null) {
                        Text(tr("Создать контрольную точку", "Create checkpoint"))
                    }
                    TextButton({ work {
                        checkpointFiles = git.checkpointFiles()
                        if (checkpointFiles == null) error(tr("Сначала создайте контрольную точку", "Create a checkpoint first"))
                    } }, enabled = !pending && !agentRunning) { Text(tr("Сравнить с контрольной точкой", "Compare with checkpoint")) }
                    Section(tr("Изменения", "Changes"))
                    if (value.changes.isEmpty()) Text(tr("Рабочая копия чистая", "Working tree is clean"))
                }
            }
            items(status?.changes.orEmpty(), key = { it.path }) { change ->
                Row {
                    Checkbox(change.path in selected, { checked ->
                        if (checked) selected.add(change.path) else selected.remove(change.path)
                    }, enabled = !pending)
                    Text("${change.status}  ${change.path}", Modifier.padding(top = 12.dp))
                }
            }
            if (status != null) item {
                TextButton({
                    val paths = selected.toList()
                    work { diff = git.diff(paths).ifBlank { tr("Для выбранных файлов нет текстового diff", "No text diff for selected files") } }
                }, enabled = selected.isNotEmpty() && !pending) { Text(tr("Посмотреть diff выбранных файлов", "Review selected diff")) }
                OutlinedTextField(commit, { commit = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Сообщение коммита", "Commit message")) })
                TextButton({ suggest = true }, enabled = selected.isNotEmpty() && !pending && brain.currentModel() != null) {
                    Text(tr("Попросить модель предложить сообщение", "Ask the model for a commit message"))
                }
                Button({
                    val paths = selected.toList()
                    work { git.commit(commit, paths); commit = ""; selected.clear() }
                }, enabled = selected.isNotEmpty() && commit.isNotBlank() && !pending && !agentRunning) {
                    Text(tr("Создать коммит", "Commit"))
                }
                Section(tr("История", "History"))
                SelectionContainer { Text(status?.history.orEmpty(), fontFamily = FontFamily.Monospace) }
            }
        }
    }
    diff?.let { patch ->
        AlertDialog(onDismissRequest = { diff = null }, title = { Text(tr("Изменения в выбранных файлах", "Selected changes")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer { Text(patch, fontFamily = FontFamily.Monospace) }
            } }, confirmButton = { TextButton({ diff = null }) { Text(tr("Закрыть", "Close")) } })
    }
    checkpointFiles?.let { paths ->
        if (restore == null) AlertDialog(onDismissRequest = { checkpointFiles = null },
            title = { Text(tr("Контрольная точка", "Checkpoint")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (paths.isEmpty()) Text(tr("Рабочие файлы совпадают с точкой", "Working files match the checkpoint"))
                paths.forEach { path ->
                    TextButton({ restore = listOf(path) }, enabled = !agentRunning && !pending) { Text(path) }
                }
            } },
            confirmButton = { TextButton({ restore = paths }, enabled = paths.isNotEmpty() && !agentRunning && !pending) {
                Text(tr("Восстановить всё", "Restore all"))
            } },
            dismissButton = { TextButton({ checkpointFiles = null }) { Text(tr("Закрыть", "Close")) } })
    }
    restore?.let { paths ->
        AlertDialog(onDismissRequest = { restore = null }, title = { Text(tr("Восстановить файлы?", "Restore files?")) },
            text = { Text(tr("Изменения после контрольной точки в этих файлах будут отменены.",
                "Changes since the checkpoint in these files will be reverted.")) },
            confirmButton = { TextButton({
                restore = null
                checkpointFiles = null
                work { git.restoreCheckpoint(paths); checkpointFiles = git.checkpointFiles() }
            }, enabled = !agentRunning && !pending) { Text(tr("Восстановить", "Restore")) } },
            dismissButton = { TextButton({ restore = null }) { Text(tr("Отмена", "Cancel")) } })
    }
    if (showCloneFolder) FolderDialog(brain) { showCloneFolder = false }
    if (showClone && !showCloneFolder) AlertDialog(onDismissRequest = { showClone = false },
        title = { Text(tr("Клонировать репозиторий", "Clone repository")) },
        text = { Column {
            OutlinedTextField(cloneUrl, { cloneUrl = it }, label = { Text("GitHub URL") }, singleLine = true)
            TextButton({ showCloneFolder = true }) { Text(brain.folder().absolutePath) }
            Text(tr("Клон будет создан в новой подпапке. Поддерживаются публичные репозитории.",
                "A new subfolder will be created. Public repositories are supported."))
        } },
        confirmButton = { TextButton({
            showClone = false
            val parent = brain.folder()
            work {
                val project = ProjectGit(parent, brain.linux).cloneGitHub(cloneUrl)
                brain.update { it.copy(folder = project.absolutePath) }
                brain.openSession(null)
                brain.screens.clear()
            }
        }, enabled = cloneUrl.isNotBlank() && !pending) { Text(tr("Клонировать", "Clone")) } },
        dismissButton = { TextButton({ showClone = false }) { Text(tr("Отмена", "Cancel")) } })
    prTarget?.let { target ->
        AlertDialog(onDismissRequest = { prTarget = null }, title = { Text(tr("Черновик pull request", "Pull request draft")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${target.repository} · ${target.branch}")
                OutlinedTextField(prBase, { prBase = it }, label = { Text(tr("Целевая ветка", "Base branch")) }, singleLine = true)
                OutlinedTextField(prTitle, { prTitle = it }, label = { Text(tr("Заголовок", "Title")) }, singleLine = true)
                OutlinedTextField(prBody, { prBody = it }, label = { Text(tr("Описание", "Description")) }, maxLines = 8)
            } },
            confirmButton = { TextButton({
                val compare = Uri.parse("https://github.com/${target.repository}/compare/${Uri.encode(prBase.trim())}...${Uri.encode(target.branch)}")
                    .buildUpon().appendQueryParameter("expand", "1")
                    .appendQueryParameter("title", prTitle.take(150)).appendQueryParameter("body", prBody.take(1800)).build()
                runCatching { uriHandler.openUri(compare.toString()) }
                    .onSuccess { prTarget = null }.onFailure { prTarget = null; problem = it.message }
            }, enabled = prTitle.isNotBlank() && prBase.isNotBlank() && prBase.trim() != target.branch) {
                Text(tr("Открыть на GitHub", "Open in GitHub"))
            } },
            dismissButton = { TextButton({ prTarget = null }) { Text(tr("Отмена", "Cancel")) } })
    }
    send?.let { target ->
        AlertDialog(
            onDismissRequest = { send = null },
            title = { Text(tr("Отправить ветку на GitHub?", "Push branch to GitHub?")) },
            text = { Text("${target.repository} · ${target.branch}") },
            confirmButton = { TextButton({
                send = null
                work {
                    git.pushGitHub(target, brain.github.token())
                    pushed = tr("Ветка ${target.branch} отправлена", "Branch ${target.branch} pushed")
                }
            }, enabled = !agentRunning && !pending) { Text(tr("Отправить", "Push")) } },
            dismissButton = { TextButton({ send = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
    if (suggest) AlertDialog(
        onDismissRequest = { suggest = false },
        title = { Text(tr("Предложить сообщение коммита", "Suggest a commit message")) },
        text = { Text(tr("Имена выбранных файлов и часть diff будут отправлены выбранной модели.",
            "Selected filenames and part of the diff will be sent to the selected model.")) },
        confirmButton = { TextButton({
            suggest = false
            val paths = selected.toList()
            work { commit = brain.suggestCommit(directory, paths, git.diff(paths)) }
        }) { Text(tr("Сгенерировать", "Generate")) } },
        dismissButton = { TextButton({ suggest = false }) { Text(tr("Отмена", "Cancel")) } },
    )
    branch?.let { name ->
        AlertDialog(
            onDismissRequest = { branch = null },
            title = { Text(tr("Переключиться на $name?", "Switch to $name?")) },
            confirmButton = { TextButton({ branch = null; work { git.switchBranch(name) } }, enabled = !agentRunning) { Text(tr("Переключить", "Switch")) } },
            dismissButton = { TextButton({ branch = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}

@Composable
fun ChangesScreen(brain: Brain, back: () -> Unit) {
    val id = brain.open ?: return
    val directory = brain.session(id)?.directory
    var files by remember(id) { mutableStateOf<List<JSONObject>?>(null) }
    var problem by remember(id) { mutableStateOf<String?>(null) }
    var version by remember(id) { mutableStateOf(0) }
    var selected by remember(id) { mutableStateOf<JSONObject?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var reverting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(id, version, brain.isRunning(id)) {
        problem = null
        try {
            files = brain.changes(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problem = e.message ?: e.toString()
        }
    }
    Page(tr("Правки агента", "Agent changes"), back) { mod ->
        LazyColumn(mod.navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            item {
                Text(tr("Файлы последнего ответа", "Files changed in the latest turn"))
                TextButton({ version++ }) { Text(tr("Обновить", "Refresh")) }
                if (files == null && problem == null) CircularProgressIndicator()
                if (files?.isEmpty() == true) Text(tr("Правок нет", "No changes"))
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(files.orEmpty()) { file ->
                TextButton({ selected = file }, Modifier.fillMaxWidth()) {
                    Text("${file.optString("file")}  +${file.optInt("additions")}  −${file.optInt("deletions")}")
                }
                HorizontalDivider()
            }
        }
    }

    selected?.let { file ->
        val patch = file.optString("patch")
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(file.optString("file")) },
            text = {
                SelectionContainer {
                    Text(patch, modifier = Modifier.verticalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton({ clipboard.setText(AnnotatedString(patch)) }, enabled = patch.isNotBlank()) {
                    Text(tr("Копировать патч", "Copy patch"))
                }
            },
            dismissButton = {
                TextButton({ confirm = true },
                    enabled = directory != null && brain.linux.installed && !reverting && !brain.isRunning(id) &&
                        patch.startsWith("diff --git ") && patch.contains("@@")) {
                    Text(tr("Откатить файл", "Revert file"))
                }
            },
        )
        if (confirm) AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Откатить этот файл?", "Revert this file?")) },
            text = { Text(tr("Патч применится, если файл по-прежнему совпадает с версией после правок агента.",
                "The patch applies if the file still matches the agent's edited version.")) },
            confirmButton = {
                TextButton({
                    confirm = false
                    if (directory == null || brain.isRunning(id)) return@TextButton
                    reverting = true
                    scope.launch {
                        try {
                            val current = brain.changes(id).firstOrNull { it.str("file") == file.str("file") }
                            check(current?.str("patch") == patch && !brain.isRunning(id)) {
                                tr("Правки изменились. Обновите список.", "Changes have moved on. Refresh the list.")
                            }
                            ProjectGit(File(directory), brain.linux).reverse(file.getString("file"), patch)
                            version++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            problem = e.message ?: e.toString()
                        } finally {
                            selected = null
                            reverting = false
                        }
                    }
                }) { Text(tr("Откатить", "Revert")) }
            },
            dismissButton = { TextButton({ confirm = false }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}

private fun isLoopback(uri: Uri): Boolean =
    uri.scheme in setOf("http", "https") && uri.host in setOf("localhost", "127.0.0.1", "[::1]", "::1") &&
        uri.userInfo == null && uri.port in 1..65535

private fun samePreviewOrigin(origin: Uri?, request: Uri): Boolean {
    if (origin == null) return false
    return isLoopback(request) && request.scheme == origin.scheme && request.host == origin.host && request.port == origin.port
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(brain: Brain, back: () -> Unit) {
    var address by rememberSaveable { mutableStateOf(brain.previewUrl ?: "http://127.0.0.1:3000") }
    val previewOrigin = remember { AtomicReference<Uri?>() }
    val console = remember { mutableStateListOf<String>() }
    var showConsole by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }
    Page(tr("Веб-предпросмотр", "Web preview"), back) { mod ->
        Column(mod.navigationBarsPadding().imePadding().fillMaxSize()) {
            OutlinedTextField(address, { address = it; error = false },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
                label = { Text(tr("Адрес локального сервера", "Local server URL")) }, isError = error)
            TextButton({
                val target = address.trim()
                error = !isLoopback(Uri.parse(target))
                if (!error) {
                    brain.previewUrl = target
                    refresh++
                }
            }, Modifier.padding(horizontal = 12.dp)) { Text(tr("Открыть / обновить", "Open / refresh")) }
            TextButton({ showConsole = true }, enabled = console.isNotEmpty(), modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(tr("Консоль (${console.size})", "Console (${console.size})"))
            }
            brain.previewUrl?.let { url ->
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(Color.rgb(16, 16, 16))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                    if (console.size >= 100) console.removeAt(0)
                                    console.add("${message.messageLevel()}:${message.lineNumber()} ${message.message().take(800)}")
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                                    !samePreviewOrigin(previewOrigin.get(), request.url)

                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                                    if (samePreviewOrigin(previewOrigin.get(), request.url)) null
                                    else WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { it.destroy() },
                    update = { web ->
                        val key = url to refresh
                        if (web.tag != key) {
                            web.tag = key
                            console.clear()
                            previewOrigin.set(Uri.parse(url))
                            web.loadUrl(url)
                        }
                    },
                )
            }
        }
    }
    if (showConsole) AlertDialog(onDismissRequest = { showConsole = false },
        title = { Text(tr("Консоль сайта", "Site console")) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            SelectionContainer { Text(console.joinToString("\n"), fontFamily = FontFamily.Monospace) }
        } },
        confirmButton = { TextButton({
            brain.shared = (tr("Консоль веб-предпросмотра", "Web preview console") + "\n" +
                console.takeLast(25).joinToString("\n").takeLast(6000)) to emptyList()
            brain.screens.clear()
        }, enabled = console.isNotEmpty()) { Text(tr("Добавить в черновик чата", "Add to chat draft")) } },
        dismissButton = { TextButton({ showConsole = false }) { Text(tr("Закрыть", "Close")) } })

}
