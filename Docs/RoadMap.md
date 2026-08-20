# CarpetPlayers — Development Roadmap

CarpetPlayers is a Minecraft mod/plugin project that brings fully autonomous, AI-powered bots to your server. Bots can follow players, wander, fight in PvP, eat, and respond to chat commands — all driven by a pluggable multi-provider AI system (OpenAI, Gemini, OpenRouter, Groq, and local Ollama). This roadmap tracks the project's progress from core foundation through community polish.

---

## Phase 1: Core Foundation (COMPLETED)

- [x] Bot spawning system (normal + PvP bots)
- [x] Bot AI brain (follow, wander, PvP, chill, eat states)
- [x] 15 AI tools for bot control
- [x] Multi-provider AI system (OpenAI, Gemini, OpenRouter, Groq, Local)
- [x] PvP combat system (weapon switching, W/A/S/D tap, multiple weapons)
- [x] Kit system (6 PvP kits with full enchants)
- [x] Chat command system (!botname command)
- [x] Bot control mode (player mirrors movement to bot)
- [x] Defensive AI (reacts to attacks)
- [x] Config system (JSON-based)
- [x] Multi-platform: Fabric 1.16.5 mod + Paper 1.16.5 plugin + Paper 1.21.11 plugin

## Phase 2: Command Execution & AI Integration (COMPLETED)

- [x] run_command AI tool — bots can execute server commands
- [x] Improved AI system prompt with step-by-step instructions
- [x] ExecutorService thread pool for AI (replaced per-call threads)
- [x] Server type detection (singleplayer vs multiplayer)
- [x] Graceful shutdown hooks

## Phase 3: User Interface (COMPLETED)

- [x] Client-side GUI screens (Fabric mod) — CarpetPlayersScreen with spawn/remove/kit/control/settings
- [x] ESC Menu integration — "Carpet Players" button via PauseScreenMixin
- [x] Main Menu integration — mod showcase button via TitleScreenMixin
- [x] /carpetplayers menu command — opens UI from in-game
- [x] UI Controller panel — visual bot management with real-time status
- [x] Real-time bot status display
- [x] One-click kit application from UI
- [x] Network packets (ModPackets, ServerNetworking, CarpetPlayersClient)

## Phase 4: Advanced Features (COMPLETED)

- [x] Bot rank system (Admin, Moderator, User roles) — all 3 platforms
- [x] WorldEdit-style AI tools (set_blocks, replace_blocks, copy_region, paste_region) — all 3 platforms
- [x] Source code reading tool (read_file) — all 3 platforms
- [x] Group commands (group_command) — all 3 platforms
- [x] Persistent bot configurations (survive server restart) — Fabric

## Phase 5: Enhanced Features (COMPLETED)

- [x] Real-Sync In-Game — /reload hot-syncs configs, AI providers, ranks, bot persistence
- [x] A* Pathfinding — bots navigate around obstacles using A* algorithm
- [x] Recording & Playback — record bot actions, replay them, save/load recordings
- [x] AI Credit Optimization — token usage tracking + per-action credit budget (1-5 credits)
- [x] Updated README.md — AI-generated disclosure, Groq recommendation, full feature list
- [x] Changelog (Docs/CHANGE.md) — version history tracking

## Phase 6: Full WorldEdit Integration (PLANNED)

- [ ] Full WorldEdit 1.16.X feature set (//set, //replace, //copy, //paste, //wand, //expand, //contract, //stack, //rotate, //flip, //fill, //drain, //butcher, etc.)
- [ ] WorldEdit selection system (pos1/pos2, //wand tool)
- [ ] History/undo system (//undo, //redo)

## Phase 7: Polish & Community (PLANNED)

- [ ] Custom AI model training integration
- [ ] In-game configuration GUI
- [ ] Permission system refinement
- [ ] Multi-language support
- [ ] Performance optimization for large bot counts
- [ ] Plugin API for third-party extensions
- [ ] Wiki and community documentation
- [ ] Modrinth/CurseForge publishing

---

## Version History

### v1.0.0 — Initial Release
Initial release with core features: bot spawning (normal + PvP), full AI brain with 5 behavior states, multi-provider AI integration, PvP combat system with weapon switching and W/A/S/D tap mechanics, 6 PvP kits with enchants, chat command system, bot control mode, defensive AI, and JSON-based configuration. Available on Fabric 1.16.5 (mod), Paper 1.16.5 (plugin), and Paper 1.21.11 (plugin).

### v1.1.0 — Client UI + Advanced Features
- **Phase 3 (UI):** Client-side GUI with CarpetPlayersScreen, Title/Pause screen mixins, /carpetplayers menu command, real-time bot status, one-click kit application.
- **Phase 4.1 (Rank System):** Admin/Moderator/User rank system with persistent JSON config, rank-based bot limits, /carpetplayers rank commands. All 3 platforms.
- **Phase 4.2 (AI Tools):** 6 new AI tools across all platforms — set_blocks, replace_blocks, copy_region, paste_region, read_file, group_command. Plus run_command ported to Paper 1.16.5.
- **Phase 4.3 (Persistence):** Bot configurations survive server restart (Fabric). Auto-save every 5 minutes + on shutdown.

### v1.2.0 — Enhanced Features (Unreleased)
- **Phase 5B (Real-Sync):** /reload hot-syncs all configs (ModConfig, AI providers, ranks, bot persistence) without restart. New `/carpetplayers reload` command.
- **Phase 5C (Recording):** Record & playback bot actions. Save/load recordings as JSON. Commands: `record start/stop/save/load/play/list`.
- **Phase 5D (A* Pathfinding):** A* pathfinding for bots — navigate around obstacles. New `navigate_to` AI tool.
- **Phase 5E (AI Credits):** Token usage tracking from OpenAI/Gemini APIs + per-action credit budget (configurable 1-5 credits). Budget exceeded → graceful stop.
- **Phase 5A (Docs):** Updated README.md with AI-generated disclosure, Groq recommendation. New CHANGE.md changelog.
