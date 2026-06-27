# kotomemo

A Notepad-style text editor and AI integration, built with **Kotlin Multiplatform + Compose Desktop** on **GraalVM 21**.

> ⚠️ Early development. Core editor and HTTP integration work; single-binary Native Image distribution are still on the roadmap.

> 📌 **About the version numbering**: release tags start at `v1.0.x` because the underlying `jpackage` installer toolchain rejects a major version of `0` in its installer-metadata format. The bump to `1.x` is a packaging-constraint workaround, **not** a stability claim — the project is still very much a work in progress and many roadmap items below remain unfinished. Treat anything before a proper announcement as actively evolving.

## Goals

- A Notepad replacement worth using in 2026: tabs, regex, sane encoding handling
- Generic, configurable HTTP integration so any LLM / webhook / private backend can receive the selected text

## Features

### Editor core
- Multi-tab editing with dirty markers
- Open / Save / Save As with **first-line filename suggestion**
- Encoding: UTF-8 / UTF-16, BOM toggle, CRLF/LF switch
- Undo / Redo, Cut / Copy / Paste, Select All
- **Regex** find/replace (single + all), wrap-around indicator
- Multi-line **bulk indent** (Tab / Shift+Tab)
- Font family/size + zoom (Ctrl+= / Ctrl+- / Ctrl+0)
- **Simple syntax highlighting** (comments + strings) for c-family, hash, xml, sql

### HTTP API integration
- Per-preset URL, method, headers, body template
- Template placeholders: `{{selection}}`, `{{selectionJson}}`, `{{filename}}`, `{{tokens.NAME}}`
- Response targets: **new tab / insert after selection / status only**
- JSON path extraction (e.g. `choices.0.message.content`)
- Tokens stored in `~/.kotomemo/config` (plain text, MIT-licensed editor — bring your own secret hygiene)

### Misc
- CLI launch: `kotomemo path/to/file.txt`
- Status bar with `Ln/Col`, charset, line ending, BOM, zoom %, send status

## Architecture

Clean architecture, all Kotlin:

```
entity/     pure domain types (Contents, ApiPreset, ...)
usecase/    Command<I, O> use cases + ports
adapter/    controllers, repositories, HTTP client impl
framework/  Compose UI, file IO, AWT bridge, entrypoint
```

The `framework` layer depends on `adapter` depends on `usecase` depends on `entity`. Repository ports live under `usecase/port/` so the dependency arrow always points inward.

## Requirements

- **GraalVM Community Edition 21** (auto-detected; otherwise auto-downloaded via the Foojay resolver)
- Java toolchain handling is project-local — your system `JAVA_HOME` is not used at runtime

## Build

```bash
# Compile + run all unit tests
./gradlew :composeApp:jvmTest

# Run from Gradle
./gradlew :composeApp:run

# Open a file at startup
./gradlew :composeApp:run --args="path/to/file.txt"
```

## Distribution

```bash
# App image (folder, no installer; ~160 MB with bundled JRE)
./gradlew :composeApp:createDistributable
# Output: composeApp/build/compose/binaries/main/app/kotomemo/

# Windows MSI installer (~72 MB)
./gradlew :composeApp:packageMsi
# Output: composeApp/build/compose/binaries/main/msi/kotomemo-1.0.0.msi

# OS-appropriate installer (MSI on Windows, DMG on macOS, DEB on Linux)
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Roadmap

- [ ] Native Image build (single-file `kotomemo.exe` without bundled JRE)
- [ ] GitHub Actions CI for build + tests on Windows / macOS / Linux
- [ ] GitHub Actions release workflow for distributions
- [ ] VSCode TextMate grammar import for richer syntax highlighting
- [ ] Better IME support (CorvusSKK / TSF) — currently Google IME works, CorvusSKK does not
- [ ] Helpers for Windows context menu / "open with" registration

## License

[MIT](LICENSE)
