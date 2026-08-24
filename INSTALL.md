# CarpetPlayers — Installation Guide

CarpetPlayers is a Minecraft mod/plugin that brings fully autonomous, AI-powered bots to your server. Bots can follow players, wander, fight in PvP, eat, and respond to chat commands — all driven by a pluggable multi-provider AI system (OpenAI, Gemini, OpenRouter, Groq, OrcaRouter, and local Ollama).

---

## 📦 System Requirements

| Platform | Requirement |
|---|---|
| **Fabric Mod** | Minecraft 1.16.5 + Fabric Loader + Carpet Mod (required dependency) |
| **Paper 1.16.5** | Paper 1.16.5 server |
| **Paper 1.21.11** | Paper 1.21.11 server |

---

## 🚀 Quick Start

### Option A: Fabric Mod (1.16.5)

1. **Install Minecraft 1.16.5** with Fabric Loader
2. **Install Carpet Mod** (required dependency — download from [CurseForge](https://www.curseforge.com/minecraft/mcarpet/carpet-minecraft))
3. **Place the mod jar** in your `mods/` folder:
   ```
   .minecraft/mods/carpetplayers-1.0.0.jar
   ```
4. **Launch the game** — commands available with op level 2

### Option B: Paper Plugin (1.16.5 or 1.21.11)

1. **Install Paper** server (download from [PaperMC](https://papermc.io/))
2. **Place the plugin jar** in your `plugins/` folder:
   ```
   plugins/CarpetPlayers/carpetplayers-plugin-1.0.0.jar
   ```
3. **Restart the server** — commands available with `carpetplayers.admin` permission

---

## 📋 Prerequisites

- **Java 8** (for 1.16.5) or **Java 21** (for 1.21.11)
- **Internet connection** for AI features (unless using offline mode)
- **AI API key** for AI-powered bot commands (optional — bots work without AI)

---

## 📂 Project Structure

```
CarpetPlayersMod/
├── src/                          # Fabric 1.16.5 Mod source
├── Plugins/
│   ├── src/                      # Paper 1.16.5 Plugin source
│   ├── 1.21.11/
│   │   └── src/                  # Paper 1.21.11 Plugin source
│   └── build/libs/               # Built plugin JARs
├── build/libs/                   # Built Fabric mod JAR
├── Docs/
│   ├── Features.md               # Complete feature reference
│   ├── RoadMap.md                # Development roadmap
│   ├── CHANGE.md                 # Version history
│   └── DocsCode.md               # Code documentation
├── Plan.txt                      # Master development plan
├── README.md                     # Project overview
└── INSTALL.md                    # This file
```

---

## 🎮 First Steps

After installation, open a Minecraft world and try these commands:

```bash
# Spawn a normal bot
/carpetplayers spawn 1

# Spawn a PvP bot
/carpetplayers pvp spawn 1

# List all active bots
/carpetplayers list

# Open the GUI menu (Fabric only)
/carpetplayers menu

# Set AI provider (optional)
/carpetplayers ai provider groq <your-api-key>

# Check AI status
/carpetplayers ai status
```

---

## 📚 Documentation

| Document | Description | Link |
|---|---|---|
| **Features** | Complete feature reference | [Docs/Features.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/Features.md) |
| **RoadMap** | Development roadmap | [Docs/RoadMap.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/RoadMap.md) |
| **Changelog** | Version history | [Docs/CHANGE.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/CHANGE.md) |
| **Code Docs** | Every file documented | [Docs/DocsCode.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Docs/DocsCode.md) |
| **Plan** | Master development plan | [Plan.txt](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plan.txt) |
| **README** | Project overview | [README.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/README.md) |

---

## 🔧 Building from Source

### Prerequisites
- **Gradle** (bundled with project)
- **JDK 8** (for 1.16.5) or **JDK 21** (for 1.21.11)

### Build Commands

```bash
# Build Fabric mod (root directory)
gradlew.bat build --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"

# Build Paper 1.16.5 plugin
cd Plugins
gradlew.bat build --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"

# Build Paper 1.21.11 plugin
cd Plugins/1.21.11
gradlew.bat build --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"
```

Output JARs:
- Fabric: `build/libs/carpetplayers-1.0.0.jar`
- Paper 1.16.5: `Plugins/build/libs/carpetplayers-plugin-1.0.0.jar`
- Paper 1.21.11: `Plugins/1.21.11/build/libs/carpetplayers-1.21.11.jar`

---

## ⚠️ Notes

- **CarpetPlayers is an optional enhancement mod** designed to make Minecraft singleplayer and multiplayer more fun. It adds helpful features for playing alongside AI companions — not required for normal gameplay.
- **AI-assisted development**: core architecture, feature design, and critical fixes are human-driven, while AI handles implementation scaffolding. The developer reviews all code and fixes edge cases that require real testing.
- **Offline mode**: When AI provider is unavailable, bots automatically fall back to offline behavior (defend → eat → follow → wander). Toggle with `/carpetplayers ai offline <true|false>`.
- **MIT License** — see [LICENSE](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/LICENSE) for details.

---

## 🆘 Troubleshooting

| Issue | Solution |
|---|---|
| Bots not spawning | Check maxBots config (default: 50) |
| AI not responding | Verify provider API key with `/carpetplayers ai test` |
| Config not saving | Ensure server has write permissions to config folder |
| Plugin fails to load | Check Paper version compatibility (1.16.5 or 1.21.11) |
| Fabric mod crashes | Ensure Carpet Mod is installed and compatible version |

---

*Last updated: 2026-08-24*