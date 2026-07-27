package org.dreeam.leaf.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class LeafConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("LeafConfig");
    private static final Path SERVER_DIR = Paths.get("");
    private static final Path CONFIG_DIR = SERVER_DIR.resolve("config");
    private static YamlConfiguration config;
    private static File configFile;

    public static void loadConfig() {
        try {
            // Ensure /config/ directory exists
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                LOGGER.info("Reactive: Created config/ directory for server configuration files.");
            }

            // Create default eula.txt in config/ if not exists (auto-accept)
            File eulaFile = CONFIG_DIR.resolve("eula.txt").toFile();
            if (!eulaFile.exists()) {
                try {
                    String eulaContent = "#By changing the setting below to TRUE, you are indicating your agreement to our EULA\n"
                        + "#https://aka.ms/MinecraftEULA\n"
                        + "eula=true\n";
                    Files.writeString(eulaFile.toPath(), eulaContent);
                    LOGGER.info("Reactive: Created eula.txt in config/ directory (auto-accepted).");
                } catch (IOException e) {
                    LOGGER.warn("Reactive: Could not create eula.txt", e);
                }
            }

            // Create default server.properties in config/ if not exists
            File serverProps = CONFIG_DIR.resolve("server.properties").toFile();
            if (!serverProps.exists()) {
                YamlConfiguration props = new YamlConfiguration();
                props.set("online-mode", false);
                props.set("spawn-protection", 0);
                props.set("max-players", 20);
                props.set("motd", "A Reactive Server");
                try {
                    // Use java.util.Properties for server.properties format
                    java.util.Properties javaProps = new java.util.Properties();
                    javaProps.setProperty("online-mode", "false");
                    javaProps.setProperty("spawn-protection", "0");
                    javaProps.setProperty("max-players", "20");
                    javaProps.setProperty("motd", "A Reactive Server");
                    javaProps.setProperty("difficulty", "easy");
                    try (var fos = new java.io.FileOutputStream(serverProps)) {
                        javaProps.store(fos, "Minecraft server properties");
                    }
                    LOGGER.info("Reactive: Created default server.properties in config/ directory.");
                } catch (IOException e) {
                    LOGGER.warn("Reactive: Could not create server.properties", e);
                }
            }

            // Create default bukkit.yml in config/ if not exists
            File bukkitYml = CONFIG_DIR.resolve("bukkit.yml").toFile();
            if (!bukkitYml.exists()) {
                try {
                    String defaultBukkit = ""
                            + "settings:\n"
                            + "  allow-end: true\n"
                            + "  warn-on-overload: true\n"
                            + "  permissions-file: permissions.yml\n"
                            + "  update-folder: update\n"
                            + "  plugin-profiling: false\n"
                            + "  connection-throttle: 4000\n"
                            + "  query-plugins: true\n"
                            + "  deprecated-verbose: default\n"
                            + "  shutdown-message: Server closed\n"
                            + "spawn-limits:\n"
                            + "  monsters: 70\n"
                            + "  animals: 15\n"
                            + "  water-animals: 5\n"
                            + "  water-ambient: 20\n"
                            + "  water-underground-creature: 5\n"
                            + "  axolotls: 5\n"
                            + "  ambient: 15\n"
                            + "chunk-gc:\n"
                            + "  period-in-ticks: 600\n"
                            + "ticks-per:\n"
                            + "  animal-spawns: 400\n"
                            + "  monster-spawns: 1\n"
                            + "  water-spawns: 1\n"
                            + "  water-ambient-spawns: 1\n"
                            + "  water-underground-creature-spawns: 1\n"
                            + "  axolotl-spawns: 1\n"
                            + "  ambient-spawns: 1\n"
                            + "  autosave: 6000\n"
                            + "aliases: /dev/null\n";
                    Files.writeString(bukkitYml.toPath(), defaultBukkit);
                    LOGGER.info("Reactive: Created default bukkit.yml in config/ directory.");
                } catch (IOException e) {
                    LOGGER.warn("Reactive: Could not create bukkit.yml", e);
                }
            }

            // Create default spigot.yml in config/ if not exists
            File spigotYml = CONFIG_DIR.resolve("spigot.yml").toFile();
            if (!spigotYml.exists()) {
                try {
                    String defaultSpigot = ""
                            + "settings:\n"
                            + "  debug: false\n"
                            + "  save-user-cache-on-stop-only: false\n"
                            + "  sample-count: 12\n"
                            + "  bungeecord: false\n"
                            + "  player-shuffle: 0\n"
                            + "  user-cache-size: 1000\n"
                            + "  int-cache-limit: 1024\n"
                            + "  moved-wrongly-threshold: 0.0625\n"
                            + "  moved-too-quickly-multiplier: 10.0\n"
                            + "  netty-threads: 4\n"
                            + "  timeout-time: 60\n"
                            + "  restart-on-crash: true\n"
                            + "  restart-script: ./start.sh\n"
                            + "  attribute:\n"
                            + "    maxHealth:\n"
                            + "      max: 2048\n"
                            + "    movementSpeed:\n"
                            + "      max: 2048\n"
                            + "    attackDamage:\n"
                            + "      max: 2048\n"
                            + "messages:\n"
                            + "  whitelist: You are not whitelisted on this server!\n"
                            + "  unknown-command: Unknown command. Type \\\"/help\\\" for help.\n"
                            + "  server-full: The server is full!\n"
                            + "  outdated-client: Outdated client! Please use {0}\n"
                            + "  outdated-server: Outdated server! I'm still on {0}\n"
                            + "  restart: Server is restarting\n"
                            + "advancements:\n"
                            + "  disable-saving: false\n"
                            + "  disabled:\n"
                            + "  - minecraft:story/disabled\n"
                            + "world-settings:\n"
                            + "  default:\n"
                            + "    verbose: false\n";
                    Files.writeString(spigotYml.toPath(), defaultSpigot);
                    LOGGER.info("Reactive: Created default spigot.yml in config/ directory.");
                } catch (IOException e) {
                    LOGGER.warn("Reactive: Could not create spigot.yml", e);
                }
            }

            // Create reactive-config.yml in config/ if not exists
            configFile = CONFIG_DIR.resolve("reactive-config.yml").toFile();
            if (!configFile.exists()) {
                try {
                    String defaultReactiveConfig = ""
                            + "# Reactive Configuration\n"
                            + "# This file contains all Reactive-specific settings.\n\n"
                            + "reactive:\n"
                            + "  # World height settings\n"
                            + "  world-height:\n"
                            + "    enabled: true\n"
                            + "    overworld-min-y: -64\n"
                            + "    overworld-max-y: 2048\n"
                            + "    nether-min-y: 0\n"
                            + "    nether-max-y: 256\n"
                            + "    end-min-y: 0\n"
                            + "    end-max-y: 256\n\n"
                            + "  # World border settings (horizontal extension to 67M blocks)\n"
                            + "  world-border:\n"
                            + "    enabled: false\n"
                            + "    max-xz: 67000000\n\n"
                            + "  # Database settings\n"
                            + "  database:\n"
                            + "    enabled: false\n"
                            + "    type: sqlite\n"
                            + "    sqlite-file: reactive.db\n\n"
                            + "  # EULA\n"
                            + "  eula:\n"
                            + "    auto-accept: true\n";
                    Files.writeString(configFile.toPath(), defaultReactiveConfig);
                    LOGGER.info("Reactive: Created reactive-config.yml in config/ directory.");
                } catch (IOException e) {
                    LOGGER.warn("Reactive: Could not create reactive-config.yml", e);
                }
            }

            // Load reactive-config.yml
            config = new YamlConfiguration();
            try {
                config.load(configFile);
            } catch (Exception e) {
                LOGGER.warn("Reactive: Could not load reactive-config.yml, using defaults", e);
            }

            LOGGER.info("LeafConfig: Configuration initialized successfully.");

        } catch (Exception e) {
            LOGGER.error("LeafConfig: Failed to initialize configuration", e);
        }
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static boolean getBoolean(String path, boolean defaultValue) {
        if (config == null) return defaultValue;
        return config.getBoolean(path, defaultValue);
    }

    public static int getInt(String path, int defaultValue) {
        if (config == null) return defaultValue;
        return config.getInt(path, defaultValue);
    }

    public static String getString(String path, String defaultValue) {
        if (config == null) return defaultValue;
        return config.getString(path, defaultValue);
    }
}
