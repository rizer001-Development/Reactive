# Contributing to Reactive

First off, thanks for taking the time to contribute! 🎉

Reactive is a high-performance fork of [Purpur](https://github.com/PurpurMC/Purpur) / [Paper](https://github.com/PaperMC/Paper).  
All contributions — bug fixes, new features, config options, performance patches — are welcome.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Submit Changes](#how-to-submit-changes)
- [What Makes a Good Merge Request](#what-makes-a-good-merge-request)
- [Commit Guidelines](#commit-guidelines)
- [Creating a Patch](#creating-a-patch)
- [Build Instructions](#build-instructions)
- [License](#license)

---

## Code of Conduct

This project follows the [Purpur Code of Conduct](https://github.com/PurpurMC/Purpur/blob/ver/26.2/CODE_OF_CONDUCT.md).  
Be respectful, constructive, and assume good faith.

---

## How to Submit Changes

1. **Fork** the repository on GitHub.
2. **Create a branch** for your changes (`git checkout -b my-feature`).
3. **Make your changes** — see [Creating a Patch](#creating-a-patch).
4. **Build locally** to verify everything compiles (`./gradlew applyAllPatches build`).
5. **Commit** with a clear message (see [Commit Guidelines](#commit-guidelines)).
6. **Push** to your fork and open a **Merge Request** (Pull Request) against the `main` branch.

That's it. I'll review it and either merge or leave feedback.

---

## What Makes a Good Merge Request

| Do ✅ | Avoid ❌ |
|---|---|
| One logical change per MR | Bundling unrelated changes |
| Clear commit message describing **what** changed and **why** | Vague messages like "fix" or "update" |
| Build passes before submitting | Commits that break the build |
| Follow the existing code style (look at surrounding code) | Large refactors without discussion first |
| Prefer minimal, focused changes | Giant diffs that are hard to review |

### Examples of good MRs

- Adding a new config option: `Add configurable entity activation range for bees`
- Fixing a bug: `Fix daylight cycle integer division causing incorrect time`
- Performance improvement: `Optimize chunk loading by caching region headers`

I'll review every MR myself. If there's an issue, I'll tell you what to fix.

---

## Commit Guidelines

Free format, but make sure the message clearly states **what was changed**:

```
Short summary (max ~72 chars)

Optional longer description — explain why the change was made,
what problem it solves, and any relevant context.
```

**Prefixes are optional but helpful:** `feat:`, `fix:`, `refactor:`, `perf:`, `docs:`, `chore:`

Examples:

```
feat: Add configurable lighting rod range

fix: Correct night-skip percentage calculation when players are sleeping

perf: Cache entity type lookups in patch application
```

---

## Creating a Patch

Reactive uses the [Paperweight](https://github.com/PaperMC/paperweight) patch system.  
Patches are stored in `reactive-server/minecraft-patches/` and `reactive-server/paper-patches/`.

### Workflow

```bash
# 1. Apply all existing patches to get the editable source
./gradlew applyAllPatches

# 2. Make your changes in the generated source directories:
#    reactive-server/src/minecraft/java/   (Minecraft/Paper code)
#    reactive-server/src/main/java/        (Reactive-specific code)

# 3. Test your changes
./gradlew compileJava

# 4. Regenerate patches from your changes
./gradlew rebuildMinecraftSourcePatches   # for Minecraft patches
./gradlew rebuildPaperSourcePatches       # for Paper patches
# or simply:
./gradlew rebuildAllServerPatches

# 5. Build the final JAR
./gradlew build
```

### Patch naming

- Patches in `minecraft-patches/sources/` are auto-numbered.  
  The `rebuildPatches` task handles this automatically.
- Always include a comment with your change description in the patch header, e.g.:
  ```java
  // Reactive - Add configurable entity tick limiter
  ```

### Where to put your code

| Type | Location |
|---|---|
| Minecraft server patches | `reactive-server/minecraft-patches/sources/` |
| Server Java source | `reactive-server/src/main/java/org/purpurmc/reactive/` |
| Config values | `ReactiveConfig.java` + `reactive-config.yml` default |
| API changes | `reactive-api/src/main/java/` |

---

## Build Instructions

### Prerequisites

- **Java 25+** (recommended: Eclipse Adoptium Temurin)
- **Git**

### Commands

```bash
# Full build (applies patches + compiles)
./gradlew applyAllPatches
./gradlew build

# Quick compilation check (after patches are applied)
./gradlew :reactive-server:compileJava

# Create a deployable JAR
./gradlew createReactiveJar
```

The built JAR will be at `build/libs/reactive-server.jar`.

### CI

Every push and pull request is automatically built on GitHub Actions.  
The build must pass before merging.

---

## License

By contributing, you agree that your contributions will be licensed under the **GNU Affero General Public License v3.0** (AGPLv3).

This means:
- Anyone can use, modify, and distribute the code
- Modified versions must also be **open source** under AGPLv3
- If you run a modified version on a server, users must be able to access the source

See [LICENSE](LICENSE) for the full text.

---

*Questions? Open an issue or start a discussion on GitHub.*
