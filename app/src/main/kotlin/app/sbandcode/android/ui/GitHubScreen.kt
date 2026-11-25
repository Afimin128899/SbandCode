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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.sbandcode.android.Brain
import app.sbandcode.android.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun GitHubScreen(brain: Brain, back: () -> Unit) {
    var login by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var token by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    val browser = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(brain.github) {
        login = brain.github.login()
        loading = false
    }

    fun update(action: suspend () -> Unit) {
        if (loading) return
        loading = true
        problem = null
        scope.launch {
            try {
                action()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                problem = failure.message ?: tr("Не удалось подключиться к GitHub", "Could not connect to GitHub")
            } finally {
                loading = false
            }
        }
    }

    Page("GitHub", back) { modifier ->
        Column(modifier.navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Column(Modifier.fillMaxWidth().graphitePanel().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val account = login
                if (account == null) {
                    Text(tr("Подключить аккаунт", "Connect account"), style = MaterialTheme.typography.titleLarge)
                    Text(tr("Создайте токен для нужных репозиториев. В разрешениях GitHub установите Contents → Read and write.",
                        "Create a token for the repositories you need. Set Contents → Read and write in GitHub permissions."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton({
                        runCatching { browser.openUri("https://github.com/settings/personal-access-tokens/new?name=SbandCode&contents=write") }
                            .onFailure { problem = tr("Не удалось открыть браузер", "Could not open the browser") }
                    }) { Text(tr("Создать токен на GitHub", "Create token on GitHub")) }
                    OutlinedTextField(token, { token = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        enabled = !loading, label = { Text(tr("Токен GitHub", "GitHub token")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation())
                    Button({ update { login = brain.github.signIn(token); token = "" } }, enabled = !loading && token.isNotBlank()) {
                        Text(if (loading) tr("Подключение…", "Connecting…") else tr("Войти по токену", "Sign in with token"))
                    }
                } else {
                    Text("@$account", style = MaterialTheme.typography.titleLarge)
                    Text(tr("Аккаунт подключён. Отправка ветки доступна в Инструменты → Git.",
                        "Account connected. Push a branch from Tools → Git."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton({ update { brain.github.signOut(); login = null } }, enabled = !loading) {
                        Text(tr("Выйти", "Sign out"))
                    }
                }
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
