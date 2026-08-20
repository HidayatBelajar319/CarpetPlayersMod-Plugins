# CarpetPlayers Mod &amp; Plugins

AI-powered Minecraft bot mod with PvP, command execution, WorldEdit tools, and multi-provider AI support.

Fabric 1.16.5 | Paper 1.16.5 | Paper 1.21.11

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

All commands are under the `/carpetplayers` prefix:

- `/carpetplayers spawn <count>` — Spawn AI-free fake player bots
- `/carpetplayers pvp spawn <count>` — Spawn PvP bots with gear
- `/carpetplayers pvp <w-tap|a-tap|s-tap|d-tap> <true|false>` — Toggle tap-hit control directions
- `/carpetplayers pvp multipleweapons <true|false>` — Enable multi-weapon PvP system
- `/carpetplayers ai start` / `stop` / `status` / `reload` / `test` — AI engine control
- `/carpetplayers ai act <botname> <instruction>` — Give a bot an instruction
- `/carpetplayers ai chat <true|false>` — Toggle AI chat replies
- `/carpetplayers ai forget <botname>` — Clear bot memory
- `/carpetplayers ai defensive <true|false>` — Enable defensive AI
- `/carpetplayers ai provider <openai|gemini|openrouter|groq|local> <apikey>` — Set AI provider API key
- `/carpetplayers control <name>` — Take direct control of a bot
- `/carpetplayers release` — Stop controlling the current bot
- `/carpetplayers remove <name>` — Remove a bot
- `/carpetplayers list` — List all active bots
- `/carpetplayers kit <botname> <kit>` — Apply a PvP kit (netherite_crystal, diamond_crystal, netherite_pot, diamond_pot, netherite_basic, diamond_basic)
- `/carpetplayers useitem <true|false>` — Allow bots to use items
- `/carpetplayers interactive <true|false>` — Toggle interactive bot mode
- `/carpetplayers menu` — Open the new GUI menu (new UI)

---

## License

MIT License
