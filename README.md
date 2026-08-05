<div align="center">

<img src="images/reactive.png" alt="Reactive" width="200">

# Reactive

[![AGPLv3 License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?&logo=github)](LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/rizer001/Reactive/build.yml?branch=main&event=push&logo=github)](https://github.com/rizer001/Reactive/actions)
[![GitHub release](https://img.shields.io/github/v/release/rizer001/Reactive?include_prereleases&logo=github)](https://github.com/rizer001/Reactive/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-success?logo=minecraft&logoColor=white)](https://github.com/rizer001/Reactive)
[![Stars](https://img.shields.io/github/stars/rizer001/Reactive?logo=github)](https://github.com/rizer001/Reactive/stargazers)
[![GitHub commits since latest release](https://img.shields.io/github/commits-since/rizer001/Reactive/latest?logo=git)](https://github.com/rizer001/Reactive/commits/main)
[![Development status](https://img.shields.io/badge/status-In_development-yellow)](https://github.com/rizer001/Reactive)

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

### 🧩 Companion Plugin — UltimateImprovments

[**UltimateImprovments**](https://github.com/rizer001/UltimateImprovments) (UI) is an addon plugin designed specifically for Reactive.
It provides authentication (Custom Screen dialogs), anti-cheat, player management commands,
and other server-side features that complement the core server.

Adds the `/ui` command for server management. See the project's
[README](https://github.com/rizer001/UltimateImprovments) for full documentation.

### 🔒 EULA Auto-Accept

Reactive automatically accepts the EULA on your behalf on first launch. This is recorded in the database and does not require manually editing `eula.txt`.

### 📊 MSPT Server Load Alerts

Reactive can automatically warn players (with the `reactive.alerts` permission) when the server's MSPT (Milliseconds Per Tick) exceeds configured thresholds. This helps server administrators detect performance issues in real time.

Configured in `config/reactive-config.yml`:

```yaml
reactive:
  mspt-alert:
    # Enable/disable the alert system entirely
    enabled: true
    # MSPT threshold for a yellow "⚡ High server load" warning
    warning-threshold: 40.0
    # MSPT threshold for a red "⚠ Server Overloaded" critical alert
    critical-threshold: 50.0
    # Minimum seconds between repeated alerts to prevent spam
    cooldown-seconds: 10
    # Permission node required to receive alerts
    permission: reactive.alerts
    # How often to check MSPT (in ticks; 20 ticks = 1 second)
    check-interval-ticks: 20
```

**Alert levels:**
| MSPT | Message | Color |
|------|---------|-------|
| > 50 ms | `⚠ Server Overloaded! MSPT: X ms` | Red |
| > 40 ms | `⚡ High server load! MSPT: X ms` | Gold |

- Players need the `reactive.alerts` permission to see warnings
- A 10-second cooldown prevents message spam during sustained load
- The permission and cooldown are fully configurable

### 💾 RAM Usage Alerts

Reactive can automatically warn players (with the `reactive.alerts` permission) when the server's JVM memory usage exceeds configured thresholds. This helps administrators detect memory leaks or insufficient RAM allocation.

Configured in `config/reactive-config.yml`:

```yaml
reactive:
  ram-alert:
    # Enable/disable the alert system entirely
    enabled: true
    # RAM usage % threshold for a gold "⚡ High RAM usage" warning
    warning-threshold: 80.0
    # RAM usage % threshold for a red "⚠ Critical RAM usage" alert
    critical-threshold: 90.0
    # Minimum seconds between repeated alerts to prevent spam
    cooldown-seconds: 10
    # Permission node required to receive alerts
    permission: reactive.alerts
    # How often to check RAM usage (in ticks; 20 ticks = 1 second)
    check-interval-ticks: 20
```

**Alert levels:**
| RAM Usage | Message | Color |
|-----------|---------|-------|
| > 90% | `⚠ Critical RAM usage! X.XGB / X.XGB (XX%)` | Red |
| > 80% | `⚡ High RAM usage! X.XGB / X.XGB (XX%)` | Gold |

- Shows actual used vs max GB alongside the percentage
- Players need the `reactive.alerts` permission to see warnings
- 10-second cooldown prevents spam during sustained high usage

### 🧟 Entity Tick Limiter

Reactive can automatically detect entity types that consume excessive tick time and **pause their ticking** until the server recovers. This prevents a single laggy entity type (e.g., a mob farm with thousands of zombies) from degrading the entire server experience.

Configured in `config/reactive-config.yml`:

```yaml
reactive:
  entity-tick-limiter:
    # Enable/disable the entity tick limiter entirely
    enabled: true
    # MSPT threshold to activate tick limiting (server is overloaded)
    threshold-mspt: 50.0
    # Minimum % of total entity tick time for a type to be paused.
    # E.g., 20.0 means an entity type must consume at least 20%
    # of all entity tick time to be paused.
    min-contribution-percent: 20.0
    # Minimum seconds between repeated announcements
    cooldown-seconds: 10
    # Permission node required to receive announcements
    permission: reactive.alerts
    # How often to check entity tick times (in ticks; 20 = 1 second)
    check-interval-ticks: 20
```

**How it works:**
| Trigger | Action |
|---------|--------|
| MSPT > threshold (50ms by default) | Reactive analyzes per-entity-type tick time consumption |
| Entity type contributes > min-contribution-percent | That entity type's `tick()` is **skipped** until the server recovers |
| MSPT drops below threshold | All paused entity types resume normal ticking |

- Players with `reactive.alerts` permission receive announcements when entity types are paused
- A 10-second cooldown prevents message spam during sustained load
- Once the server recovers, all entity types are automatically unpaused
- **Completely safe** — entity state is preserved, only `tick()` is temporarily bypassed

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

# Build the server (produces the runnable JAR)
./gradlew build
```

The built JAR will be at `reactive-server/build/libs/reactive-server-<version>.jar`.  
You can also find a paperclip-style executable at `reactive-server/build/libs/reactive-paperclip-<version>.jar`.

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

---

## 🚧 Development Status

**Development status: 🟡 In development.** Reactive is currently in active development. New features, improvements, and bug fixes are being added regularly.

Planned additions include:
- ✅ Extended world height (up to 2048 blocks)
- ✅ X/Z extension to 67 million blocks
- 🔄 **More performance optimizations** — further improvements to chunk and entity processing
- 🔄 **Enhanced configuration options** — even more fine-grained control over server behavior
- 🔄 **Deeper UltimateImprovments integration** — seamless compatibility with the companion plugin
- 🔄 **New APIs** — additional hooks for plugin developers

Stay tuned for updates! Follow the [Releases page](https://github.com/rizer001/Reactive/releases) to be notified of new builds.
<!-- webhook test -->
