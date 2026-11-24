package app.sbandcode.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.engine.Local
import app.sbandcode.android.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun gb(bytes: Long) = "%.1f".format(bytes / 1e9).replace('.', if (app.sbandcode.android.Lang.ru) ',' else '.') +
    tr(" ГБ", " GB")

/** Models that run on the phone: download from the list or by a link, and how much they remember. */
@Composable
fun LocalScreen(brain: Brain, back: () -> Unit) {
    val local = brain.local
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    val installed = remember(round) { local.installed() }
    val transfers = remember(round) { local.transfers() }
    val free = remember(round) { local.freeBytes() }
    var download by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var estimate by remember { mutableStateOf<Local.DownloadSize?>(null) }
    var estimateError by remember { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<String?>(null) }
    val progress = remember { mutableStateMapOf<String, Float>() }
    val jobs = remember { mutableMapOf<String, Job>() }
    var problem by remember { mutableStateOf<String?>(null) }
    var pictures by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    val ram = remember { local.ramGb() }

    androidx.compose.runtime.LaunchedEffect(download) {
        val target = download ?: return@LaunchedEffect
        estimate = null
        estimateError = null
        try {
            estimate = local.downloadSize(target.first, target.second)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            estimateError = e.message ?: e.toString()
        }
    }

    fun get(id: String, urls: List<String>) {
        problem = null
        progress[id] = 0f
        jobs[id] = scope.launch {
            runCatching {
                local.download(id, urls) { done, total ->
                    if (total > 0) scope.launch(Dispatchers.Main) { progress[id] = done.toFloat() / total }
                }
            }.onSuccess { brain.restart() }.onFailure { if (it !is kotlinx.coroutines.CancellationException) problem = it.message ?: it.toString() }
            progress.remove(id)
            jobs.remove(id)
            round++
        }
    }

    Page(tr("Модели на телефоне", "Models on the phone"), back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Работают без интернета и бесплатно, прямо на процессоре телефона. Они заметно медленнее и слабее облачных. Памяти в телефоне: ${"%.0f".format(ram)} ГБ, модель должна быть заметно меньше.",
                "They work offline and for free, right on the phone's processor. They are much slower and weaker than cloud ones. This phone has ${"%.0f".format(ram)} GB of memory; a model should be well under that."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("Свободно: ${gb(free)}", "Free space: ${gb(free)}"))
            brain.localError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Row(Modifier.fillMaxWidth().clickable { pictures = !pictures }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(pictures, { pictures = it })
                Text(tr("Скачивать и чтение картинок (ещё около 1 ГБ)", "Also download picture reading (about 1 GB more)"))
            }

            for (o in Local.OFFERS) {
                val have = o.id in installed
                val p = progress[o.id]
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(o.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(o.note + " · " + (if (have) gb(local.size(o.id)) else "${o.gb}" + tr(" ГБ", " GB")) +
                                    (if (have && local.sees(o.id)) tr(" · видит картинки", " · sees pictures") else ""),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            when {
                                p != null -> TextButton({ jobs[o.id]?.cancel() }) { Text(tr("Пауза", "Pause")) }
                                transfers[o.id] != null -> {
                                    TextButton({ download = o.id to transfers.getValue(o.id) }) {
                                        Text(tr("Продолжить", "Resume"))
                                    }
                                    IconButton({ remove = o.id }) { Icon(Icons.Outlined.Delete, tr("Удалить", "Delete")) }
                                }
                                have -> {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    IconButton({ remove = o.id }) { Icon(Icons.Outlined.Delete, tr("Удалить", "Delete")) }
                                }
                                else -> IconButton({
                                    download = o.id to (listOf(Local.url(o.repo, o.file)) + if (pictures && o.vision) listOf(Local.url(o.repo, o.mmproj)) else emptyList())
                                }) { Icon(Icons.Outlined.Download, tr("Скачать", "Download")) }
                            }
                        }
                        if (p != null) {
                            LinearProgressIndicator({ p }, Modifier.fillMaxWidth())
                            Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        if (!have && p == null && o.gb > ram * 0.6) Text(tr("Скорее всего, не поместится в память этого телефона", "Probably won't fit in this phone's memory"),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            val own = installed.filter { id -> id !in transfers && Local.OFFERS.none { it.id == id } }
            if (own.isNotEmpty()) {
                Section(tr("Свои", "Your own"))
                for (id in own) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(id, Modifier.weight(1f))
                    Text(gb(local.size(id)), style = MaterialTheme.typography.bodySmall)
                    IconButton({ remove = id }) { Icon(Icons.Outlined.Delete, tr("Удалить", "Delete")) }
                }
            }
            Section(tr("Своя модель", "A model of your own"))
            Text(tr("Ссылка на файл .gguf на Hugging Face (кнопка «download» у файла).", "A link to a .gguf file on Hugging Face (the file's download button)."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(link, { link = it.trim() }, singleLine = true, placeholder = { Text("https://huggingface.co/…/model.gguf") },
                modifier = Modifier.fillMaxWidth())
            val linkId = link.substringAfterLast('/').substringBefore('?').removeSuffix(".gguf").lowercase().replace(Regex("[^a-z0-9._-]+"), "-")
            OutlinedButton({
                val fixed = link.replace("/blob/", "/resolve/")
                download = linkId to listOf(fixed)
            }, enabled = link.startsWith("https://huggingface.co/") && link.contains(".gguf") && linkId.any(Char::isLetterOrDigit) && progress[linkId] == null && linkId !in installed && linkId !in transfers) {
                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                Text(tr("Скачать", "Download"), Modifier.padding(start = 8.dp))
            }
            val customTransfers = (transfers.keys + progress.keys).filter { key -> Local.OFFERS.none { it.id == key } }
            for (key in customTransfers) Row(verticalAlignment = Alignment.CenterVertically) {
                val value = progress[key]
                Text(if (value == null) key else "$key · ${(value * 100).toInt()}%", Modifier.weight(1f))
                if (value != null) TextButton({ jobs[key]?.cancel() }) { Text(tr("Пауза", "Pause")) }
                else {
                    TextButton({ download = key to transfers.getValue(key) }) { Text(tr("Продолжить", "Resume")) }
                    IconButton({ remove = key }) { Icon(Icons.Outlined.Delete, tr("Удалить", "Delete")) }
                }
            }

            Section(tr("Память разговора", "Conversation memory"))
            Text(tr("Сколько текста модель держит в голове. Больше помнит, но ест больше памяти и медленнее начинает.",
                "How much text the model keeps in mind. More remembers more but eats more memory and starts slower."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in listOf(8192, 16384, 32768)) FilterChip(brain.prefs.localContext == c, {
                    brain.update { it.copy(localContext = c) }
                    brain.restart()
                }, label = { Text("${c / 1024}k") })
            }
            problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(24.dp))
        }
    }
    download?.let { target ->
        val size = estimate
        val available = local.freeBytes()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { download = null },
            title = { Text(tr("Загрузить модель", "Download model")) },
            text = {
                when {
                    estimateError != null -> Text(estimateError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    size == null -> androidx.compose.material3.CircularProgressIndicator()
                    else -> {
                        val total = gb(size.total)
                        val remaining = gb(size.remaining)
                        val freeSize = gb(available)
                        Text(tr(
                            "Размер: $total · Осталось: $remaining · Свободно: $freeSize",
                            "Size: $total · Remaining: $remaining · Free: $freeSize",
                        ))
                    }
                }
            },
            confirmButton = {
                TextButton({
                    download = null
                    get(target.first, target.second)
                }, enabled = size != null && size.remaining <= available) { Text(tr("Загрузить", "Download")) }
            },
            dismissButton = { TextButton({ download = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
    remove?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text(tr("Удалить $id?", "Delete $id?")) },
            confirmButton = { TextButton({
                remove = null
                scope.launch {
                    try {
                        local.remove(id)
                        round++
                        brain.restart()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        problem = e.message ?: e.toString()
                    }
                }
            }) { Text(tr("Удалить", "Delete")) } },
            dismissButton = { TextButton({ remove = null }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}
