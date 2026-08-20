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

## Phase 3: User Interface (IN PROGRESS)

- [ ] Client-side GUI screens (Fabric mod)
- [ ] ESC Menu integration — "Carpet Players" button
- [ ] Main Menu integration — mod showcase button
- [ ] /carpetplayers menu command — opens UI from in-game
- [ ] UI Controller panel — visual bot management
- [ ] Real-time bot status display
- [ ] One-click kit application from UI
- [ ] AI provider configuration from UI

## Phase 4: Advanced Features (PLANNED)

- [ ] WorldEdit-style tools (//set, //replace, //copy, //paste)
- [ ] Source code reading tool (AI can read and analyze code)
- [ ] Bot rank system (Admin, Moderator, User roles)
- [ ] Persistent bot configurations (survive server restart)
- [ ] Bot pathfinding improvements (A* navigation)
- [ ] Group commands (send commands to multiple bots)
- [ ] Recording & playback system
- [ ] Custom AI model training integration

## Phase 5: Polish & Community (PLANNED)

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
