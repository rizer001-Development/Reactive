package org.rizer001.reactive.gamerules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static hook for per-world game rules.
 * Called from patched ServerLevel.getGameRules() instead of server.getGameRules().
 *
 * Stack replacement:
 *   Original:  aload_0; getfield server; invokevirtual MinecraftServer.getGameRules(); areturn
 *   Patched:   aload_0; getfield server; aload_0; invokestatic ReactiveGameRuleHooks.getGameRules(server, this); areturn
 */
public class ReactiveGameRuleHooks {

    private static final Map<String, GameRules> worldGameRules = new ConcurrentHashMap<>();

    /**
     * Get or create per-world GameRules.
     * Called from patched ServerLevel.getGameRules().
     */
    public static GameRules getGameRules(MinecraftServer server, ServerLevel level) {
        String worldName = level.dimension().identifier().toString();
        return worldGameRules.computeIfAbsent(worldName, k -> {
            // Create new GameRules by copying from server defaults
            GameRules defaults = server.getGameRules();
            GameRules rules = defaults.copy(level.enabledFeatures());
            // Apply persisted values from gamerules.toml (if any) so edits survive restarts
            Map<String, Map<String, String>> tomlData = GameRuleTomlStore.loadAll();
            Map<String, String> worldRules = tomlData.get(worldName);
            if (worldRules != null && !worldRules.isEmpty()) {
                GameRuleTomlStore.applyToGameRules(Map.of(worldName, worldRules), Map.of(worldName, rules));
                System.out.println("[Reactive] Applied game rules from gamerules.toml for " + worldName);
            }
            return rules;
        });
    }

    /**
     * Get GameRules for a specific world.
     */
    public static GameRules getForWorld(String worldName) {
        return worldGameRules.get(worldName);
    }

    /**
     * Get all world game rules (for saving).
     */
    public static Map<String, GameRules> getAll() {
        return worldGameRules;
    }

    /**
     * Register a world's game rules (called when level is loaded).
     */
    public static void register(String worldName, GameRules rules) {
        worldGameRules.put(worldName, rules);
    }

    /**
     * Clear all game rules (for shutdown/reload).
     */
    public static void clear() {
        worldGameRules.clear();
    }
}
