<div align="center">

<img src="https://user-images.githubusercontent.com/74448585/150906023-101cd383-da82-4a3c-9603-a3b5741c3994.png" alt="Reactive" width="200">

# Reactive

[![AGPLv3 License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?&logo=github)](LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/rizer001/Reactive/build.yml?branch=main&event=push&logo=github)](https://github.com/rizer001/Reactive/actions)
[![GitHub release](https://img.shields.io/github/v/release/rizer001/Reactive?include_prereleases&logo=github)](https://github.com/rizer001/Reactive/releases)
[![Discord](https://img.shields.io/discord/685683385313919172?logo=discord&label=Discord)](https://purpurmc.org/discord)

**Reactive** is a high-performance fork of [Purpur](https://github.com/PurpurMC/Purpur) for Minecraft 26.2, built on top of [Paper](https://github.com/PaperMC/Paper).  
It inherits all Purpur features and adds its own improvements: extended world height, auto-configuration on first launch, integrated SQLite database, and more.

---

**Download latest build:** [GitHub Releases](https://github.com/rizer001/Reactive/releases)

---

</div>

## ✨ What is Reactive?

Reactive is a drop-in replacement for Purpur/Paper servers. Just swap your JAR file and you get:

- ✅ **All Purpur functionality** — 400+ configurable options, WASD controls, mob/item/block/AI configuration, and more
- ✅ **Paper performance** — optimized chunk engine, anti-lag systems, Moonrise patches
- ✅ **Vanilla compatibility** — all Paper/Spigot plugins work without changes
- ✅ **Reactive exclusive features** — unique improvements listed below

---

## 🚀 Reactive Features

### 📐 Extended World Height (up to 2048 blocks)

Reactive allows increasing the world build height to **2048 blocks** (vs the standard 320). Configured in `config/reactive-config.yml`:

```yaml
reactive:
  world-height:
    enabled: true
    overworld-min-y: -64
    overworld-max-y: 2048
    nether-min-y: 0
    nether-max-y: 256
    end-min-y: 0
    end-max-y: 256
```

- Implemented by intercepting the `DimensionType` CODEC before the registry freezes
- Works server-side — **no client mods or resource packs required**
- All commands (`/tp`, `/setblock`, `/fill`, etc.) use the updated limits

### 🗺️ X/Z Extension to 67 Million Blocks

Horizontal coordinates can be extended to **67,000,000 blocks** along X/Z. Enable it with:

```yaml
reactive:
  world-border:
    enabled: false
    max-xz: 67000000
```

### ⚙️ Auto-Configuration on First Launch

On first startup, Reactive automatically creates the `config/` directory and generates all necessary files:

| File | Purpose |
|------|---------|
| `config/server.properties` | Core server settings |
| `config/bukkit.yml` | Bukkit settings |
| `config/spigot.yml` | Spigot settings |
| `config/purpur.yml` | Purpur settings |
| `config/reactive-config.yml` | **Reactive settings** |

**Important:** The `config/` folder is the **single source of truth** for configuration files. If a file is missing at startup, it will be created automatically.

**`reactive-config.yml`** is the central config file for all Reactive-specific features.

> 💡 **Note:** Advanced features like player authentication, permissions, anti-cheat, custom screens (Dialogs),
> and server management commands are implemented in the companion plugin
> **[UltimateImprovments](https://github.com/rizer001/UltimateImprovments)** (aka UI / `/ui`),
> not in Reactive core. Reactive focuses on server performance and world configuration.

### 🔒 EULA Auto-Accept

Reactive automatically accepts the EULA on your behalf on first launch. This is recorded in the database and does not require manually editing `eula.txt`.

### 🗄️ SQLite Database (optional)

Built-in SQLite support for storing server data:

```yaml
reactive:
  database:
    enabled: false
    type: sqlite
    sqlite-file: reactive.db
```

### 🏷️ Branding

- **Server name:** `Reactive Server`
- **Identifier:** `rizer001:reactive`
- **Console:** displays `Reactive` instead of `Purpur`
- **JAR file:** `reactive-server.jar` when built

---

## 📦 Installation

1. **Download** the latest JAR from [Releases](https://github.com/rizer001/Reactive/releases)
2. **Replace** your current server JAR with `reactive-server.jar`
3. **Start** the server. Reactive will create the `config/` folder and all required files automatically
4. **Configure** `config/reactive-config.yml` to your needs

> **Requirements:** Java 25+ (recommended: Eclipse Adoptium Temurin-25+)

---

## 🛠️ Building from Source

### Prerequisites
- **Java 25+** (Eclipse Adoptium Temurin)
- **Git**

### Build Instructions

```bash
# Clone the repository
git clone https://github.com/rizer001/Reactive.git
cd Reactive

# Apply all patches
./gradlew applyAllPatches

# Build the server
./gradlew build

# Build the server-ready JAR
./gradlew createReactiveJar
```

The built JAR will be at `build/libs/reactive-server.jar` or `reactive-server/build/libs/reactive-server.jar`.

### Building in an IDE
After running `./gradlew applyAllPatches`, the project is ready to import into IntelliJ IDEA or Eclipse.

### Creating a Patch

1. Make changes in `paper-server/` or `reactive-server/src/minecraft/`
2. Run `./gradlew rebuildPatches`
3. Patches will appear in `reactive-server/paper-patches/` or `reactive-server/minecraft-patches/`

---

## 📋 Project Structure

```
Reactive/
├── reactive-api/          # API layer (extends Paper API)
│   └── paper-patches/     # API patches
├── reactive-server/       # Server implementation
│   ├── paper-patches/     # Paper server patches
│   │   ├── features/      # Feature patches
│   │   └── files/         # Individual file patches
│   ├── minecraft-patches/ # Minecraft code patches
│   │   ├── features/      # Feature patches (Purpur)
│   │   └── sources/       # Source file patches
│   └── src/
│       ├── main/java/     # Reactive and Purpur code
│       └── minecraft/     # Modified Minecraft code
├── paper-api/             # Generated Paper API
├── paper-server/          # Generated Paper Server
├── patches/               # Additional patches
├── build-data/            # Build data
└── gradle.properties      # Version and build settings
```

---

## 🔗 API

### Maven Repository

```kotlin
repositories {
    maven("https://repo.purpurmc.org/snapshots")
}
```

```kotlin
dependencies {
    compileOnly("org.rizer001.reactive:reactive-api:26.2.build.+")
}
```

The API includes all interfaces from Paper, Spigot, and Bukkit.

---

## 📜 License

This project is licensed under the **GNU Affero General Public License v3.0** (AGPLv3).

[![AGPLv3 License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?&logo=github)](LICENSE)

See [PaperMC/Paper](https://github.com/PaperMC/Paper) and [PurpurMC/Purpur](https://github.com/PurpurMC/Purpur) for the license of material used by this project.

---

## 🙏 Acknowledgements

- **[PurpurMC/Purpur](https://github.com/PurpurMC/Purpur)** — project foundation
- **[PaperMC/Paper](https://github.com/PaperMC/Paper)** — high-performance engine
- **[PaperMC/Paperweight](https://github.com/PaperMC/paperweight)** — build system
- **[YourKit](https://www.yourkit.com/)** — Java profiler
- **All contributors** to Purpur, Paper, and Reactive

---

<div align="center">

**[GitHub](https://github.com/rizer001/Reactive)** · **[Releases](https://github.com/rizer001/Reactive/releases)** · **[Issues](https://github.com/rizer001/Reactive/issues)**

</div>
