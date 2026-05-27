# ![logo](logo/icon_intellij_haskell_32.png) IntelliJ plugin for Haskell

A fork of [rikvdkleij/intellij-haskell](https://github.com/rikvdkleij/intellij-haskell), modernized for current IntelliJ IDEA (2025.2 / 2026.1) and re-platformed onto [Haskell Language Server](https://haskell-language-server.readthedocs.io/) (HLS) over LSP.

The original plugin ran its own Stack/GHCi REPL processes in the background to provide diagnostics, type info, completion, and navigation. This fork retires that bespoke layer and routes language intelligence through HLS via [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij), keeping the native IntelliJ PSI for structure-only features (lexer highlighting, structure view, folding, brace matching).

# Requirements

- IntelliJ IDEA 2025.2 or newer (Community or Ultimate). Tested against `ideaIC-252.28539.54` and `ideaIC-261.x`.
- [Stack](https://docs.haskellstack.org/) for project builds and SDK detection.
- [ghcup](https://www.haskell.org/ghcup/) on `PATH` for automatic HLS installation, or HLS already installed and pointed at via plugin settings.

# Features

Provided by HLS over LSP (LSP4IJ plumbing):
- Diagnostics from GHC + HLint, with quick-fixes via `textDocument/codeAction` (Alt+Enter).
- Hover (kind/type signature + Haddock) and Quick Documentation.
- Completion with type-on-resolve.
- Goto declaration (Cmd+B / Ctrl+B) — see *Known limitations* below.
- Find Usages, Rename, Implementation and Type Definition lookups.
- Code formatting (Cmd+Opt+L / Ctrl+Alt+L) via ormolu by default; configurable through HLS.

Provided natively by the plugin:
- Lexer-based syntax highlighting (color settings page included).
- Structure view, brace matching, folding, code-block commenting.
- Stack SDK detection and project import.
- Run Configurations: GHCi console, run, test.
- Cabal file completion (module names, language extensions, package names).
- Hoogle integration: **Hoogle For It** action (Shift+Ctrl+H) and **(Re)Build Hoogle database** menu item.
- Ormolu and Stylish Haskell as explicit menu actions (alongside HLS-driven `Reformat Code`).
- Auto-install of Hoogle / Ormolu / Stylish Haskell via Stack on first project open; HLS via `ghcup install hls`.

# Installation

This fork isn't published to the JetBrains Marketplace. To use it:

1. Clone this repo.
2. Build the plugin: `sbt packageArtifact` (requires JDK 21).
3. In IntelliJ: `Settings` → `Plugins` → gear icon → `Install Plugin from Disk…` and point at `target/plugin/IntelliJ-Haskell/lib/IntelliJ Haskell.jar` (or zip the `target/plugin/IntelliJ-Haskell/` directory and install that).
4. Also install [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) from the Marketplace. The plugin declares a hard dependency on it.

# Getting started

1. Configure a *Haskell Tool Stack* SDK once per IDE: `Project Structure` → `SDKs` → `+` → `Haskell Tool Stack`, pointing at your `stack` binary.
2. Import an existing project via `File` → `New` → `Project from Existing Sources…` and select `Haskell Stack` in the wizard, or create a new project via `File` → `New` → `Project` → `Haskell`.
3. On first open the plugin will:
   - Resolve HLS: user-set path → `haskell-language-server-wrapper` on `PATH` → `ghcup install hls --set` if neither is found.
   - Auto-install Hoogle, Ormolu, and Stylish Haskell via `stack install` if not already present.
   - Download library sources and add them as source roots to your modules.
4. Open a `.hs` file. HLS will spawn (visible in the **Language Servers** tool window). Once the cradle finishes loading you'll see diagnostics, hover, completion, and so on.

# Known limitations

## Goto definition (Cmd+B / Ctrl+B) doesn't jump to library sources

HLS's `textDocument/definition` resolves project-local symbols but, on a Stack cradle, sees only `.hi` interface files for external library symbols (e.g. `putMVar` in `Control.Concurrent`). Hover and type info still work; navigation returns empty.

Workarounds:
- Configure a [`hie.yaml`](https://github.com/haskell/hie-bios#explicit-configuration) cradle that includes library packages with their sources, or switch to a `cabal.project` layout with `documentation: True` and the relevant `source-repository-package` entries so HLS finds unpacked sources on disk.
- For ad-hoc lookups, use **Hoogle For It** (Shift+Ctrl+H) to jump to the symbol's online documentation, which links through to Hackage source.

## HLS plugin-rule transient errors during cradle warmup

LSP4IJ surfaces every server-side error as a notification. HLS's import-lens (codeLens) and inlay-hint plugins occasionally fail with `Rule Failed: ImportActions` / `Rule Failed: GhcSession` while the cradle is still settling. This fork disables both features on the LSP4IJ client side (they're informational decorations) to silence the noise. Core diagnostics / hover / completion / navigation / code-actions / formatting are unaffected.

# Architecture notes

- LSP server factory: `intellij.haskell.lsp.HaskellLspServerFactory` (registered via the LSP4IJ extension point). Spawns `haskell-language-server-wrapper --lsp` with the project base dir as working directory.
- Project structure metadata (cabal stanzas, component targets, source dirs, build-depends) is parsed once at init by `intellij.haskell.external.repl.StackReplsManager` — kept under the legacy name for now; no Stack REPL processes are spawned.
- The native PSI tree (lexer + parser generated by JFlex + Grammar-Kit under `gen/`) is unchanged from upstream and powers the IntelliJ-side structure features.

If you want to contribute, read [the contributing guideline](CONTRIBUTING.md).
