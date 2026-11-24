package app.sbandcode.android.engine

import app.sbandcode.android.tr
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

    fun download(url: String): Pair<ByteArray, String> {
        var address = URI(url.trim())
        val parts = address.rawPath.orEmpty().split('/')
        if (address.host == "github.com" && parts.size >= 6 && parts[3] == "blob") {
            address = URI("https://raw.githubusercontent.com/${parts[1]}/${parts[2]}/${parts.drop(4).joinToString("/")}")
        }
        repeat(6) {
            require(address.scheme == "https" && address.host != null && address.userInfo == null) {
                tr("Нужна HTTPS-ссылка на SKILL.md или ZIP.", "Use an HTTPS link to SKILL.md or ZIP.")
            }
            val connection = address.toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 15_000
            try {
                val code = connection.responseCode
                if (code in setOf(301, 302, 303, 307, 308)) {
                    address = address.resolve(connection.getHeaderField("Location") ?: error("HTTP $code"))
                } else {
                    check(code in 200..299) { tr("Скачать скилл не удалось (HTTP $code).", "Could not download the skill (HTTP $code).") }
                    return connection.inputStream.use { read(it) } to address.path.orEmpty().substringAfterLast('/')
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

    @Synchronized
    fun install(directory: File, bytes: ByteArray, name: String, remote: Boolean): String {
        val zipped = name.endsWith(".zip", true) || bytes.take(2) == listOf(80.toByte(), 75.toByte())
        val entries = if (zipped) unzip(bytes) else mapOf("SKILL.md" to bytes)
        val skill = entries.keys.filter { it.substringAfterLast('/') == "SKILL.md" }.minByOrNull { it.count { c -> c == '/' } }
            ?: error(tr("В архиве нет SKILL.md.", "There's no SKILL.md in the archive."))
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

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var remaining = MAX_BYTES
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val path = entry.name
                require(path.length <= 512 && '\\' !in path &&
                    path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }) {
                    tr("Недопустимый путь в архиве.", "Invalid path in the archive.")
                }
                check(result.size < 512 && path !in result) { tr("Некорректный ZIP скилла.", "Invalid skill ZIP.") }
                val data = read(zip, remaining)
                remaining -= data.size
                result[path] = data
            }
        }
        return result
    }
}
