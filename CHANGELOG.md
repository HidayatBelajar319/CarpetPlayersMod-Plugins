# CarpetPlayers — Changelog

All notable changes to CarpetPlayers Mod/Plugin are documented here.

---

## v1.4.0 — Phase 8: Enhanced Bot Behavior (Released 2026-08-24)

### Added
- **PvP Movement Fix (Phase 8.1):** Enhanced bot combat with natural movement
  - Strafing: bots move left/right randomly during combat
  - Sprint-reset: sprint to target → stop 2 ticks before attack → resume
  - Jump combat: random jumps when enemy is 2-4 blocks away
  - Gap management: retreat when health < 30%, aggro when enemy weak > 70%
  - Side-step: avoids projectile attacks
  - Commands:
    - `/carpetplayers pvp movement <botname> <aggressive|defensive|balanced>`
    - `/carpetplayers pvp strafe <true|false>`
    - `/carpetplayers pvp sprint-reset <true|false>`
- **Offline Mode (Phase 8.2):** Graceful fallback when AI provider unavailable
  - `OfflineBehavior.java` — behavior tree: defend → eat → follow → wander
  - Chat commands without AI: follow, stop, wander, chill, eat, goto
  - `/carpetplayers ai offline <true|false>` — toggle offline mode
  - `/carpetplayers ai status` — now shows "Mode: OFFLINE/ONLINE"
- **Environment Awareness (Phase 8.3):** Bots can see and react to their surroundings
  - `scan_area` — scan blocks in a radius
  - `goto_block` — navigate to nearest block of a type
  - `break_block` — destroy block at coordinates
- **Config File Editor (Phase 8.4/8.5):** In-game JSON config editor (Fabric only)
  - `/carpetplayers config file <filename>` — opens syntax-highlighted editor
  - Tab completion for filenames: carpetplayers-config.json, minecraft-ai/providers.json, carpetplayers-ranks.json
  - Syntax highlighting: keys (aqua), strings (green), numbers (gold), booleans (purple), null (gray), brackets (white)
  - Buttons: Save (writes to disk), Cancel (close without saving), Reload (read from disk)
  - Line numbers, cursor navigation, basic editing (insert/delete/backspace)
- **Full Reload Sync (Phase 8.6):** Enhanced `/carpetplayers reload`
  - Hash-based file change detection (only reloads modified files)
  - Datapack reload integration
  - Reports which files were reloaded
- **BYOK + OrcaRouter Provider:** New AI provider option
  - OrcaRouter: baseUrl `https://api.orcarouter.ai/v1`, model `orcarouter/auto`
  - `/carpetplayers ai provider orcarouter <apikey>` — set OrcaRouter API key
  - OpenRouter simplified to single model: `openrouter/auto` (was multi-model)
- **Kit System:** Expanded from hardcoded kits to configurable system
  - Commands: `/carpetplayers kitp <menu|kit1|kit2|kit3>`, `/carpetplayers kitb <menu|kit1|kit2|kit3>`
  - Aliases: `/cps` and `/cp` shortcuts
  - Double Chest GUI with configurable kit buttons
  - `/carpetplayers kitp save <namakit/kit1/kit2/kit3>` — save current inventory as a kit
  - Custom kit names: `/carpetplayers kitp "Crystal Kit"`

### Changed
- Config file editor moved from `/carpetplayers ai config` → `/carpetplayers config file` (matches Plan.txt spec)
- OpenRouter provider now uses single model `openrouter/auto` instead of multiple models
- `ai status` command now displays Mode: OFFLINE or ONLINE indicator
- Reload command now uses hash comparison instead of unconditional reload

### Fixed
- CodeEditorScreen.java rewritten for Minecraft 1.16.5 Mojang mappings (`GuiGraphics` → `PoseStack`, `Formatting` → `ChatFormatting`, `Component` → `TextComponent`, etc.)
- Vec3i private field access (`.x`/`.y`/`.z` → `.getX()`/`.getY()`/`.getZ()`) in MinecraftToolManager
- `getRegistryName()` replaced with `Registry.BLOCK.getKey()` in MinecraftToolManager
- `bot.onGround` protected access replaced with `bot.getDeltaMovement().y == 0.0D` in PvPBot
- `CommandSource` import path fixed in BotManager

---

## v1.3.0 — Waypoint System (Released 2026-08-22)

### Added
- **Waypoint System (Phase 8):** Full per-player waypoint management with persistent JSON storage (`carpetplayers-waypoints/<uuid>.json`).
  - Commands: `add`, `remove`, `list`, `color`, `enable`, `disable`, `tp`, `here`
  - 16 color options: white, gold, yellow, aqua, red, light_purple, blue, green, gray, dark_gray, dark_aqua, dark_red, dark_purple, dark_blue, dark_green, black
  - Tab-completion for all waypoint subcommands, names, and colors
- **Death Waypoint:** Automatic death location markers. On death: creates "Death" waypoint. On next death: renames previous to "Old Death" and creates new "Death". On 3rd death: removes "Old Death".
  - Fabric: `ClientPlayerMixin` detects `LocalPlayer.die()` + sends packet to server
  - Paper: `PlayerDeathEvent` listener with same logic
- **HUD Overlay (Fabric only):** `WaypointRenderer` renders colored indicators on-screen with:
  - Colored dot + waypoint name
  - Distance in blocks
  - Direction arrow (N/S/E/W/NW/NE/SW/SE)
  - Sorted by distance (nearest first)
- **Keybind (Fabric only):** Press **K** to open Carpet Players Menu (configurable in Controls)
- **Command Aliases:** `/cp` and `/cps` now work as shortcuts for `/carpetplayers` on all platforms
- **Config Options:** `deathWaypointEnabled` (default: true), `maxWaypoints` (default: 50), `waypointHudEnabled` (default: true)

### Changed
- TitleScreenMixin button repositioned to `centerX-100, height/4+172`
- PauseScreenMixin button repositioned to `centerX+104, height/4+96`
- TitleScreen button now uses `PoseStack` instead of deprecated `MatrixStack`
- AI error spam throttled to 60-second cooldown between repeated errors
- Spawn command now requires bot name; count is optional (default: 1)

### Fixed
- `ServerNetworking` buffer crash on packet decode
- PvP targeting bug — bots now properly track and chase targets
- AI model auto-discovery with `pickBestModel()` fallback
- TitleScreenMixin `@Shadow` crash on missing field
- "Cannot send packets" error from TitleScreen button (client-side guard added)

---

## v1.2.0 — Advanced Features (Released 2026-08-21)

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

## v1.1.0 — Client UI + Advanced Features (Released 2026-08-20)

### Added
- **Client-side GUI (Phase 3):** `CarpetPlayersScreen` with spawn/remove/kit/control/settings panels
- **Title Screen button:** "Carpet Players" button via `TitleScreenMixin`
- **Pause Screen button:** "Carpet Players" button via `PauseScreenMixin`
- **Menu command:** `/carpetplayers menu` opens GUI from in-game
- **Network packets:** ModPackets, ServerNetworking for client-server communication
- **Real-time bot status display** in GUI
- **One-click kit application** from UI

---

## v1.0.0 — Initial Release (Released 2026-08-19)

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