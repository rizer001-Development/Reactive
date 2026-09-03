# Reactive - Vanilla Minecraft Server Core

Custom Minecraft server core based on Mojang's vanilla server code (version **26.2**).

## What is Reactive?

Reactive is a **vanilla Minecraft server fork** — it patches Mojang's server jar directly with a fast, deterministic build pipeline (no 10+ minute patch stacks).

## Features

### ✅ Core
- **Vanilla 26.2 base** — clean Mojang-mapped server jar
- **Fast builds** — ~1 minute (patch + merge + verify)
- **Verified patches** — build fails if a patch silently did not land
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
├── build.gradle.kts                    # Build script (patch + merge + verify pipeline)
├── libs/
│   ├── server-26.2.jar                 # Original Mojang server (only tracked dependency)
│   └── bundled/                        # Mojang runtime libraries (local, not tracked)
├── src/main/java/
│   ├── net/minecraft/                  # Modified Mojang classes (source overrides)
│   │   ├── commands/Commands.java      # Command hook (2 lines vanilla change)
│   │   ├── server/Eula.java            # EULA always accepted
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
│       ├── patch/
│       │   ├── PatchVanilla.java       # ASM bytecode patcher (build time)
│       │   └── VerifyServerJar.java    # Post-build patch verification
│       └── server/StartMessages.java   # Entry point
└── src/main/resources/
    └── default-*.toml                  # Default config templates
```

## How It Works (NMS pipeline)

Reactive patches Mojang's server at build time. 26.2 ships Mojang-mapped
(unobfuscated) classes, so no remapping is needed — the pipeline is:

1. **Patch** (`./gradlew patchVanilla`) — one ASM pass over `libs/server-26.2.jar`
   rewrites `ServerLevel.getGameRules()` to route through
   `ReactiveGameRuleHooks`, and drops Mojang's now-invalid signature files.
   Output: `build/server-26.2-patched.jar`.
2. **Merge** (`jar`) — compiled Reactive classes (source overrides for
   `Eula`, `Settings`, `Commands`, ...) are merged over the patched jar, so the
   modified classes physically replace the vanilla ones in the final jar. No
   classpath-ordering tricks: a single self-contained `reactive-26.2-1.0.0.jar`.
3. **Verify** (`verifyServerJar`, part of `./gradlew build`) — checks the final
   jar actually contains the patched bytecode and not silently-vanilla classes,
   and that no signature entries survived. The build fails if a patch is missing.
4. **Runtime** — per-world `GameRules` live in a `ConcurrentHashMap`; values are
   persisted to `gamerules.toml` on an interval and on shutdown.

Adding a new NMS patch = add the modified `net/minecraft/...` source file
under `src/main/java` (it replaces the vanilla class at merge time), or extend
`PatchVanilla` for bytecode-only changes, and the verifier will confirm it landed.

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
| Code changes | Patch stacks (diffs) | Direct class overrides + ASM |
| Patch conflicts | Common on updates | Rare (few overrides) |
| Verification | None built in | `verifyServerJar` on every build |
| Config format | YAML | TOML (comments, sections) |

## License

Mojang's code is subject to Mojang's EULA.
Reactive is AGPLv3 licensed.
