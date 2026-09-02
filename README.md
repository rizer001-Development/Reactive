# Reactive - Vanilla Minecraft Server Core

Custom Minecraft server core based on Mojang's vanilla server code (version **26.2**).

## What is Reactive?

Reactive is a **vanilla Minecraft server fork** — we take Mojang's decompiled server code and modify it directly. No complex patch systems, no 10+ minute builds.

## Features

### ✅ Core
- **Vanilla 26.2 base** — clean decompiled Mojang code
- **Fast builds** — ~1 minute, no patch system
- **TOML configs** — human-readable, comments supported, error-resistant
- **ASM patching** — build-time bytecode modification (per-world game rules)

### ✅ Configuration
- `server.toml` — server settings (TOML format, sections, comments)
- `reactive.toml` — Reactive-specific settings
- `gamerules.toml` — per-world game rules (auto-saved, hot-reloadable)
- `ops.toml`, `whitelist.toml`, `banned-players.toml`, `banned-ips.toml`, `usercache.toml`

### ✅ Commands
- `/reactive help [page]` — paginated help
- `/reactive reload` — hot-reload all configs (except server.toml)
- `/reactive gamerules` — reload game rules from gamerules.toml

### ✅ Game Rules
- **Per-world game rules** — each dimension (overworld, nether, end) has independent rules
- **Auto-save** — configurable interval (default 30 min)
- **Hot-reload** — `/reactive reload` applies changes without restart
- **Shutdown save** — rules saved on server stop

## Architecture

```
Reactive/
├── build.gradle.kts                    # Build script
├── libs/
│   ├── server-26.2.jar                 # Original Mojang server
│   └── server-26.2-stripped.jar        # Unsignsed for compilation
├── src/main/java/
│   ├── net/minecraft/                  # Decompiled vanilla classes (modified)
│   │   ├── commands/Commands.java      # Command hook (2 lines vanilla change)
│   │   ├── server/Eula.java            # EULA removed
│   │   ├── server/Services.java        # usercache.toml support
│   │   ├── server/dedicated/Settings.java  # server.toml support
│   │   └── server/players/
│   │       ├── StoredUserList.java     # TOML + templates + reload
│   │       └── CachedUserNameToIdResolver.java
│   └── org/rizer001/reactive/
│       ├── command/
│       │   ├── CommandRegistry.java    # Command hook system
│       │   └── ReactiveCommands.java   # /reactive commands
│       ├── config/ReactiveConfig.java  # reactive.toml loader
│       ├── gamerules/
│       │   ├── GameRuleTomlStore.java  # gamerules.toml read/write
│       │   ├── ReactiveGameRuleHooks.java  # ASM hook target
│       │   └── ReactiveGameRuleManager.java # Lifecycle manager
│       ├── patch/PatchVanilla.java     # ASM bytecode patcher
│       └── server/StartMessages.java   # Entry point
└── src/main/resources/
    ├── default-*.toml                  # Default config templates
    └── reactive.mixins.json
```

## How It Works

1. **Build time**: `PatchVanilla` patches `ServerLevel.getGameRules()` in the vanilla jar
2. **Runtime**: Patched method calls `ReactiveGameRuleHooks.getGameRules()` instead of server-wide rules
3. **Per-world**: Each dimension gets its own `GameRules` instance from `ConcurrentHashMap`
4. **Persistence**: Rules saved to `gamerules.toml` on interval and shutdown

## Building

```bash
# Build (includes ASM patching)
./gradlew build

# Run server
./gradlew run

# Or run jar directly
java -jar build/libs/reactive-26.2-1.0.0.jar nogui
```

## Configuration

### First Run
On first start, Reactive creates all config files from templates:
- `server.toml` — server settings (edit manually, restart to apply)
- `reactive.toml` — Reactive settings (edit, `/reactive reload`)
- `gamerules.toml` — per-world game rules (edit, `/reactive reload`)

### Editing gamerules.toml
```toml
["minecraft:overworld"]
keepInventory = "true"
doDaylightCycle = "false"
randomTickSpeed = "3"

["minecraft:the_nether"]
keepInventory = "false"
```

Then run `/reactive reload` to apply.

## Comparison with Paper/Purpur

| Aspect | Paper/Purpur | Reactive |
|--------|-------------|----------|
| Build time | 10-20+ min | ~1 min |
| Code changes | Patch files (diffs) | Direct editing |
| Patch conflicts | Common on updates | None |
| Understanding | Hard (500+ patches) | Easy (readable vanilla) |
| Config format | YAML | TOML (comments, sections) |

## License

Mojang's code is subject to Mojang's EULA.
Reactive is AGPLv3 licensed.
