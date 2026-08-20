# CarpetPlayers — Changelog

All notable changes to CarpetPlayers Mod/Plugin are documented here.

---

## v1.2.0 — Advanced Features (Unreleased)

### Added
- **Bot Rank System (Phase 4.1):** Admin/Moderator/User ranks with persistent JSON config (`carpetplayers-ranks.json`). Rank-based spawn limits and `/carpetplayers rank set/list/remove/default` commands. All 3 platforms.
- **New AI Tools (Phase 4.2):** 6 new tools across all platforms:
  - `set_blocks` — WorldEdit //set equivalent (set rectangular region to block type)
  - `replace_blocks` — WorldEdit //replace equivalent (replace block types in region)
  - `copy_region` / `paste_region` — clipboard-based block copy and paste
  - `read_file` — AI can read `.java`, `.json`, `.yml` source files
  - `group_command` — send commands to multiple bots at once
- **Persistent Bot Configs (Phase 4.3):** Bot configurations (names, positions, states) survive server restart. Auto-save every 5 minutes + on shutdown. Config: `persistentBots`, `autoSaveIntervalMinutes`.

### Changed
- `run_command` AI tool ported to Paper 1.16.5 (was Fabric/1.21.11 only)
- Updated `Mods` rank system to be opt-in (`rankSystemEnabled = false` by default)
- All 3 platforms now have 21 AI tools (up from 15)

---

## v1.1.0 — Client UI + Advanced Features

### Added
- **Client-side GUI (Phase 3):** `CarpetPlayersScreen` with spawn/remove/kit/control/settings panels
- **Title Screen button:** "Carpet Players" button via `TitleScreenMixin`
- **Pause Screen button:** "Carpet Players" button via `PauseScreenMixin`
- **Menu command:** `/carpetplayers menu` opens GUI from in-game
- **Network packets:** ModPackets, ServerNetworking for client-server communication
- **Real-time bot status display** in GUI
- **One-click kit application** from UI

---

## v1.0.0 — Initial Release

### Features
- Bot spawning system (normal + PvP bots)
- Bot AI brain with 5 behavior states (follow, wander, PvP, chill, eat)
- 15 AI tools for bot control
- Multi-provider AI system (OpenAI, Gemini, OpenRouter, Groq, Local Ollama)
- PvP combat system (weapon switching, W/A/S/D tap, multiple weapons)
- Kit system (6 PvP kits with full enchants)
- Chat command system (`!botname command`)
- Bot control mode (player mirrors movement to bot)
- Defensive AI (reacts to attacks)
- JSON-based configuration
- Multi-platform: Fabric 1.16.5 mod + Paper 1.16.5 plugin + Paper 1.21.11 plugin
- `run_command` AI tool — bots can execute server commands
- ExecutorService thread pool for AI
- Graceful shutdown hooks

---

*This project is AI-assisted. Core architecture, ideas, and many fixes are human-driven. See README.md for details.*
