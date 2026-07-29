package org.purpurmc.reactive.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * ReactiveConfig — инициализация директории /config/ и управление reactive-config.yml.
 * <p>
 * Вызывается на раннем этапе загрузки сервера (в Main.main) для создания файлов.
 * Загрузка конфига через {@link #loadReactiveConfig()} вызывается позже,
 * перед стартом MSPTAlertTask.
 * <p>
 * Reactive — форк Purpur, поэтому этот конфиг дополняет PurpurConfig,
 * а не заменяет его.
 */
public final class ReactiveConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReactiveConfig");
    private static final Path CONFIG_DIR = Paths.get("config");

    private ReactiveConfig() {}

    // ==========================================================================
    // MSPT Alert Settings
    // ==========================================================================

    /** Whether MSPT overload alerts are enabled. */
    public static boolean msptAlertEnabled = true;
    /** MSPT threshold for a "high load" warning (gold). */
    public static double msptAlertWarningThreshold = 40.0D;
    /** MSPT threshold for a "server overloaded" critical alert (red). */
    public static double msptAlertCriticalThreshold = 50.0D;
    /** Cooldown in seconds between repeated alerts. */
    public static int msptAlertCooldownSeconds = 10;
    /** Permission required for players to receive alerts. */
    public static String msptAlertPermission = "reactive.alerts";
    /** Check interval in ticks (20 ticks = 1 second). */
    public static int msptAlertIntervalTicks = 20;

    // ==========================================================================
    // RAM Alert Settings
    // ==========================================================================

    /** Whether RAM usage alerts are enabled. */
    public static boolean ramAlertEnabled = true;
    /** RAM usage % threshold for a "high usage" warning (gold). */
    public static double ramAlertWarningThreshold = 80.0D;
    /** RAM usage % threshold for a "critical usage" alert (red). */
    public static double ramAlertCriticalThreshold = 90.0D;
    /** Cooldown in seconds between repeated alerts. */
    public static int ramAlertCooldownSeconds = 10;
    /** Permission required for players to receive alerts. */
    public static String ramAlertPermission = "reactive.alerts";
    /** Check interval in ticks (20 ticks = 1 second). */
    public static int ramAlertIntervalTicks = 20;

    // ==========================================================================
    // Entity Tick Limiter Settings
    // ==========================================================================

    /** Whether entity tick limiting is enabled. */
    public static boolean entityTickLimiterEnabled = true;
    /** MSPT threshold above which entity tick limiting activates. */
    public static double entityTickLimiterThreshold = 50.0D;
    /** Minimum contribution % of total entity tick time for a type to be paused. */
    public static double entityTickLimiterMinContributionPercent = 20.0D;
    /** Cooldown in seconds between repeated announcements. */
    public static int entityTickLimiterCooldownSeconds = 10;
    /** Permission required for players to receive announcements. */
    public static String entityTickLimiterPermission = "reactive.alerts";
    /** Check interval in ticks. */
    public static int entityTickLimiterCheckIntervalTicks = 20;

    // ==========================================================================
    // Init — called from Main.main
    // ==========================================================================

    /**
     * Инициализация: создаёт /config/ и дефолтные конфиги.
     * Вызывается один раз при старте сервера.
     */
    public static void init() {
        try {
            createConfigDir();
            createEula();
            createServerProperties();
            createReactiveConfig();
            LOGGER.info("Reactive: Configuration initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Reactive: Failed to initialize configuration", e);
        }
    }

    // ==========================================================================
    // Load — called from DedicatedServer.boot() before MSPTAlertTask.startTask()
    // ==========================================================================

    /**
     * Loads reactive-config.yml and populates the static config fields.
     * Called after the server is fully initialized, right before MSPTAlertTask starts.
     */
    @SuppressWarnings("unchecked")
    public static void loadReactiveConfig() {
        Path configPath = CONFIG_DIR.resolve("reactive-config.yml");
        if (!Files.exists(configPath)) {
            LOGGER.warn("Reactive: reactive-config.yml not found, using defaults.");
            return;
        }

        try (InputStream in = new FileInputStream(configPath.toFile())) {
            Yaml yaml = new Yaml();
            Object raw = yaml.load(in);

            if (!(raw instanceof Map<?, ?> rootMap)) {
                LOGGER.warn("Reactive: Invalid reactive-config.yml structure, using defaults.");
                return;
            }

            Object reactiveRaw = rootMap.get("reactive");
            if (!(reactiveRaw instanceof Map<?, ?> reactiveMap)) {
                LOGGER.warn("Reactive: Missing 'reactive' key in reactive-config.yml, using defaults.");
                return;
            }

            // ── Load MSPT alert config ──
            Object msptRaw = ((Map<String, Object>) reactiveMap).get("mspt-alert");
            if (msptRaw instanceof Map<?, ?> msptMap) {
                Map<String, Object> mspt = (Map<String, Object>) msptMap;

                if (mspt.containsKey("enabled"))
                    msptAlertEnabled = toBoolean(mspt.get("enabled"), true);
                if (mspt.containsKey("warning-threshold"))
                    msptAlertWarningThreshold = toDouble(mspt.get("warning-threshold"), 40.0D);
                if (mspt.containsKey("critical-threshold"))
                    msptAlertCriticalThreshold = toDouble(mspt.get("critical-threshold"), 50.0D);
                if (mspt.containsKey("cooldown-seconds"))
                    msptAlertCooldownSeconds = toInt(mspt.get("cooldown-seconds"), 10);
                if (mspt.containsKey("permission"))
                    msptAlertPermission = mspt.get("permission").toString();
                if (mspt.containsKey("check-interval-ticks"))
                    msptAlertIntervalTicks = toInt(mspt.get("check-interval-ticks"), 20);

                LOGGER.info("Reactive: Loaded MSPT alert config (warning={}ms, critical={}ms, cooldown={}s, interval={}t)",
                    msptAlertWarningThreshold, msptAlertCriticalThreshold,
                    msptAlertCooldownSeconds, msptAlertIntervalTicks);
            } else {
                LOGGER.info("Reactive: No 'mspt-alert' section in config, using defaults.");
            }

            // ── Load RAM alert config ──
            Object ramRaw = ((Map<String, Object>) reactiveMap).get("ram-alert");
            if (ramRaw instanceof Map<?, ?> ramMap) {
                Map<String, Object> ram = (Map<String, Object>) ramMap;

                if (ram.containsKey("enabled"))
                    ramAlertEnabled = toBoolean(ram.get("enabled"), true);
                if (ram.containsKey("warning-threshold"))
                    ramAlertWarningThreshold = toDouble(ram.get("warning-threshold"), 80.0D);
                if (ram.containsKey("critical-threshold"))
                    ramAlertCriticalThreshold = toDouble(ram.get("critical-threshold"), 90.0D);
                if (ram.containsKey("cooldown-seconds"))
                    ramAlertCooldownSeconds = toInt(ram.get("cooldown-seconds"), 10);
                if (ram.containsKey("permission"))
                    ramAlertPermission = ram.get("permission").toString();
                if (ram.containsKey("check-interval-ticks"))
                    ramAlertIntervalTicks = toInt(ram.get("check-interval-ticks"), 20);

                LOGGER.info("Reactive: Loaded RAM alert config (warning={}%, critical={}%, cooldown={}s, interval={}t)",
                    ramAlertWarningThreshold, ramAlertCriticalThreshold,
                    ramAlertCooldownSeconds, ramAlertIntervalTicks);
            } else {
                LOGGER.info("Reactive: No 'ram-alert' section in config, using defaults.");
            }

            // ── Load Entity Tick Limiter config ──
            Object tickLimiterRaw = ((Map<String, Object>) reactiveMap).get("entity-tick-limiter");
            if (tickLimiterRaw instanceof Map<?, ?> tickLimiterMap) {
                Map<String, Object> tlm = (Map<String, Object>) tickLimiterMap;

                if (tlm.containsKey("enabled"))
                    entityTickLimiterEnabled = toBoolean(tlm.get("enabled"), true);
                if (tlm.containsKey("threshold-mspt"))
                    entityTickLimiterThreshold = toDouble(tlm.get("threshold-mspt"), 50.0D);
                if (tlm.containsKey("min-contribution-percent"))
                    entityTickLimiterMinContributionPercent = toDouble(tlm.get("min-contribution-percent"), 20.0D);
                if (tlm.containsKey("cooldown-seconds"))
                    entityTickLimiterCooldownSeconds = toInt(tlm.get("cooldown-seconds"), 10);
                if (tlm.containsKey("permission"))
                    entityTickLimiterPermission = tlm.get("permission").toString();
                if (tlm.containsKey("check-interval-ticks"))
                    entityTickLimiterCheckIntervalTicks = toInt(tlm.get("check-interval-ticks"), 20);

                LOGGER.info("Reactive: Loaded Entity Tick Limiter config (threshold={}ms, minContribution={}%, cooldown={}s, interval={}t)",
                    entityTickLimiterThreshold, entityTickLimiterMinContributionPercent,
                    entityTickLimiterCooldownSeconds, entityTickLimiterCheckIntervalTicks);
            } else {
                LOGGER.info("Reactive: No 'entity-tick-limiter' section in config, using defaults.");
            }

        } catch (Exception e) {
            LOGGER.error("Reactive: Failed to load reactive-config.yml", e);
        }
    }

    // ==========================================================================
    // File creation helpers
    // ==========================================================================

    private static void createConfigDir() throws IOException {
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
            LOGGER.info("Reactive: Created config/ directory for server configuration files.");
        }
    }

    private static void createEula() throws IOException {
        File eulaFile = CONFIG_DIR.resolve("eula.txt").toFile();
        if (!eulaFile.exists()) {
            String content = "#By changing the setting below to TRUE, you are indicating your agreement to our EULA\n"
                + "#https://aka.ms/MinecraftEULA\n"
                + "eula=true\n";
            Files.writeString(eulaFile.toPath(), content);
            LOGGER.info("Reactive: Created eula.txt in config/ directory (auto-accepted).");
        }
    }

    private static void createServerProperties() throws IOException {
        File propsFile = CONFIG_DIR.resolve("server.properties").toFile();
        if (!propsFile.exists()) {
            Properties props = new Properties();
            props.setProperty("online-mode", "false");
            props.setProperty("spawn-protection", "0");
            props.setProperty("max-players", "20");
            props.setProperty("motd", "A Reactive Server");
            props.setProperty("difficulty", "easy");
            try (FileOutputStream fos = new FileOutputStream(propsFile)) {
                props.store(fos, "Minecraft server properties");
            }
            LOGGER.info("Reactive: Created default server.properties in config/ directory.");
        }
    }

    private static void createReactiveConfig() throws IOException {
        File configFile = CONFIG_DIR.resolve("reactive-config.yml").toFile();
        if (!configFile.exists()) {
            String content = ""
                + "# Reactive Configuration\n"
                + "# This file contains all Reactive-specific settings.\n"
                + "\n"
                + "reactive:\n"
                + "  # World height settings\n"
                + "  world-height:\n"
                + "    enabled: true\n"
                + "    overworld-min-y: -64\n"
                + "    overworld-max-y: 2048\n"
                + "    nether-min-y: 0\n"
                + "    nether-max-y: 256\n"
                + "    end-min-y: 0\n"
                + "    end-max-y: 256\n"
                + "\n"
                + "  # World border settings (horizontal extension to 67M blocks)\n"
                + "  world-border:\n"
                + "    enabled: false\n"
                + "    max-xz: 67000000\n"
                + "\n"
                + "  # MSPT (Milliseconds Per Tick) alert settings\n"
                + "  # Sends warnings to players with the configured permission\n"
                + "  # when server MSPT exceeds the specified thresholds.\n"
                + "  mspt-alert:\n"
                + "    enabled: true\n"
                + "    # MSPT threshold for a yellow 'high load' warning\n"
                + "    warning-threshold: 40.0\n"
                + "    # MSPT threshold for a red 'server overloaded' critical alert\n"
                + "    critical-threshold: 50.0\n"
                + "    # Minimum seconds between repeated alerts (prevents spam)\n"
                + "    cooldown-seconds: 10\n"
                + "    # Permission node required to receive alerts\n"
                + "    permission: reactive.alerts\n"
                + "    # How often to check MSPT (in ticks; 20 ticks = 1 second)\n"
                + "    check-interval-ticks: 20\n"
                + "\n"
                + "  # RAM usage alert settings\n"
                + "  # Sends warnings to players with the configured permission\n"
                + "  # when JVM memory usage exceeds the specified thresholds.\n"
                + "  ram-alert:\n"
                + "    enabled: true\n"
                + "    # RAM usage % threshold for a gold 'high usage' warning\n"
                + "    warning-threshold: 80.0\n"
                + "    # RAM usage % threshold for a red 'critical usage' alert\n"
                + "    critical-threshold: 90.0\n"
                + "    # Minimum seconds between repeated alerts (prevents spam)\n"
                + "    cooldown-seconds: 10\n"
                + "    # Permission node required to receive alerts\n"
                + "    permission: reactive.alerts\n"
                + "    # How often to check RAM usage (in ticks; 20 ticks = 1 second)\n"
                + "    check-interval-ticks: 20\n"
                + "\n"
                + "\n"
                + "  # Entity Tick Limiter settings\n"
                + "  # When the server is overloaded (MSPT > threshold), monitors\n"
                + "  # per-entity-type tick time consumption. If a specific entity\n"
                + "  # type contributes more than min-contribution-percent of total\n"
                + "  # entity tick time, its ticking is paused until the server recovers.\n"
                + "  entity-tick-limiter:\n"
                + "    enabled: true\n"
                + "    # MSPT threshold to activate tick limiting\n"
                + "    threshold-mspt: 50.0\n"
                + "    # Minimum % of total entity tick time for a type to be paused\n"
                + "    min-contribution-percent: 20.0\n"
                + "    # Minimum seconds between repeated announcements\n"
                + "    cooldown-seconds: 10\n"
                + "    # Permission node required to receive announcements\n"
                + "    permission: reactive.alerts\n"
                + "    # How often to check entity tick times (in ticks; 20 = 1 second)\n"
                + "    check-interval-ticks: 20\n"
                + "\n"
                + "  # Database settings\n"
                + "  database:\n"
                + "    enabled: false\n"
                + "    type: sqlite\n"
                + "    sqlite-file: reactive.db\n"
                + "\n"
                + "  # EULA\n"
                + "  eula:\n"
                + "    auto-accept: true\n";
            Files.writeString(configFile.toPath(), content);
            LOGGER.info("Reactive: Created reactive-config.yml in config/ directory.");
        }
    }

    // ==========================================================================
    // Type conversion helpers
    // ==========================================================================

    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private static double toDouble(Object value, double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
