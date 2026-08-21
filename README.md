# CarpetPlayers Mod & Plugins

AI-powered Minecraft bot mod/plugin with PvP, command execution, WorldEdit tools, waypoints, and multi-provider AI support.

**Fabric 1.16.5 | Paper 1.16.5 | Paper 1.21.11**

> **About this project:** CarpetPlayers is developed with AI assistance (OpenAI, Claude, and others). Core architecture, feature ideas, design decisions, and many bug fixes are human-driven. AI handles implementation scaffolding and boilerplate, while the developer reviews, edits, and fixes edge cases that require hands-on testing. This hybrid approach lets us ship a full multi-platform mod faster while keeping quality high.

---

## Features

### Bot System
- **Spawn AI bots** — create fake player bots that follow you, wander, fight, or chill
- **Full AI brain** — 5 behavior states: FOLLOW, WANDER, PVP, CHILL, EAT
- **Bot control mode** — take direct control of a bot, mirror your movements
- **Defensive AI** — bots react to attacks automatically
- **Bot rank system** — Admin/Moderator/User ranks with per-rank spawn limits

### PvP Combat
- **Weapon switching** — bots automatically switch between weapons
- **W/A/S/D tap** — toggleable tap-hit mechanics for combat
- **Multiple weapons** — multi-weapon PvP system
- **6 PvP kits** — netherite_crystal, diamond_crystal, netherite_pot, diamond_pot, netherite_basic, diamond_basic

### AI Integration
- **Multi-provider AI** — OpenAI, Gemini, OpenRouter, Groq, and local Ollama
- **21 AI tools** — bots can execute commands, manipulate blocks, read files, and more
- **Groq recommended** — fastest, most interactive responses with lowest credit usage
- **1-5 credits per action** — optimized AI call budget

### WorldEdit Tools (AI-powered)
- `set_blocks` — set rectangular regions to any block type
- `replace_blocks` — replace block types in a region
- `copy_region` / `paste_region` — clipboard-based block copy/paste
- `read_file` — AI reads your source code for context
- `group_command` — send commands to multiple bots at once
- `run_command` — bots execute server commands

### Waypoint System
- **Per-player waypoints** — add, remove, teleport, list waypoints with persistent JSON storage
- **16 color options** — white, gold, yellow, aqua, red, light_purple, blue, green, gray, dark_gray, dark_aqua, dark_red, dark_purple, dark_blue, dark_green, black
- **Death waypoints** — automatic death location markers (Death -> Old Death -> removed on 3rd death)
- **HUD overlay (Fabric)** — colored indicators with distance and direction arrows on-screen
- **Keybind K (Fabric)** — press K to open the Carpet Players Menu
- **Command aliases** — `/cp`, `/cps` shortcuts for `/carpetplayers`

### Client UI (Fabric)
- **GUI menu** — visual bot management with real-time status
- **Title & Pause screen** integration
- **One-click kit application** from the GUI
- `/carpetplayers menu` command

### Server Features
- **Persistent bots** — bot configs survive server restart
- **Bot rank system** — Admin/Moderator/User with JSON config
- **Multi-platform** — Fabric mod + Paper plugins (1.16.5 & 1.21.11)

---

## AI Provider Recommendation

**Groq is recommended** for the best experience. Groq provides the most enthusiastic and interactive AI responses while using the least credits. It's the only provider that feels truly alive — your bots will respond with energy and personality.

Setup: `/carpetplayers ai provider groq <your-api-key>`

Other providers: OpenAI, Gemini, OpenRouter, or local Ollama (no API key needed).

---

## Projects

| Project | Platform | Version | Link |
|---|---|---|---|
| Fabric Mod (Primary) | Fabric + Carpet | 1.16.5 | [src/](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/src) |
| Paper Plugin | Paper/Spigot | 1.16.5 | [Plugins/](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/Plugins) |
| Paper Plugin | Paper/Spigot | 1.21.11 | [Plugins/1.21.11/](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/Plugins/1.21.11) |

---

## Documentation

- [Features](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/Features.md) — Full feature list
- [Roadmap](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/RoadMap.md) — Development roadmap
- [Changelog](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/CHANGE.md) — Version history
- [Code Documentation](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/DocsCode.md) — Every file documented

---

## Quick Start (Fabric Mod)

1. Install Minecraft 1.16.5 with Fabric Loader
2. Install Carpet Mod (required dependency)
3. Place the mod jar in `mods/` folder
4. Launch the game — commands available with op level 2

## Quick Start (Paper Plugin)

1. Install Paper 1.16.5 or Paper 1.21.11 server
2. Place the plugin jar in `plugins/` folder
3. Restart server — commands available with `carpetplayers.admin` permission

---

## Commands Overview

All commands are under the `/carpetplayers` prefix (aliases: `/cp`, `/cps`):

### Bot Management
- `/carpetplayers spawn <count>` — Spawn AI-free fake player bots
- `/carpetplayers pvp spawn <count>` — Spawn PvP bots with gear
- `/carpetplayers remove <name>` — Remove a bot
- `/carpetplayers list` — List all active bots
- `/carpetplayers control <name>` — Take direct control of a bot
- `/carpetplayers release` — Stop controlling the current bot

### PvP Settings
- `/carpetplayers pvp <w-tap|a-tap|s-tap|d-tap> <true|false>` — Toggle tap-hit control directions
- `/carpetplayers pvp multipleweapons <true|false>` — Enable multi-weapon PvP system
- `/carpetplayers kit <botname> <kit>` — Apply a PvP kit
- `/carpetplayers useitem <true|false>` — Allow bots to use items

### AI Control
- `/carpetplayers ai start` / `stop` / `status` / `reload` / `test` — AI engine control
- `/carpetplayers ai act <botname> <instruction>` — Give a bot an instruction
- `/carpetplayers ai chat <true|false>` — Toggle AI chat replies
- `/carpetplayers ai forget <botname>` — Clear bot memory
- `/carpetplayers ai defensive <true|false>` — Enable defensive AI
- `/carpetplayers ai provider <openai|gemini|openrouter|groq|local> <apikey>` — Set AI provider

### Waypoints
- `/carpetplayers waypoint add <name> [color]` — Add waypoint at current position
- `/carpetplayers waypoint remove <name>` — Remove a waypoint
- `/carpetplayers waypoint list` — List all waypoints
- `/carpetplayers waypoint color <name> <color>` — Change waypoint color
- `/carpetplayers waypoint enable <name>` / `disable <name>` — Toggle waypoint
- `/carpetplayers waypoint tp <name>` — Teleport to waypoint
- `/carpetplayers waypoint here <name>` — Show waypoint location

### UI & Config
- `/carpetplayers menu` — Open the GUI menu (Fabric)
- `/carpetplayers interactive <true|false>` — Toggle interactive bot mode
- `/carpetplayers rank set <player> <admin|moderator|user>` — Set player rank
- `/carpetplayers rank list` — List all ranks

---

## Note

CarpetPlayers is an **optional enhancement mod** designed to make Minecraft singleplayer and multiplayer more fun. It adds helpful features for playing alongside AI companions — not required for normal gameplay.

This project is **AI-assisted**: core architecture, feature design, and critical fixes are human-driven, while AI handles implementation scaffolding. The developer reviews all code and fixes edge cases that require real testing.

---

## License

MIT License
