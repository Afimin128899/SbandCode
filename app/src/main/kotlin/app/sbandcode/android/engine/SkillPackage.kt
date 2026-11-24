package app.sbandcode.android.engine

import app.sbandcode.android.data.objects
import app.sbandcode.android.data.str
import app.sbandcode.android.tr
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipInputStream

object SkillPackage {
    private const val MAX_BYTES = 12 * 1024 * 1024
    private val namePattern = Regex("""(?m)^name:[ \t]*["']?([^"'\r\n]+)""")

    fun installFromUrl(directory: File, url: String): String {
        val address = URI(url.trim())
        requireHttps(address)
        val parts = address.rawPath.orEmpty().removePrefix("/").trimEnd('/').split('/')
        if (address.host.equals("github.com", ignoreCase = true)) {
            val owner = parts.getOrNull(0).orEmpty()
            val repo = parts.getOrNull(1).orEmpty().removeSuffix(".git")
            val validName = Regex("[A-Za-z0-9_.-]+")
            if (parts.size == 2 && validName.matches(owner) && validName.matches(repo) &&
                owner !in setOf(".", "..") && repo !in setOf(".", "..")) {
                return installRepository(directory, owner, repo)
            }
            if (parts.size >= 5 && parts[2] == "blob") {
                val raw = URI("https://raw.githubusercontent.com/${parts.take(2).joinToString("/")}/${parts.drop(3).joinToString("/")}")
                val (bytes, name) = download(raw)
                return install(directory, bytes, name, remote = true)
            }
        }
        val (bytes, name) = download(address)
        return install(directory, bytes, name, remote = true)
    }

    private fun installRepository(directory: File, owner: String, repo: String): String {
        val api = "https://api.github.com/repos/$owner/$repo"
        val head = githubJson("$api/commits/HEAD").getString("sha")
        check(head.matches(Regex("[a-f0-9]{40}"))) { "Invalid GitHub commit" }
        val tree = githubJson("$api/git/trees/$head?recursive=1")
        check(!tree.optBoolean("truncated")) {
            tr("Список файлов GitHub неполный. Используйте ссылку на SKILL.md.", "GitHub returned an incomplete file list. Link to SKILL.md.")
        }
        val files = tree.optJSONArray("tree").objects().filter { it.str("type") == "blob" && it.str("mode") in setOf("100644", "100755") }
        val manifests = files.mapNotNull { it.str("path") }.filter { it.substringAfterLast('/') == "SKILL.md" }
        val skill = listOf("SKILL.md", "skills/$repo/SKILL.md", "$repo/SKILL.md", ".agents/skills/$repo/SKILL.md",
            ".claude/skills/$repo/SKILL.md", ".openclaw/skills/$repo/SKILL.md")
            .firstOrNull { candidate -> manifests.any { it.equals(candidate, ignoreCase = true) } }
            ?.let { candidate -> manifests.first { it.equals(candidate, ignoreCase = true) } }
            ?: manifests.singleOrNull()
            ?: error(if (manifests.isEmpty()) tr("В репозитории нет SKILL.md.", "There's no SKILL.md in the repository.")
                else tr("В репозитории несколько скиллов. Укажите ссылку на нужный SKILL.md.", "There are several skills. Link to the chosen SKILL.md."))
        val root = skill.substringBeforeLast('/', "")
        val selected = files.mapNotNull { entry ->
            val path = entry.str("path") ?: return@mapNotNull null
            if (root.isNotEmpty() && !path.startsWith("$root/")) return@mapNotNull null
            path to entry.optLong("size", 0)
        }
        check(selected.size <= 512 && selected.sumOf { it.second } <= MAX_BYTES) {
            tr("Папка скилла слишком большая. Используйте ссылку на SKILL.md или ZIP скилла.",
                "The skill directory is too large. Link to SKILL.md or a skill ZIP.")
        }
        var remaining = MAX_BYTES
        val entries = linkedMapOf<String, ByteArray>()
        for ((path, _) in selected) {
            val relative = if (root.isEmpty()) path else path.removePrefix("$root/")
            validatePath(relative)
            val raw = URI("https", "raw.githubusercontent.com", "/$owner/$repo/$head/$path", null)
            val (bytes, _) = download(raw, remaining)
            remaining -= bytes.size
            entries[relative] = bytes
        }
        return installEntries(directory, entries, "SKILL.md", repo, remote = true)
    }

    private fun githubJson(url: String) = JSONObject(download(URI(url)).first.toString(Charsets.UTF_8))

    private fun requireHttps(address: URI) {
        require(address.scheme.equals("https", ignoreCase = true) && address.host != null && address.userInfo == null) {
            tr("Нужна HTTPS-ссылка на репозиторий GitHub, SKILL.md или ZIP.", "Use an HTTPS link to a GitHub repository, SKILL.md or ZIP.")
        }
    }

    private fun download(source: URI, limit: Int = MAX_BYTES): Pair<ByteArray, String> {
        var address = source
        repeat(6) {
            requireHttps(address)
            val connection = address.toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "SbandCode")
            connection.connectTimeout = 8_000
            connection.readTimeout = 15_000
            try {
                val code = connection.responseCode
                if (code in setOf(301, 302, 303, 307, 308)) {
                    address = address.resolve(connection.getHeaderField("Location") ?: error("HTTP $code"))
                } else {
                    check(code in 200..299) { tr("Скачать скилл не удалось (HTTP $code).", "Could not download the skill (HTTP $code).") }
                    return connection.inputStream.use { read(it, limit) } to address.path.orEmpty().substringAfterLast('/')
                }
            } finally {
                connection.disconnect()
            }
        }
        error(tr("Слишком много перенаправлений.", "Too many redirects."))
    }

    fun read(input: InputStream, limit: Int = MAX_BYTES): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) return output.toByteArray()
            check(output.size() + size <= limit) { tr("Пакет скилла слишком большой.", "The skill package is too large.") }
            output.write(buffer, 0, size)
        }
    }

    fun install(directory: File, bytes: ByteArray, name: String, remote: Boolean): String {
        val zipped = name.endsWith(".zip", true) || bytes.take(2) == listOf(80.toByte(), 75.toByte())
        val entries = if (zipped) unzip(bytes) else mapOf("SKILL.md" to bytes)
        val skill = entries.keys.filter { it.substringAfterLast('/') == "SKILL.md" }.minByOrNull { it.count { c -> c == '/' } }
            ?: error(tr("В архиве нет SKILL.md.", "There's no SKILL.md in the archive."))
        return installEntries(directory, entries, skill, name, remote)
    }

    @Synchronized
    private fun installEntries(directory: File, entries: Map<String, ByteArray>, skill: String, name: String, remote: Boolean): String {
        val root = skill.substringBeforeLast('/', "")
        val text = entries.getValue(skill).toString(Charsets.UTF_8)
        val header = text.removePrefix("\uFEFF").replace("\r\n", "\n").takeIf { it.startsWith("---\n") }
            ?.substringAfter("\n")?.substringBefore("\n---", "")
        val title = header?.let { namePattern.find(it)?.groupValues?.get(1)?.trim() }
        if (remote) check(!title.isNullOrBlank()) {
            tr("Файл должен быть SKILL.md с полем name в заголовке.", "The file must be SKILL.md with a name in its front matter.")
        }
        val fallback = root.substringAfterLast('/').ifBlank { name.substringBeforeLast('.') }
        val slug = (title ?: fallback).lowercase().replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('.', '-').ifBlank { "skill" }.take(100)
        val destination = File(directory, slug)
        if (remote) check(!destination.exists()) { tr("Скилл «$slug» уже установлен.", "The skill '$slug' is already installed.") }
        directory.mkdirs()
        val staging = Files.createTempDirectory(directory.parentFile!!.toPath(), ".skill-").toFile()
        val fresh = File(staging, "new").apply { mkdir() }
        val previous = File(staging, "previous")
        try {
            for ((path, data) in entries) {
                if (root.isNotEmpty() && !path.startsWith("$root/")) continue
                val out = File(fresh, if (root.isEmpty()) path else path.removePrefix("$root/"))
                out.parentFile?.mkdirs()
                out.writeBytes(data)
            }
            if (destination.exists()) Files.move(destination.toPath(), previous.toPath())
            try {
                Files.move(fresh.toPath(), destination.toPath())
            } catch (failure: Exception) {
                if (previous.exists()) Files.move(previous.toPath(), destination.toPath())
                throw failure
            }
        } finally {
            if (!previous.exists() || destination.exists()) staging.deleteRecursively()
        }
        return title ?: slug
    }

    private fun validatePath(path: String) {
        require(path.length <= 512 && '\\' !in path &&
            path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }) {
            tr("Недопустимый путь в пакете скилла.", "Invalid path in the skill package.")
        }
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var remaining = MAX_BYTES
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val path = entry.name
                validatePath(path)
                check(result.size < 512 && path !in result) { tr("Некорректный ZIP скилла.", "Invalid skill ZIP.") }
                val data = read(zip, remaining)
                remaining -= data.size
                result[path] = data
            }
        }
        return result
    }
}
