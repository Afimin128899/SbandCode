package app.sbandcode.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import app.sbandcode.android.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

class GitHubAccount(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "github-account.enc"))

    private class Session(val login: String, val token: String)

    suspend fun login(): String? = withContext(Dispatchers.IO) { read()?.login }

    suspend fun token(): String = withContext(Dispatchers.IO) {
        read()?.token ?: error(tr("Войдите в GitHub", "Sign in to GitHub"))
    }

    suspend fun signIn(input: String): String = withContext(Dispatchers.IO) {
        val token = input.trim()
        require(token.matches(Regex("(?:github_pat_|ghp_)[A-Za-z0-9_]{16,240}"))) {
            tr("Введите личный токен GitHub", "Enter a GitHub personal access token")
        }
        val connection = URL("https://api.github.com/user").openConnection() as HttpsURLConnection
        val login = try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "SbandCode")
            when (val status = connection.responseCode) {
                200 -> connection.inputStream.bufferedReader().use { JSONObject(it.readText()).getString("login") }
                401 -> error(tr("GitHub отклонил токен. Проверьте срок действия и права.", "GitHub rejected the token. Check its expiry and permissions."))
                else -> error(tr("GitHub вернул HTTP $status", "GitHub returned HTTP $status"))
            }
        } finally {
            connection.disconnect()
        }
        coroutineContext.ensureActive()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        val payload = JSONObject().put("login", login).put("token", token).toString().toByteArray()
        val encrypted = cipher.doFinal(payload)
        val stream = file.startWrite()
        try {
            stream.write(cipher.iv)
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (failure: Exception) {
            file.failWrite(stream)
            throw failure
        }
        login
    }

    suspend fun signOut() = withContext(Dispatchers.IO) { file.delete() }

    private fun read(): Session? = runCatching {
        val bytes = file.readFully()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes, 0, 12))
        val data = JSONObject(cipher.doFinal(bytes, 12, bytes.size - 12).toString(Charsets.UTF_8))
        Session(data.getString("login"), data.getString("token"))
    }.getOrNull()

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "GitHub account key is missing" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }

    private companion object {
        const val ALIAS = "sbandcode-github-account"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
