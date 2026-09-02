package org.rizer001.reactive.gamerules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-world game rules lifecycle:
 * - Load from gamerules.toml on startup
 * - Auto-save to gamerules.toml on interval
 * - Save on server shutdown
 * - Reload via /reactive reload
 */
public class ReactiveGameRuleManager {

    private static final Path TOML_PATH = Path.of("gamerules.toml");
    private static ScheduledExecutorService autoSaveScheduler;

    /**
     * Called during server startup.
     * 1. Creates gamerules.toml from default template if missing
     * 2. Loads gamerules from TOML
     * 3. Starts auto-save timer
     */
    public static void init() {
        // Create from default template if missing
        if (!Files.exists(TOML_PATH)) {
            try (InputStream in = ReactiveGameRuleManager.class.getResourceAsStream("/default-gamerules.toml")) {
                if (in != null) {
                    Files.copy(in, TOML_PATH);
                    System.out.println("[Reactive] Created gamerules.toml from default template");
                }
            } catch (Exception e) {
                System.out.println("[Reactive] Failed to create gamerules.toml: " + e.getMessage());
            }
        }
    }

    /**
     * Called after all worlds are loaded — applies TOML rules to worlds.
     */
    public static void applyToWorlds(Map<String, GameRules> worldRules) {
        Map<String, Map<String, String>> tomlData = GameRuleTomlStore.loadAll();
        if (!tomlData.isEmpty()) {
            GameRuleTomlStore.applyToGameRules(tomlData, worldRules);
            System.out.println("[Reactive] Applied game rules from gamerules.toml");
        }
    }

    /**
     * Called after worlds are loaded — register all worlds with ReactiveGameRuleHooks.
     */
    public static void registerWorlds(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            String worldName = level.dimension().identifier().toString();
            GameRules rules = ReactiveGameRuleHooks.getForWorld(worldName);
            if (rules == null) {
                // Level was loaded before our hook was registered — grab from level
                // This shouldn't happen with the ASM patch, but handle gracefully
                rules = level.getGameRules();
            }
            ReactiveGameRuleHooks.register(worldName, rules);
        }
    }

    /**
     * Start the auto-save timer.
     */
    public static void startAutoSave(int intervalSeconds) {
        stopAutoSave();
        autoSaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Reactive-GameRule-AutoSave");
            t.setDaemon(true);
            return t;
        });
        autoSaveScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    Map<String, GameRules> all = ReactiveGameRuleHooks.getAll();
                    if (!all.isEmpty()) {
                        GameRuleTomlStore.saveAll(all);
                    }
                } catch (Exception e) {
                    System.out.println("[Reactive] Auto-save game rules failed: " + e.getMessage());
                }
            },
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        System.out.println("[Reactive] Game rules auto-save started: every " + intervalSeconds + "s");
    }

    /**
     * Stop the auto-save timer.
     */
    public static void stopAutoSave() {
        if (autoSaveScheduler != null && !autoSaveScheduler.isShutdown()) {
            autoSaveScheduler.shutdownNow();
            autoSaveScheduler = null;
        }
    }

    /**
     * Save all game rules to TOML (called on shutdown).
     */
    public static void saveAll() {
        try {
            Map<String, GameRules> all = ReactiveGameRuleHooks.getAll();
            if (!all.isEmpty()) {
                GameRuleTomlStore.saveAll(all);
            }
        } catch (Exception e) {
            System.out.println("[Reactive] Failed to save game rules on shutdown: " + e.getMessage());
        }
    }

    /**
     * Reload gamerules from TOML file.
     * Called from /reactive reload.
     */
    public static void reload(MinecraftServer server) {
        Map<String, GameRules> all = ReactiveGameRuleHooks.getAll();
        Map<String, Map<String, String>> tomlData = GameRuleTomlStore.loadAll();
        if (!tomlData.isEmpty()) {
            GameRuleTomlStore.applyToGameRules(tomlData, all);
            System.out.println("[Reactive] Reloaded game rules from gamerules.toml");
        } else {
            System.out.println("[Reactive] No game rules found in gamerules.toml");
        }
    }
}
