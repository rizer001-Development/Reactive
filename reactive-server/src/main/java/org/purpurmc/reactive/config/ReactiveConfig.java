package org.purpurmc.reactive.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ReactiveConfig — инициализация директории /config/ и создание файлов по умолчанию.
 * <p>
 * Вызывается на раннем этапе загрузки сервера (в Main.main), до того как сервер
 * начнёт читать конфигурационные файлы. Создаёт директорию /config/ и все
 * необходимые файлы с безопасными значениями по умолчанию, если их нет.
 * <p>
 * Reactive — форк Purpur, поэтому этот конфиг дополняет PurpurConfig,
 * а не заменяет его.
 */
public final class ReactiveConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReactiveConfig");
    private static final Path CONFIG_DIR = Paths.get("config");

    private ReactiveConfig() {}

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
}
