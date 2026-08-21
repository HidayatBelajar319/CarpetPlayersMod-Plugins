# CarpetPlayers Plugin — Paper 1.21.11

Server-side plugin for Paper 1.21.11 with ViaVersion/ViaBackwards support.

---

## What's Different from 1.16.5

- Built for Minecraft 1.21.11 (Mojang-mapped NMS)
- Includes ViaCompat for legacy client detection (1.16.5 clients via ViaBackwards)
- Custom FakePlayer implementation (extends ServerPlayer directly)
- Full protocol version display (`/carpetplayers protocol`)
- English language for all messages

---

## Features

Same feature list as the 1.16.5 plugin, plus:

- ViaVersion integration — works alongside ViaBackwards for older clients
- Protocol detection — reports each client's protocol version
- Mojang-mapped NMS — direct `net.minecraft.*` API access (see the API reference)

Core features:

- AI bots — spawn fake players driven by an AI brain
- PvP bots — combat-ready bots with tap-hit controls and multiple weapons
- 21 AI tools — WorldEdit-style building, command execution, and more
- 5 AI providers — OpenAI, Gemini, OpenRouter, Groq, and local endpoints
- 6 PvP kits — netherite/diamond variants (crystal, pot, basic)
- Waypoint system — per-player waypoints with 16 colors, death tracking, teleport
- Command aliases — `/cp`, `/cps` shortcuts
- Command execution — AI can run server commands on demand
- Defensive AI — bots can react and defend themselves
- Chat commands — talk to bots and get AI-powered replies

---

## Requirements

- Paper 1.21.11 server
- Java 21+
- Optional: ViaVersion + ViaBackwards

---

## Installation

1. Build with `./gradlew build`
2. Place jar in `plugins/` folder
3. For legacy client support, also install ViaVersion + ViaBackwards

---

## Commands

All commands require the `carpetplayers.admin` permission (aliases: `/cp`, `/cps`):

### Bot Management
- `/carpetplayers spawn <count>` — Spawn AI-free fake player bots
- `/carpetplayers pvp spawn <count>` — Spawn PvP bots with gear
- `/carpetplayers remove <name>` — Remove a bot
- `/carpetplayers list` — List all active bots
- `/carpetplayers control <name>` — Take direct control of a bot
- `/carpetplayers release` — Stop controlling the current bot
- `/carpetplayers kit <botname> <kit>` — Apply a PvP kit

### PvP Settings
- `/carpetplayers pvp <w-tap|a-tap|s-tap|d-tap> <true|false>` — Toggle tap-hit
- `/carpetplayers pvp multipleweapons <true|false>` — Multi-weapon PvP
- `/carpetplayers useitem <true|false>` — Allow bots to use items

### AI Control
- `/carpetplayers ai start` / `stop` / `status` / `reload` / `test` — AI engine control
- `/carpetplayers ai act <botname> <instruction>` — Give a bot an instruction
- `/carpetplayers ai chat <true|false>` — Toggle AI chat replies
- `/carpetplayers ai forget <botname>` — Clear bot memory
- `/carpetplayers ai defensive <true|false>` — Enable defensive AI
- `/carpetplayers ai provider <openai|gemini|openrouter|groq|local> <apikey>` — Set provider

### Waypoints
- `/carpetplayers waypoint add <name> [color]` — Add waypoint at position
- `/carpetplayers waypoint remove <name>` — Remove a waypoint
- `/carpetplayers waypoint list` — List all waypoints
- `/carpetplayers waypoint color <name> <color>` — Change waypoint color
- `/carpetplayers waypoint enable <name>` / `disable <name>` — Toggle waypoint
- `/carpetplayers waypoint tp <name>` — Teleport to waypoint
- `/carpetplayers waypoint here <name>` — Show waypoint location

### Protocol & Config
- `/carpetplayers protocol [player]` — Shows client protocol version
- `/carpetplayers rank set <player> <admin|moderator|user>` — Set player rank
- `/carpetplayers rank list` — List all ranks
- `/carpetplayers interactive <true|false>` — Toggle interactive bot mode

---

## Configuration

Same config structure as the 1.16.5 plugin:

- Config file: `plugins/CarpetPlayers/carpetplayers-config.json`
- AI providers: `plugins/CarpetPlayers/minecraft-ai/providers.json`
- Waypoints: `plugins/CarpetPlayers/waypoints/<uuid>.json`

---

## API Reference

See [API-REFERENCE.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/API-REFERENCE.md)

---

## Links

- [Full Documentation](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/Docs)
- [Features](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/Features.md)
- [Code Docs](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/DocsCode.md)

---

## License

MIT
