package app.sbandcode.android.data

import app.sbandcode.android.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

data class CustomApi(val name: String = "", val address: String = "", val key: String = "", val models: List<String> = emptyList()) {
    fun endpoint(): String {
        val uri = runCatching { URI(address.trim()) }.getOrNull()
        require(uri != null && uri.scheme in setOf("http", "https") && uri.host != null &&
            uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            tr("Укажите базовый HTTP(S)-адрес API, например https://example.com/v1.",
                "Enter an HTTP(S) API base URL, for example https://example.com/v1.")
        }
        return uri.toASCIIString().trimEnd('/')
    }

    fun config(previous: JSONObject?): JSONObject {
        require(models.isNotEmpty() && models.all { it.isNotBlank() && '#' !in it && it.none(Char::isWhitespace) }) {
            tr("Укажите ID моделей, по одному на строку.", "Enter model IDs, one per line.")
        }
        val provider = previous ?: JSONObject()
        val settings = provider.optJSONObject("settings") ?: JSONObject()
        settings.put("baseURL", endpoint())
        settings.put("apiKey", key.trim().ifBlank { "local" })
        val before = provider.optJSONObject("models")
        val catalog = JSONObject()
        for (id in models.distinct()) {
            catalog.put(id, before?.optJSONObject(id) ?: jsonOf(
                "name" to id,
                "limit" to jsonOf("context" to 32768, "output" to 4096),
                "capabilities" to jsonOf("tools" to false, "input" to JSONArray().put("text"), "output" to JSONArray().put("text")),
            ))
        }
        return provider.put("name", name.trim().ifBlank { tr("Свой API", "Custom API") })
            .put("package", "@opencode/ai/providers/openai-compatible")
            .put("settings", settings).put("models", catalog)
    }

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val connection = URI(endpoint() + "/models").toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8_000
        connection.readTimeout = 15_000
        if (key.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer ${key.trim()}")
        try {
            val code = connection.responseCode
            check(code in 200..299) { tr("Список моделей недоступен (HTTP $code).", "Model list unavailable (HTTP $code).") }
            val data = connection.inputStream.bufferedReader().use {
                val buffer = CharArray(4_096)
                val text = StringBuilder()
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    check(text.length + count <= 2_000_000) { tr("Список моделей слишком большой.", "The model list is too large.") }
                    text.append(buffer, 0, count)
                }
                JSONObject(text.toString()).optJSONArray("data")
            }
            data.objects().mapNotNull { it.str("id") }.filter { '#' !in it && it.none(Char::isWhitespace) }.distinct().sorted()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ID = "sbandcode-custom"

        fun from(provider: JSONObject?): CustomApi {
            val settings = provider?.optJSONObject("settings")
            return CustomApi(provider?.str("name").orEmpty(), settings?.str("baseURL").orEmpty(),
                settings?.str("apiKey")?.takeUnless { it == "local" }.orEmpty(),
                provider?.optJSONObject("models")?.keys()?.asSequence()?.toList().orEmpty())
        }
    }
}
