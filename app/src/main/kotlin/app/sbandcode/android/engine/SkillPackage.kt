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
    private const val BUNDLE_INFO = ".sbandcode-bundle"
    data class Installed(val title: String, val count: Int, val bundle: Boolean = false)

    private val namePattern = Regex("""(?m)^name:[ \t]*["']?([^"'\r\n]+)""")

    fun installFromUrl(directory: File, url: String): Installed {
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

    private fun installRepository(directory: File, owner: String, repo: String): Installed {
        val api = "https://api.github.com/repos/$owner/$repo"
        val head = githubJson("$api/commits/HEAD").getString("sha")
        check(head.matches(Regex("[a-f0-9]{40}"))) { "Invalid GitHub commit" }
        val tree = githubJson("$api/git/trees/$head?recursive=1")
        check(!tree.optBoolean("truncated")) {
            tr("Список файлов GitHub неполный. Используйте ссылку на SKILL.md.", "GitHub returned an incomplete file list. Link to SKILL.md.")
        }
        val files = tree.optJSONArray("tree").objects()
            .filter { it.str("type") == "blob" && it.str("mode") in setOf("100644", "100755") }
            .mapNotNull { entry -> entry.str("path")?.let { it to entry.optLong("size") } }.toMap()
        val manifests = manifestPaths(files.keys)
        check(manifests.size <= 128) {
            tr("В репозитории слишком много скиллов. Укажите ссылку на SKILL.md.", "Too many skills. Link to an individual SKILL.md.")
        }
        var remaining = MAX_BYTES
        val entries = linkedMapOf<String, ByteArray>()
        fun fetch(path: String) {
            validatePath(path)
            check(files.getValue(path) <= remaining) { tr("Набор скиллов слишком большой.", "The skill bundle is too large.") }
            val raw = URI("https", "raw.githubusercontent.com", "/$owner/$repo/$head/$path", null)
            val (bytes, _) = download(raw, remaining)
            remaining -= bytes.size
            entries[path] = bytes
        }

        manifests.forEach(::fetch)
        val chosen = selectedManifests(entries, manifests, remote = true)
        val roots = manifests.map { it.substringBeforeLast('/', "") }.toSet()
        val chosenRoots = chosen.map { it.substringBeforeLast('/', "") }.toSet()
        val support = files.keys.filter { path ->
            if (path in entries) return@filter false
            val root = owningRoot(path, roots) ?: return@filter false
            root in chosenRoots && (root.isNotEmpty() || path.substringBefore('/') in setOf("scripts", "references", "assets"))
        }
        check(support.size + manifests.size <= 512) {
            tr("В наборе слишком много файлов.", "The bundle contains too many files.")
        }
        support.forEach(::fetch)
        return installDownloaded(directory, entries, manifests, repo, "$owner-$repo", remote = true)
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

    fun install(directory: File, bytes: ByteArray, name: String, remote: Boolean): Installed {
        val zipped = name.endsWith(".zip", true) || bytes.take(2) == listOf(80.toByte(), 75.toByte())
        val entries = if (zipped) unzip(bytes) else mapOf("SKILL.md" to bytes)
        val label = name.substringBeforeLast('.').ifBlank { "Skills" }
        return installDownloaded(directory, entries, manifestPaths(entries.keys), label, label, remote)
    }

    private fun manifestPaths(paths: Set<String>): List<String> =
        paths.filter { it.substringAfterLast('/') == "SKILL.md" }
            .sortedWith(compareBy<String> {
                when {
                    it.startsWith("skills/") -> 0
                    it.split('/').any { segment -> segment.startsWith('.') } -> 2
                    else -> 1
                }
            }.thenBy { it.count { c -> c == '/' } }.thenBy { it })

    private fun skillName(data: ByteArray): String? {
        val text = data.toString(Charsets.UTF_8).removePrefix("\uFEFF").replace("\r\n", "\n")
        val header = text.takeIf { it.startsWith("---\n") }?.substringAfter("\n")?.substringBefore("\n---", "")
        return header?.let { namePattern.find(it)?.groupValues?.get(1)?.trim() }?.takeIf { it.isNotBlank() }
    }

    private fun selectedManifests(entries: Map<String, ByteArray>, manifests: List<String>, remote: Boolean): List<String> {
        check(manifests.isNotEmpty()) { tr("В пакете нет SKILL.md.", "There's no SKILL.md in the package.") }
        return manifests.distinctBy { path ->
            val name = skillName(entries.getValue(path))
            if (remote || manifests.size > 1) check(name != null) {
                tr("У $path нет поля name в заголовке.", "$path is missing a front-matter name.")
            }
            name ?: path
        }
    }

    private fun owningRoot(path: String, roots: Set<String>): String? =
        roots.filter { it.isEmpty() || path.startsWith("$it/") }.maxByOrNull { it.length }

    private fun slug(title: String): String = title.lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-").trim('.', '-').ifBlank { "skill" }.take(100)

    @Synchronized
    private fun installDownloaded(
        directory: File,
        entries: Map<String, ByteArray>,
        manifests: List<String>,
        label: String,
        key: String,
        remote: Boolean,
    ): Installed {
        val chosen = selectedManifests(entries, manifests, remote)
        val roots = manifests.map { it.substringBeforeLast('/', "") }.toSet()
        val bundled = chosen.size > 1
        val first = chosen.first()
        val title = skillName(entries.getValue(first)) ?: first.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { label }
        val folder = if (bundled) "pack-${slug(key)}" else slug(title)
        val destination = File(directory, folder)
        val ownBundle = bundled && File(destination, BUNDLE_INFO).takeIf(File::isFile)?.readText() == label
        if (remote) check(!destination.exists() || ownBundle) {
            tr("Скилл или набор «$folder» уже установлен.", "The skill or bundle '$folder' is already installed.")
        }
        directory.mkdirs()
        val staging = Files.createTempDirectory(directory.parentFile!!.toPath(), ".skill-").toFile()
        val fresh = File(staging, "new").apply { mkdir() }
        val previous = File(staging, "previous")
        try {
            if (bundled) File(fresh, BUNDLE_INFO).writeText(label)
            val destinations = chosen.associate { manifest ->
                val root = manifest.substringBeforeLast('/', "")
                val name = skillName(entries.getValue(manifest)) ?: title
                val member = if (bundled) File(fresh, slug(name)) else fresh
                if (bundled) check(member.mkdir()) {
                    tr("В наборе повторяется имя скилла «$name».", "Duplicate skill folder for '$name'.")
                }
                root to member
            }
            for ((path, data) in entries) {
                val root = owningRoot(path, roots) ?: continue
                val member = destinations[root] ?: continue
                val relative = if (root.isEmpty()) path else path.removePrefix("$root/")
                validatePath(relative)
                val out = File(member, relative)
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
        return Installed(if (bundled) label else title, chosen.size, bundled)
    }

    fun listBundleMembers(directory: File): List<JSONObject> = directory.listFiles().orEmpty().flatMap { bundle ->
        val marker = File(bundle, BUNDLE_INFO)
        if (!marker.isFile || bundle.canonicalFile.parentFile != directory.canonicalFile) return@flatMap emptyList()
        val label = marker.readText()
        bundle.listFiles().orEmpty().mapNotNull { member ->
            val manifest = File(member, "SKILL.md")
            if (!manifest.isFile || member.canonicalFile.parentFile != bundle.canonicalFile) return@mapNotNull null
            val name = skillName(manifest.inputStream().use { read(it) }) ?: return@mapNotNull null
            JSONObject().put("name", name).put("path", manifest.absolutePath).put("bundle", label).put("bundleID", bundle.name)
        }
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
