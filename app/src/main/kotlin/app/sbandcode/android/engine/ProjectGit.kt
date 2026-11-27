package app.sbandcode.android.engine

import android.util.Base64
import app.sbandcode.android.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.TimeUnit

class ProjectGit(
    private val directory: File,
    private val linux: Linux,
) {
    data class Change(val path: String, val status: String)
    data class Status(val branch: String, val changes: List<Change>, val branches: List<String>, val history: String)
    data class GitHubDestination(val repository: String, val branch: String) {
        val url get() = "https://github.com/$repository.git"
    }

    suspend fun status(): Status {
        val status = git("status", "--porcelain=v1", "-z", "--no-renames")
        val changes = status.split('\u0000').filter { it.length > 3 }.map { Change(it.substring(3), it.take(2)) }
        val branch = git("symbolic-ref", "--quiet", "--short", "HEAD", allowFailure = true).trim().ifEmpty { "HEAD" }
        val branches = git("for-each-ref", "--format=%(refname:short)", "refs/heads/").lineSequence().filter { it.isNotBlank() }.toList()
        val history = git("log", "-30", "--date=short", "--format=%h  %ad  %s", allowFailure = true).trim()
        return Status(branch, changes, branches, history)
    }

    suspend fun diff(paths: List<String>): String {
        val selected = paths.map { ":(top,literal)$it" }
        val options = listOf("--no-ext-diff", "--no-textconv", "--stat", "--patch", "--")
        return git("diff", *options.toTypedArray(), *selected.toTypedArray()).take(9_000) +
            git("diff", "--cached", *options.toTypedArray(), *selected.toTypedArray()).take(3_000)
    }

    suspend fun switchBranch(branch: String) {
        git("switch", "--", branch)
    }

    suspend fun githubDestination(): GitHubDestination? {
        val branch = git("symbolic-ref", "--quiet", "--short", "HEAD", allowFailure = true).trim()
        val remote = git("remote", "get-url", "--push", "--all", "origin", allowFailure = true)
            .lineSequence().map(String::trim).filter(String::isNotEmpty).singleOrNull()
        val repository = remote?.let(::githubRepository)
        return if (branch.isNotEmpty() && repository != null) GitHubDestination(repository, branch) else null
    }

    suspend fun pushGitHub(destination: GitHubDestination, token: String) {
        check(githubDestination() == destination) {
            tr("Ветка или origin изменились. Обновите экран и повторите.", "The branch or origin changed. Refresh and try again.")
        }
        val authorization = "Authorization: Basic " + Base64.encodeToString(
            "x-access-token:$token".toByteArray(Charsets.UTF_8), Base64.NO_WRAP,
        )
        val http = "http.${destination.url}"
        val config = listOf(
            "credential.helper" to "",
            "$http.proxy" to "",
            "$http.sslVerify" to "true",
            "$http.sslCAInfo" to "/etc/ssl/certs/ca-certificates.crt",
            "$http.followRedirects" to "false",
            "$http.extraHeader" to "",
            "$http.extraHeader" to authorization,
        )
        val pushEnvironment = buildMap {
            put("GIT_ALLOW_PROTOCOL", "https")
            put("GIT_CONFIG_COUNT", config.size.toString())
            config.forEachIndexed { index, (key, value) ->
                put("GIT_CONFIG_KEY_$index", key)
                put("GIT_CONFIG_VALUE_$index", value)
            }
        }
        val ref = "refs/heads/${destination.branch}"
        try {
            git("push", "--no-follow-tags", "--", destination.url, "$ref:$ref", extraEnvironment = pushEnvironment, timeoutSeconds = 180)
        } catch (failure: IllegalStateException) {
            val message = failure.message.orEmpty()
                .replace(authorization.substringAfter("Basic "), "[redacted]")
                .replace(token, "[redacted]")
            error(message.ifBlank { tr("Не удалось отправить ветку", "Could not push the branch") })
        }
    }

    suspend fun commit(message: String, paths: List<String>) {
        require(message.isNotBlank() && paths.isNotEmpty())
        // Intent to add lets --only include newly created files while preserving unrelated staged changes.
        val selected = paths.map { ":(top,literal)$it" }
        val new = status().changes.filter { it.status == "??" && it.path in paths }.map { ":(top,literal)${it.path}" }
        if (new.isNotEmpty()) git("add", "--intent-to-add", "--", *new.toTypedArray())
        try {
            git("commit", "--only", "-m", message.trim(), "--", *selected.toTypedArray())
        } catch (failure: Exception) {
            if (new.isNotEmpty()) kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                runCatching { git("reset", "--", *new.toTypedArray()) }
            }
            throw failure
        }
    }

    suspend fun canCheckpoint(): Boolean = linux.installed &&
        git("rev-parse", "--show-toplevel", allowFailure = true).isNotBlank()

    private suspend fun checkpointRef(): String {
        val branch = git("symbolic-ref", "--quiet", "HEAD", allowFailure = true).trim()
        val revision = branch.ifEmpty { "detached/" + git("rev-parse", "HEAD").trim() }
        return "refs/sbandcode/checkpoints/$revision"
    }

    suspend fun checkpoint() {
        git("update-ref", checkpointRef(), workingTree())
    }

    suspend fun checkpointFiles(): List<String>? {
        val saved = git("rev-parse", "--verify", checkpointRef(), allowFailure = true).trim().ifEmpty { return null }
        return git("diff", "--no-renames", "--name-only", "-z", saved, workingTree())
            .split('\u0000').filter(String::isNotEmpty)
    }

    suspend fun restoreCheckpoint(paths: List<String>) {
        require(paths.isNotEmpty())
        val root = File(git("rev-parse", "--show-toplevel").trim())
        val repository = ProjectGit(root, linux)
        val saved = repository.git("rev-parse", "--verify", repository.checkpointRef()).trim()
        val working = repository.workingTree()
        val patch = repository.git("diff", "--binary", "--full-index", "--no-ext-diff", "--no-textconv",
            "--no-renames", saved, working, "--", *paths.map { ":(top,literal)$it" }.toTypedArray())
        check(patch.isNotBlank()) { tr("Эти файлы уже совпадают с точкой", "Those files already match the checkpoint") }
        repository.git("apply", "--reverse", "--check", "-", input = patch)
        check(repository.workingTree() == working) { tr("Рабочие файлы изменились. Повторите сравнение.", "Working files changed. Compare again.") }
        repository.git("apply", "--reverse", "-", input = patch)
    }

    private suspend fun workingTree(): String {
        val env = linux.env(true)
        check(env.isNotEmpty()) { tr("Установите Alpine и пакет git", "Install Alpine and the git package") }
        val directory = File(env.getValue("SBANDCODE_CACHE"))
        val realIndex = File(git("rev-parse", "--path-format=absolute", "--git-path", "index").trim())
        val index = File.createTempFile("sband-index-", ".tmp", directory)
        check(index.delete())
        val environment = mapOf("GIT_INDEX_FILE" to index.absolutePath)
        try {
            if (realIndex.isFile) realIndex.copyTo(index)
            else git("read-tree", "--empty", extraEnvironment = environment)
            git("add", "-A", "--", ".", extraEnvironment = environment)
            return git("write-tree", extraEnvironment = environment).trim()
        } finally {
            index.delete()
            File(index.path + ".lock").delete()
        }
    }

    suspend fun cloneGitHub(remote: String): File = runInterruptible(Dispatchers.IO) {
        val repository = githubRepository(remote.trim())
            ?: error(tr("Вставьте ссылку на репозиторий GitHub", "Paste a GitHub repository URL"))
        val target = File(directory, repository.substringAfter('/'))
        check(directory.isDirectory && !target.exists()) { tr("Папка уже существует или недоступна", "The folder already exists or is unavailable") }
        val env = linux.env(true)
        check(env.isNotEmpty()) { tr("Установите Alpine и пакет git", "Install Alpine and the git package") }
        runGit(directory, env, listOf("clone", "--no-recurse-submodules", "--template=", "--",
            "https://github.com/$repository.git", target.absolutePath), false, null,
            extraEnvironment = mapOf("GIT_ALLOW_PROTOCOL" to "https"), timeoutSeconds = 180)
        target
    }

    suspend fun defaultBase(): String = git("symbolic-ref", "--short", "refs/remotes/origin/HEAD", allowFailure = true)
        .trim().removePrefix("origin/").ifEmpty { "main" }

    suspend fun pullRequestSummary(base: String): String {
        val baseRef = "refs/remotes/origin/$base"
        val localBase = "refs/heads/$base"
        val tracking = git("rev-parse", "--verify", baseRef, allowFailure = true).trim()
        val fallback = git("rev-parse", "--verify", localBase, allowFailure = true).trim()
        val sha = tracking.ifEmpty { fallback }
        return if (sha.isNotEmpty()) git("log", "-12", "--format=%s%n%b", "$sha..HEAD").trim()
            else git("log", "-1", "--format=%s%n%b").trim()
    }

    suspend fun reverse(path: String, patch: String) {
        val root = File(git("rev-parse", "--show-toplevel").trim())
        val target = File(root, path)
        check(target.canonicalFile.toPath() == root.canonicalFile.toPath().resolve(path) &&
            target.toPath().normalize().startsWith(root.toPath())) {
            tr("Небезопасный путь к файлу", "Unsafe file path")
        }
        check(patch.lineSequence().count { it.startsWith("diff --git ") } == 1) {
            tr("Патч не содержит одного изменения файла", "The patch does not contain a single file change")
        }
        val index = Regex("(?m)^index ([a-f0-9]+)\\.\\.([a-f0-9]+)(?: [0-7]+)?$")
            .find(patch) ?: error(tr("У патча нет контрольной суммы", "The patch has no checksum"))
        val expected = index.groupValues[2]
        val unchanged = if (expected.all { it == '0' }) !target.exists() else {
            target.isFile && git("hash-object", "--no-filters", "--", target.absolutePath).trim().startsWith(expected)
        }
        check(unchanged) { tr("Файл изменился после правок агента", "The file changed after the agent edited it") }
        val repository = ProjectGit(root, linux)
        repository.git("apply", "--reverse", "--check", "--", "-", input = patch)
        repository.git("apply", "--reverse", "--", "-", input = patch)
    }

    private suspend fun git(
        vararg arguments: String,
        allowFailure: Boolean = false,
        input: String? = null,
        extraEnvironment: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 30,
    ): String = runInterruptible(Dispatchers.IO) {
        val env = linux.env(true)
        check(env.isNotEmpty()) { tr("Установите Alpine и пакет git", "Install Alpine and the git package") }
        val root = if (arguments.firstOrNull() == "rev-parse" || arguments.firstOrNull() == "apply") directory
            else File(runGit(directory, env, listOf("rev-parse", "--show-toplevel"), false, null).trim())
        runGit(root, env, arguments.toList(), allowFailure, input, extraEnvironment, timeoutSeconds)
    }

    private fun runGit(
        directory: File,
        env: Map<String, String>,
        arguments: List<String>,
        allowFailure: Boolean,
        input: String?,
        extraEnvironment: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 30,
    ): String {
        val output = File.createTempFile("sband-git-", ".log", File(env.getValue("SBANDCODE_CACHE")))
        try {
            val process = ProcessBuilder(listOf(
                env.getValue("SHELL"), "-c", "exec git \"\$@\"", "git",
                "--no-pager", "-c", "core.hooksPath=/dev/null", "-c", "core.fsmonitor=false",
            ) + arguments)
                .directory(directory).redirectErrorStream(true).redirectOutput(output)
                .apply {
                    if (extraEnvironment.isNotEmpty()) environment().keys.removeAll {
                        it.startsWith("GIT_TRACE") || it.startsWith("GIT_SSL_") ||
                            it in setOf("GIT_CONFIG_PARAMETERS", "GIT_CURL_VERBOSE", "GIT_ASKPASS", "SSH_ASKPASS")
                    }
                    environment().putAll(env)
                    environment().putAll(extraEnvironment)
                    environment()["PWD"] = directory.absolutePath
                    environment()["GIT_TERMINAL_PROMPT"] = "0"
                }.start()
            try {
                process.outputStream.use { stream -> input?.let { stream.write(it.toByteArray(Charsets.UTF_8)) } }
                check(process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { tr("Git не ответил вовремя", "Git timed out") }
                val text = output.readText()
                if (process.exitValue() == 0) return text
                if (allowFailure) return ""
                error(text.trim().ifBlank { "Git exited with code ${process.exitValue()}" })
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        } finally {
            output.delete()
        }
    }
}

private fun githubRepository(remote: String): String? {
    val path = when {
        remote.startsWith("https://github.com/", ignoreCase = true) -> remote.substring("https://github.com/".length)
        remote.startsWith("git@github.com:", ignoreCase = true) -> remote.substring("git@github.com:".length)
        remote.startsWith("ssh://git@github.com/", ignoreCase = true) -> remote.substring("ssh://git@github.com/".length)
        else -> return null
    }.removeSuffix("/").removeSuffix(".git")
    return path.takeIf { it.matches(Regex("[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+")) && it.substringAfter('/') !in setOf(".", "..") }
}
