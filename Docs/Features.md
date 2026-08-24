# CarpetPlayers — Complete Feature List

CarpetPlayers is a powerful Minecraft mod/plugin that spawns AI-powered bots with full autonomy. Bots can follow you around, wander and mine, fight in PvP with advanced combat mechanics, respond to chat commands, and even execute server commands — all driven by a pluggable AI system that supports multiple providers (OpenAI, Google Gemini, OpenRouter, Groq, and local Ollama). This document is the complete reference for every feature in the project, organized by system.

---

## 1. Bot System

### 1.1 Bot Spawning

- **Normal bots:** `/carpetplayers spawn <count>` — spawns 1–100 bots at once, capped at a configurable maximum (default 50).
- **PvP bots:** `/carpetplayers pvp spawn <count>` — spawns combat-ready bots auto-equipped with full netherite gear.
- **Bot naming:** Bots are automatically named `FriendBot_N` (e.g., `FriendBot_1`, `FriendBot_2`).
- **Bot lifecycle:** Bots are automatically cleaned up when they die; all bots are managed per server tick for smooth, predictable behavior.

### 1.2 Bot States

- **FOLLOW** — Follows the owner player, maintaining a consistent 3-block distance while pathing around obstacles.
- **WANDER** — Explores the nearby area within a configurable radius, mining blocks automatically as it travels.
- **PVP** — Searches for nearby players, engages in combat, and uses items strategically (weapons, potions, bows, food).
- **CHILL** — Stays still in an idle mode, waiting for instructions.
- **EAT** — Focuses entirely on eating until the food level is sufficient.

### 1.3 Bot Brain (AI Logic)

The bot brain runs a decision loop every tick:

- **Per-tick decision making** — evaluates the environment and chooses the best action.
- **Auto-eating** when food is low (below 15 hunger).
- **Auto-drinking potions** when health drops below 50%.
- **Auto-drinking milk** to clear harmful effects (poison, wither, etc.).
- **Bow charge and release timing** — precise 18-tick charge cycle for maximum arrow damage.
- **Throwable item usage** — throws snowballs, eggs, and splash potions at targets 4–14 blocks away.
- **Mining nearby blocks** in wander mode.
- **Hazard avoidance** — detects and steers clear of lava and fire.
- **Chat interaction** — sends random ambient messages and responds to player commands.

---

## 2. PvP Combat System

### 2.1 Combat Mechanics

- **Target acquisition** within a configurable radius (default 16 blocks).
- **Melee attack** with cooldown management (8-tick attack cooldown for optimal DPS).
- **Distance-based movement** — approaches targets when further than 3 blocks, attacks when close.
- **Weapon scoring system** — automatically selects the best weapon for the situation based on distance and context.

### 2.2 W/A/S/D Tap System

Advanced strafe-and-hit technique that mimics skilled player combat:

- **W-tap:** Forward direction tap-hit — resets sprint attack cooldown mid-combo.
- **A-tap:** Left strafe tap-hit — keeps the target on your right side.
- **S-tap:** Backward tap-hit — bait-and-reset combo technique.
- **D-tap:** Right strafe tap-hit — keeps the target on your left side.
- Each tap type can be individually enabled/disabled via the configuration file.

### 2.3 Multi-Weapon System

Automatic weapon switching based on combat context, using priority scoring:

| Priority | Condition | Weapon |
|---|---|---|
| 100 | Shield when hurt | Shield |
| 95 | Bow/Crossbow when hurt and >3 blocks | Bow / Crossbow |
| 90 | Ranged weapons at >8 blocks | Bow, Crossbow |
| 80 | Sword at <3 blocks | Sword |
| 60 | Axe at <3 blocks | Axe |
| — | Distance-based fallback scoring | Any held weapon |

### 2.4 PvPBot Class

- Extends the base `BotBrain` with enhanced combat behavior.
- **Random strafing** when hurt (10-tick strafe in a random direction).
- **Auto-potion usage** in combat (splash healing, harming, etc.).
- **Default loadout:** Full netherite armor + netherite sword + bow + golden apple + splash potion + arrows.

---

## 3. Kit System

### 3.1 Available Kits

Six PvP kits with full enchantments (Protection IV + Unbreaking III + Mending on armor; Sharpness V + Unbreaking III + Mending + Looting III on swords):

| Kit | Armor | Weapon | Special Items |
|---|---|---|---|
| netherite_crystal | Netherite Prot4+UB3+Mend | Netherite Sharp5+UB3+Mend+Loot3 | 3 Totems, 3 End Crystals, 64 Obsidian, 16 Ender Pearls, 8 XP Bottles |
| diamond_crystal | Diamond Prot4+UB3+Mend | Diamond Sharp5+UB3+Mend+Loot3 | 3 Totems, 3 End Crystals, 64 Obsidian, 16 Ender Pearls, 8 XP Bottles |
| netherite_pot | Netherite Prot4+UB3+Mend | Netherite Sharp5+UB3+Mend | 3 Totems, 8 Golden Apples, 64 Cooked Beef, 16 Splash Healing II |
| diamond_pot | Diamond Prot4+UB3+Mend | Diamond Sharp5+UB3+Mend | 3 Totems, 8 Golden Apples, 64 Cooked Beef, 16 Splash Healing II |
| netherite_basic | Netherite Prot4+UB3+Mend | Netherite Sharp5+UB3+Mend | 3 Totems, 8 Golden Apples, 64 Cooked Beef |
| diamond_basic | Diamond Prot4+UB3+Mend | Diamond Sharp5+UB3+Mend | 3 Totems, 8 Golden Apples, 64 Cooked Beef |

### 3.2 Command

```
/carpetplayers kit <botname> <kitname>
```

Also available as an AI tool: `equip_kit` — the AI can equip a kit onto a bot on demand.

---

## 4. AI System

### 4.1 AI Providers

| Provider | Models | Endpoint |
|---|---|---|
| **OpenAI** | GPT models | api.openai.com |
| **Google Gemini** | Gemini models | generativelanguage.googleapis.com |
| **OpenRouter** | Multiple models | openrouter.ai |
| **Groq** | Fast inference models | api.groq.com |
| **Local (Ollama)** | Local models | localhost:11434 |

### 4.2 Provider Features

- **OrcaRouter support** — Use `https://api.orcarouter.ai/v1` with `orcarouter/auto` model
- **OpenRouter simplified** — Single model `openrouter/auto` instead of multiple models
- **Priority-based failover** — providers are tried in configured priority order until one succeeds.
- **Automatic cooldown on failure** — a failed provider is skipped for 30 seconds before retry.
- **Health tracking per provider** — tracks success/failure history for each provider.
- **Async testing of all providers** — `/carpetplayers ai test` verifies connectivity in parallel.
- **Configurable system prompt** — full control over the AI's behavior instructions.

### 4.3 AI Tools (21 total)

1. **get_state** — Get bot position, health, food, hand item, and current state.
2. **move** — Move in a direction for N ticks (forward / back / left / right).
3. **jump** — Jump for N ticks.
4. **sneak** — Toggle sneaking for N ticks.
5. **look_at** — Point the bot's gaze at specific coordinates.
6. **attack** — Attack a named player.
7. **eat** — Eat food.
8. **chat** — Say a message in chat.
9. **stop** — Stop all current actions.
10. **set_state** — Change the bot's state (follow, wander, pvp, chill, eat).
11. **mine_block** — Mine a block at coordinates or the nearest matching block.
12. **use_item** — Use the item currently held in hand.
13. **drop_item** — Drop item(s) from hand.
14. **equip_kit** — Equip a PvP kit onto the bot.
15. **run_command** — Execute a server command as the bot.
16. **set_blocks** — WorldEdit //set equivalent (set rectangular region to block type).
17. **replace_blocks** — WorldEdit //replace equivalent (replace block types in region).
18. **copy_region** / **paste_region** — clipboard-based block copy/paste.
19. **read_file** — Read source files for AI context.
20. **group_command** — Send commands to multiple bots at once.
21. **navigate_to** — A* pathfinding to target coordinates.

### 4.4 AI Commands

| Command | Description |
|---|---|
| `/carpetplayers ai start` | Enable AI |
| `/carpetplayers ai stop` | Disable AI |
| `/carpetplayers ai status` | Show provider health + Mode: OFFLINE/ONLINE |
| `/carpetplayers ai offline <true|false>` | Toggle offline mode (fallback behavior when AI is down) |
| `/carpetplayers ai reload` | Reload provider config |
| `/carpetplayers ai test` | Test all providers |
| `/carpetplayers ai act <bot> <instruction>` | AI-controlled action |
| `/carpetplayers ai chat <bool>` | Toggle AI chat |
| `/carpetplayers ai forget <bot>` | Clear conversation memory |
| `/carpetplayers ai defensive <bool>` | Toggle defensive AI |
| `/carpetplayers ai provider <openai\|gemini\|openrouter\|groq\|orcarouter\|local> <apikey>` | Set provider API key |
| `/carpetplayers config file <filename>` | Open config editor (Fabric) |

### 4.5 Defensive AI

When enabled, if a bot is attacked by a player:

1. The bot automatically switches to the PVP state.
2. AI chat is triggered with the attack context (who attacked, when, where).
3. The AI decides how to respond: fight back, flee, or ask for help.

---

## 5. Bot Control Mode

Take direct control of any bot:

- `/carpetplayers control <botname>` — take direct control of the bot.
- The player's look direction is mirrored to the bot.
- The player's movement input is translated to bot movement.
- Attack triggers fire when the player's attack strength drops (smooth swing detection).
- Multi-weapon management stays active while controlling.
- `/carpetplayers release` — release control back to normal operation.

---

## 6. Chat Command System

When interactive mode is enabled, bots respond to chat messages:

- Mention the bot name followed by a command word (e.g., `FriendBot_1 follow`).
- Or use the `bot <command>` prefix to command any bot.
- Available commands: `follow`, `stop`, `pvp`/`fight`, `chill`, `wander`, `eat`, `menu`.

---

## 7. Waypoint System

### 7.1 Waypoint Management

Per-player waypoints with persistent JSON storage at `carpetplayers-waypoints/<uuid>.json`:

| Command | Description |
|---|---|
| `/cp waypoint add <name> [color]` | Add waypoint at current position |
| `/cp waypoint remove <name>` | Remove a waypoint |
| `/cp waypoint list` | List all waypoints with index, color, status |
| `/cp waypoint color <name> <color>` | Change waypoint color |
| `/cp waypoint enable <name>` | Enable a waypoint |
| `/cp waypoint disable <name>` | Disable a waypoint |
| `/cp waypoint tp <name>` | Teleport to waypoint |
| `/cp waypoint here <name>` | Show waypoint location in chat |

**Aliases:** `/cp`, `/cps` work on all platforms.

### 7.2 Color Options

16 Minecraft color options: `white`, `gold`, `yellow`, `aqua`, `red`, `light_purple`, `blue`, `green`, `gray`, `dark_gray`, `dark_aqua`, `dark_red`, `dark_purple`, `dark_blue`, `dark_green`, `black`

### 7.3 Death Waypoint

Automatic death location tracking:

1. **1st death:** "Death" waypoint created at death location.
2. **2nd death:** Previous "Death" renamed to "Old Death", new "Death" created.
3. **3rd death:** "Old Death" removed, new "Death" created.

Toggle via config: `deathWaypointEnabled = true`

### 7.4 HUD Overlay (Fabric Only)

`WaypointRenderer` displays all enabled waypoints on-screen:
- Colored dot + waypoint name
- Distance in blocks
- Direction arrow (N/S/E/W/NW/NE/SW/SE)
- Sorted by distance (nearest first)
- Toggle via config: `waypointHudEnabled = true`

### 7.5 Keybind (Fabric Only)

Press **K** to open the Carpet Players Menu (configurable in Controls > Carpet Players).

---

## 8. Configuration

### 8.1 Mod/Plugin Config

- **Fabric:** `config/carpetplayers-config.json`
- **Paper:** `plugins/CarpetPlayers/carpetplayers-config.json`

| Option | Default | Description |
|---|---|---|
| useItemEnabled | true | Bots auto-use items |
| interactiveEnabled | true | Chat commands enabled |
| multiWeaponEnabled | true | Auto weapon switching |
| tapWEnabled | false | W-tap combat |
| tapAEnabled | false | A-tap combat |
| tapSEnabled | false | S-tap combat |
| tapDEnabled | false | D-tap combat |
| maxBots | 50 | Maximum concurrent bots |
| wanderRadius | 16 | Wander exploration radius |
| pvpTargetRadius | 16 | PvP target detection radius |
| baseTargetRadius | 8 | Normal target detection radius |
| debugLogging | true | Debug output |
| deathWaypointEnabled | true | Auto death waypoints |
| maxWaypoints | 50 | Max waypoints per player |
| waypointHudEnabled | true | HUD waypoint display |

### 8.2 AI Provider Config

- **Fabric:** `config/minecraft-ai/providers.json`
- **Paper:** `plugins/CarpetPlayers/minecraft-ai/providers.json`

Provider settings per entry: `name`, `type`, `apiKey`, `baseUrl`, `model`, `models`, `priority`, `enabled`, `timeoutMs`.

---

## 9. Commands Reference

Full command tree (all under `/carpetplayers`, aliases `/cp` and `/cps`):

```
/carpetplayers spawn <count>                    — Spawn normal bots
/carpetplayers pvp spawn <count>                — Spawn PvP bots
/carpetplayers pvp <w-tap|a-tap|s-tap|d-tap> <true|false>
/carpetplayers pvp multipleweapons <true|false>
/carpetplayers kit <botname> <kitname>          — Equip a kit on a bot
/carpetplayers control <botname>                — Take direct control of a bot
/carpetplayers release                          — Release bot control
/carpetplayers menu                             — Open the UI menu (Fabric)
/carpetplayers waypoint <subcommand>            — Waypoint management
/carpetplayers ai start | stop | status | reload | test
/carpetplayers ai act <bot> <instruction>
/carpetplayers ai chat <bool>                   — Toggle AI chat
/carpetplayers ai forget <bot>                  — Clear conversation memory
/carpetplayers ai defensive <bool>              — Toggle defensive AI
/carpetplayers ai provider <type> <apikey>      — Set provider API key
/carpetplayers rank set <player> <rank>         — Set player rank
/carpetplayers rank list | remove | default
```

*Note: The Paper 1.21.11 plugin additionally exposes a `/carpetplayers protocol` command for protocol-level management.*

---

## 10. Multi-Platform Support

### 10.1 Fabric 1.16.5 Mod

- Uses the Carpet Mod's `EntityPlayerMPFake` for fake player entities.
- Client UI with GUI screens, Title/Pause screen mixins, keybind K.
- HUD overlay for waypoints with colored indicators and direction arrows.
- Mixins: `LivingEntityMixin`, `ServerGamePacketListenerImplMixin`, `TitleScreenMixin`, `PauseScreenMixin`, `ClientPlayerMixin`.

### 10.2 Paper 1.16.5 Plugin

- Custom `FakePlayer` extending `ServerPlayer`.
- CraftBukkit integration for full plugin API access.
- ViaVersion support for legacy clients.
- Indonesian language for player messages.

### 10.3 Paper 1.21.11 Plugin

- Mojang-mapped NMS (no more SRG remapping headaches).
- Custom `FakePlayer` with an embedded connection.
- ViaVersion + ViaBackwards support for cross-version clients.
- Additional `/carpetplayers protocol` command.
- English language for player messages.
