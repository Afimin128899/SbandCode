package app.sbandcode.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.data.CustomApi
import app.sbandcode.android.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CustomApiScreen(brain: Brain, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(CustomApi()) }
    var ids by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            profile = brain.customApi()
            ids = profile.models.joinToString("\n")
            ready = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            message = tr("Не удалось прочитать opencode.json. Проверьте файл в настройках.",
                "Could not read opencode.json. Check the file in Settings.")
        }
    }

    fun perform(action: suspend () -> Unit) {
        working = true
        message = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message.orEmpty().ifBlank { tr("Не удалось выполнить запрос.", "The request failed.") }
            } finally {
                working = false
            }
        }
    }

    Page(tr("Свой API и модели", "Custom API and models"), back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Подключение к OpenAI-совместимому API.", "Connect an OpenAI-compatible API."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val edit = ready && !working
            OutlinedTextField(profile.name, { profile = profile.copy(name = it) }, Modifier.fillMaxWidth(),
                label = { Text(tr("Имя провайдера", "Provider name")) }, singleLine = true, enabled = edit)
            OutlinedTextField(profile.address, { profile = profile.copy(address = it) }, Modifier.fillMaxWidth(),
                label = { Text(tr("Базовый URL", "Base URL")) }, placeholder = { Text("https://example.com/v1") },
                singleLine = true, enabled = edit, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            OutlinedTextField(profile.key, { profile = profile.copy(key = it) }, Modifier.fillMaxWidth(),
                label = { Text(tr("Ключ API (если требуется)", "API key (if required)")) }, singleLine = true, enabled = edit,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            OutlinedTextField(ids, { ids = it }, Modifier.fillMaxWidth(),
                label = { Text(tr("ID моделей, по одному на строку", "Model IDs, one per line")) }, minLines = 3, enabled = edit)
            OutlinedButton({
                perform {
                    val found = profile.listModels()
                    if (found.isEmpty()) message = tr("Сервер не вернул модели. Введите ID вручную.", "The server returned no models. Enter IDs manually.")
                    else ids = (ids.lineSequence().map(String::trim).filter(String::isNotEmpty).toList() + found)
                        .distinct().joinToString("\n")
                }
            }, enabled = edit && profile.address.isNotBlank()) {
                Text(tr("Загрузить модели с сервера", "Load models from server"))
            }
            Button({
                perform {
                    brain.saveCustomApi(profile.copy(models = ids.lines().map(String::trim).filter(String::isNotEmpty)))
                    message = tr("Сохранено. Модели доступны в выборе модели чата.", "Saved. Models are in the chat model picker.")
                }
            }, enabled = edit && brain.ready && profile.address.isNotBlank() && ids.isNotBlank()) {
                Text(tr("Сохранить", "Save"))
            }
            message?.let { Text(it) }
        }
    }
}
