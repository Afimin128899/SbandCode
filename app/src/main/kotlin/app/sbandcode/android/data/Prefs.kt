package app.sbandcode.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** The app's own choices. Keys, chats and providers live in opencode. */
data class Prefs(
    val model: String? = null,
    val favourites: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    /** Chosen variant (thinking level) per model ref; missing means the model's default. */
    val variants: Map<String, String> = emptyMap(),
    /** Where new chats work; empty means the app's own workspace. */
    val folder: String = "",
    /** Run the agent's commands in Alpine once it is installed. */
    val linux: Boolean = true,
    /** Keep chats on the Margy server too, for the same account on other phones. */
    val sync: Boolean = false,
    /** Context for models on the phone: more remembers more but needs more memory. */
    val localContext: Int = 16384,
    /** Models on the phone get opencode's tools too; off, they just talk, with a prompt ten times shorter. */
    val localTools: Boolean = false,
    val profiles: List<ProjectProfile> = emptyList(),
) {
    fun json() = jsonOf(
        "model" to model,
        "favourites" to JSONArray(favourites),
        "recent" to JSONArray(recent),
        "variants" to JSONObject(variants),
        "folder" to folder,
        "linux" to linux,
        "sync" to sync,
        "localContext" to localContext,
        "localTools" to localTools,
        "profiles" to JSONArray(profiles.map { it.json() }),
    )

    companion object {
        private fun file(context: Context) = File(context.filesDir, "prefs.json")

        fun load(context: Context): Prefs {
            val o = runCatching { JSONObject(file(context).readText()) }.getOrNull() ?: return Prefs()
            return Prefs(
                model = o.str("model"),
                favourites = o.optJSONArray("favourites").strings(),
                recent = o.optJSONArray("recent").strings(),
                variants = o.optJSONObject("variants")?.let { m -> m.keys().asSequence().associateWith { m.optString(it) } } ?: emptyMap(),
                folder = o.str("folder") ?: "",
                linux = o.optBoolean("linux", true),
                sync = o.optBoolean("sync"),
                localContext = o.optInt("localContext", 16384),
                localTools = o.optBoolean("localTools"),
                profiles = o.optJSONArray("profiles").objects().mapNotNull(ProjectProfile::from),
            )
        }

        fun save(context: Context, prefs: Prefs) {
            val tmp = File(context.filesDir, "prefs.json.tmp")
            tmp.writeText(prefs.json().toString(1))
            tmp.renameTo(file(context))
        }
    }
}

data class ProjectProfile(
    val directory: String,
    val model: String?,
    val agent: String?,
    val shell: String?,
    val read: String? = null,
    val edit: String? = null,
) {
    fun json() = jsonOf("directory" to directory, "model" to model, "agent" to agent,
        "shell" to shell, "read" to read, "edit" to edit)

    fun permissions(): JSONArray? {
        val rules = listOf("shell" to shell, "read" to read, "edit" to edit).mapNotNull { (action, effect) ->
            effect?.let { jsonOf("action" to action, "resource" to "*", "effect" to it) }
        }
        return rules.takeIf { it.isNotEmpty() }?.let(::JSONArray)
    }

    companion object {
        fun from(o: JSONObject): ProjectProfile? = o.str("directory")?.let {
            ProjectProfile(it, o.str("model"), o.str("agent"), o.str("shell"), o.str("read"), o.str("edit"))
        }
    }
}
