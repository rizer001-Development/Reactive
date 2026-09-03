package org.rizer001.reactive.gamerules;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads/writes gamerules.toml with per-world sections.
 * Format:
 *   [world]
 *   doDaylightCycle = true
 *   doMobSpawning = false
 *
 *   [world_nether]
 *   doDaylightCycle = true
 */
public class GameRuleTomlStore {

    private static final Path TOML_PATH = Path.of("gamerules.toml");

    /**
     * Load gamerules from TOML file. Returns map of worldName -> (ruleName -> value string).
     * If file doesn't exist, returns empty map.
     */
    public static Map<String, Map<String, String>> loadAll() {
        Map<String, Map<String, String>> result = new HashMap<>();
        if (!Files.exists(TOML_PATH)) {
            return result;
        }
        try {
            // IMPORTANT: use toMap(), not entrySet() — toml4j returns nested tables as
            // Toml objects from entrySet(), not as Map, so instanceof Map would always fail.
            Map<String, Object> root = new Toml().read(TOML_PATH.toFile()).toMap();
            for (Map.Entry<String, Object> worldEntry : root.entrySet()) {
                if (worldEntry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rules = (Map<String, Object>) worldEntry.getValue();
                    Map<String, String> worldRules = new HashMap<>();
                    for (Map.Entry<String, Object> rule : rules.entrySet()) {
                        worldRules.put(rule.getKey(), String.valueOf(rule.getValue()));
                    }
                    // toml4j 0.7.2 keeps the quotes of quoted table keys (["minecraft:overworld"]),
                    // so strip them to match level.dimension().identifier().toString()
                    result.put(stripQuotes(worldEntry.getKey()), worldRules);
                }
            }
        } catch (Exception e) {
            System.out.println("[Reactive] Failed to load gamerules.toml: " + e.getMessage());
        }
        return result;
    }

    /**
     * toml4j 0.7.2 keeps the surrounding quotes of quoted keys (["minecraft:overworld"]),
     * so strip them when reading.
     */
    private static String stripQuotes(String key) {
        if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    /**
     * Save all game rules for all worlds to TOML file.
     * worldRules: worldName -> GameRules object
     */
    public static void saveAll(Map<String, GameRules> worldRules) {
        if (worldRules.isEmpty()) {
            return;
        }
        TomlWriter writer = new TomlWriter();
        Map<String, Map<String, String>> data = new HashMap<>();

        for (Map.Entry<String, GameRules> entry : worldRules.entrySet()) {
            String worldName = entry.getKey();
            GameRules rules = entry.getValue();
            Map<String, String> ruleMap = new HashMap<>();

            // Iterate over all registered game rules
            for (GameRule<?> rule : BuiltInRegistries.GAME_RULE.stream().toList()) {
                try {
                    ruleMap.put(rule.id(), String.valueOf(rules.get(rule)));
                } catch (IllegalArgumentException ignored) {
                    // Skip rules not available for this world's feature flags
                }
            }
            data.put(worldName, ruleMap);
        }

        try (FileWriter fw = new FileWriter(TOML_PATH.toFile())) {
            fw.write("# Reactive Server - Game Rules (per world)\n");
            fw.write("# Auto-generated. Edit values and use /reactive reload to apply.\n");
            fw.write("# For server.toml changes, restart the server.\n\n");
            writer.write(data, fw);
            System.out.println("[Reactive] Saved gamerules.toml");
        } catch (IOException e) {
            System.out.println("[Reactive] Failed to save gamerules.toml: " + e.getMessage());
        }
    }

    /**
     * Apply loaded TOML rules to actual GameRules objects.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void applyToGameRules(Map<String, Map<String, String>> tomlData,
                                         Map<String, GameRules> worldRules) {
        for (Map.Entry<String, Map<String, String>> worldEntry : tomlData.entrySet()) {
            String worldName = worldEntry.getKey();
            GameRules rules = worldRules.get(worldName);
            if (rules == null) {
                System.out.println("[Reactive] World '" + worldName + "' not found, skipping gamerules");
                continue;
            }
            for (Map.Entry<String, String> ruleEntry : worldEntry.getValue().entrySet()) {
                String ruleName = ruleEntry.getKey();
                String valueStr = ruleEntry.getValue();
                try {
                    // Look up rule by name
                    Identifier id = Identifier.parse(ruleName);
                    var ref = BuiltInRegistries.GAME_RULE.get(id);
                    if (ref.isPresent()) {
                        GameRule rule = ref.get().value();
                        // Deserialize the string value to the proper type
                        var result = rule.deserialize(valueStr);
                        if (result.result().isPresent()) {
                            rules.set(rule, result.result().get(), null); // null = no server to broadcast
                        } else {
                            System.out.println("[Reactive] Invalid value for gamerule " + ruleName + ": " + valueStr);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Reactive] Failed to set gamerule " + ruleName + "=" + valueStr + ": " + e.getMessage());
                }
            }
        }
    }
}
