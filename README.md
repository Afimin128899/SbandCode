<div align="center">

<img src="assets/banner.png" alt="SbandCode" width="640">

**An AI client for Android with the real opencode inside.**
The models, providers and agent of [opencode](https://opencode.ai), in an app made for a phone.

[![licence](https://img.shields.io/badge/licence-MIT-444444?style=flat-square)](#licence)

</div>

---

SbandCode is maintained by [Afimin128899](https://github.com/Afimin128899).
It starts `opencode serve`
on the phone and talks to it over its HTTP API, the same way opencode's desktop
app does. So what works in opencode on a computer works here, and the screens
are drawn for a thumb instead of a terminal.

Android 9 and up, arm64 only. No root. Provider credentials and chat history are stored on the phone.

## What it does

Use the Chat, Tools, and Settings tabs to switch between your conversation, project tools, and app preferences. The chat header opens history or the model picker. The plus button in the composer opens attachments, model options, and the working directory.

- Review the files changed in the latest agent turn, copy a patch, or reverse a Git patch when the edited file still matches.
- Search chat titles and messages by project, fork a chat at a message, and share text or a file from Android to a new or current chat.
- Save the model, agent mode, and command and file approval rules for each project folder. Permission prompts show the command and working directory whenever OpenCode provides them.
- Queue follow-ups during a reply. The queue survives an app restart and can be resumed in the chat. Enable chat notifications in Settings for completed turns and permission requests while in the background.
- Open Git from the menu for status, history, local branches and commits from selected files. Install Alpine and its `git` package first. Web preview opens a local server at a loopback URL with an explicit port.
- Check required and free space before downloading a local model, and pause, resume or delete downloads in the model screen.

Use **+ → Files** in the composer to attach documents. To install a skill from chat, send `/skill https://example.com/SKILL.md` or use a HTTPS link to a ZIP containing a skill. The app shows the result in the chat and lists installed skills under **Tools → Skills and MCP**.

<details>
<summary><b>Every model opencode knows</b></summary>

OpenCode Go and Zen, opencode's free models, sign-in with ChatGPT and GitHub
Copilot, and an API key for any provider on models.dev. OpenAI-compatible endpoints such as LM Studio, Ollama or a proxy can be added in **Settings → Custom API and models**. Enter the base URL, an API key if required, and model IDs manually or load them from the server. Advanced provider configuration is available in the `opencode.json` editor.

The free models have a usage limit. On a VPN it can show up as already used, and
the same models answered without one, so it seems to be counted by IP address.
</details>

<details>
<summary><b>Claude on your Pro or Max plan</b></summary>

This is opencode's own Claude Agent provider, which works through the official
Claude Code. Anthropic publishes no build for Android, so the app downloads the
Linux one from npm when you ask (about 145 MB) and runs it through musl's loader.
It isn't in the apk and it isn't changed.

You sign in to Claude Code itself: a link, a code, done. opencode and the app
never see the token. A free Claude account won't work, because Claude Code isn't
available on the free plan.
</details>

<details>
<summary><b>An agent that can do things</b></summary>

The model reads and writes files and runs commands, and you can follow each step.
It works in a folder you pick for each new chat: the app's own folder, the whole
phone storage, or a folder of your own. Anything outside the app's folder asks
for Android's "all files" permission first, and opencode asks before it acts
where its rules say so.
</details>

<details>
<summary><b>A real Linux for it</b></summary>

Android's shell has no package manager, so the app can install Alpine Linux
(about 4 MB, from Alpine, checked by its hash) and run the model's commands there
through proot. The model installs what it needs with `apk add`: Python, Node, git,
compilers. Your folders keep their real paths inside.

There is a small console in the settings for commands of your own.
</details>

<details>
<summary><b>Models on the phone itself</b></summary>

Gemma 4 and Qwen 3.5 from a list, or any GGUF file by its link on Hugging Face,
run by llama.cpp. They work offline and cost nothing, and they are much slower
and weaker than cloud models.

Measured on a Snapdragon 8 Gen 2 with Gemma 4 E2B: up to 17 tokens a second on a
cool phone, down to about 3 to 6 once it is hot. Reading the prompt is the slow
part, at 20 to 30 tokens a second. opencode's tools add about 5000 tokens to it,
which means over four minutes before the first word. So these models start in
**Chat** mode, without tools, which takes about 20 seconds. **Agent** mode is one
tap away if you are willing to wait.
</details>

<details>
<summary><b>Skills, MCP and rules</b></summary>

Skills from a `SKILL.md` or a zip, remote and local MCP servers, and global rules
in `AGENTS.md`. They are kept where opencode keeps them.
</details>

<details>
<summary><b>Limits</b></summary>

Your Claude plan's windows, asked of Claude Code itself with no model call, and
the OpenCode Go allowance with Zen budgets. opencode shows those two only when
you sign in with an OpenCode account, not with a key alone.
</details>

<details>
<summary><b>Chats: export and import</b></summary>

Any chat can be saved to a file in opencode's own transfer format and loaded back. Use **Settings → Data** to back up and restore all chats in one file. Restoration skips existing chat IDs.

</details>

## How it is built

| Path | What it is |
|---|---|
| `app/` | the Android app: Kotlin and Compose, Material 3 |
| `engine/build-engine.sh` | opencode 2.2.5 from source, on the official Bun for Android |
| `engine/build-loader.sh` | musl's loader from Alpine and the wrapper for Claude Code |
| `engine/build-linux.sh` | proot (Termux's fork) and the wrapper for Alpine |
| `engine/build-llama.sh` | llama.cpp's server, in two builds picked by the phone's processor |
| `assets/`, `art/` | the banner, the logo, and the scripts that draw them |

The programs live in `jniLibs` under library names because that is the one place
Android lets an app run programs from. Claude Code, Alpine and the models are not
in the apk: the app downloads them from their own sources when you ask.

## Building it yourself

```sh
engine/build-engine.sh
engine/build-loader.sh
engine/build-linux.sh
engine/build-llama.sh
SIGNER=/path/to/uber-apk-signer.jar ./build.sh
```

Use JDK 17, Gradle 8.11.1, Android SDK 35 and NDK 28. Check the SDK and NDK paths at
the top of the engine scripts. Set `GRADLE` if Gradle is outside your PATH.
For a signed release, use your own keystore at `keys/sbandcode.jks` with the
`sbandcode` alias. Put its password in `keys/sbandcode.pass`.
For a debug build after building the engine, run `gradle :app:assembleDebug`.

SbandCode uses the Android application ID `app.sbandcode.android`.

## Licence

MIT, in [LICENSE](LICENSE).

The apk also carries opencode and Bun (MIT), musl (MIT), llama.cpp (MIT),
[proot](https://github.com/termux/proot) (GPL-2.0) and talloc (LGPL-3.0), each
under its own licence. The scripts above fetch and build their exact sources.

SbandCode is not made by or affiliated with opencode, Anthropic, OpenAI or Google.
