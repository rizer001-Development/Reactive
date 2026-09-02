/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.RegistryAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Reactive: all server settings live in server.toml
 * (TOML, human-edited, never rewritten by the server).
 *
 * Loading strategy (loadFromFile):
 *   1. server.toml is created from the bundled default-server.toml if missing.
 *   2. Every [section] key is registered under its plain name ("port") and,
 *      when not conflicting, also under its section-qualified name ("server.port",
 *      "rcon.port", "management.port", ...).
 *   3. Official vanilla key aliases ("server-port", "motd", "rcon.port",
 *      "management-server-port", ...) are registered explicitly, so vanilla
 *      DedicatedServerProperties keeps reading its expected keys unchanged.
 *   4. The internal Properties object is only an in-memory settings store —
 *      nothing is ever read from or written to a .properties file.
 */
@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public abstract class Settings<T extends Settings<T>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Path of the server config file, relative to the working directory. */
    public static final String SERVER_TOML = "server.toml";
    /** Bundled template resource used to create server.toml on first run. */
    private static final String DEFAULT_SERVER_TOML_RESOURCE = "/default-server.toml";

    /** Vanilla key aliases: vanilla key -> canonical TOML key (section-qualified). */
    private static final Map<String, String> VANILLA_KEY_ALIASES = buildVanillaKeyAliases();

    /** Legacy plain key fallbacks for server.toml files created by older Reactive templates. */
    private static final Map<String, String> LEGACY_KEY_ALIASES = Map.of(
        "server-port", "port",
        "server-ip", "ip"
    );

    protected final Properties properties;

    public Settings(Properties properties) {
        this.properties = properties;
    }

    private static Map<String, String> buildVanillaKeyAliases() {
        Map<String, String> m = new LinkedHashMap<>();
        // [server]
        m.put("server-port", "server.server-port");
        m.put("server-ip", "server.server-ip");
        m.put("motd", "server.motd");
        m.put("level-name", "server.level-name");
        m.put("level-seed", "server.level-seed");
        m.put("level-type", "server.level-type");
        m.put("max-players", "server.max-players");
        m.put("online-mode", "server.online-mode");
        m.put("difficulty", "server.difficulty");
        m.put("gamemode", "server.gamemode");
        m.put("hardcore", "server.hardcore");
        m.put("pvp", "server.pvp");
        m.put("allow-flight", "server.allow-flight");
        m.put("spawn-protection", "server.spawn-protection");
        m.put("max-world-size", "server.max-world-size");
        m.put("view-distance", "server.view-distance");
        m.put("simulation-distance", "server.simulation-distance");
        m.put("network-compression-threshold", "server.network-compression-threshold");
        m.put("max-tick-time", "server.max-tick-time");
        m.put("rate-limit", "server.rate-limit");
        m.put("sync-chunk-writes", "server.sync-chunk-writes");
        m.put("use-native-transport", "server.use-native-transport");
        m.put("enable-status", "server.enable-status");
        m.put("enforce-whitelist", "server.enforce-whitelist");
        m.put("white-list", "server.white-list");
        m.put("force-gamemode", "server.force-gamemode");
        m.put("prevent-proxy-connections", "server.prevent-proxy-connections");
        m.put("max-chained-neighbor-updates", "server.max-chained-neighbor-updates");
        m.put("op-permission-level", "server.op-permission-level");
        m.put("function-permission-level", "server.function-permission-level");
        m.put("player-idle-timeout", "server.player-idle-timeout");
        m.put("enforce-secure-profile", "server.enforce-secure-profile");
        m.put("hide-online-players", "server.hide-online-players");
        m.put("initial-enabled-packs", "server.initial-enabled-packs");
        m.put("initial-disabled-packs", "server.initial-disabled-packs");
        m.put("resource-pack", "server.resource-pack");
        m.put("resource-pack-sha1", "server.resource-pack-sha1");
        m.put("require-resource-pack", "server.resource-pack-required");
        m.put("resource-pack-prompt", "server.resource-pack-prompt");
        m.put("generate-structures", "server.generate-structures");
        m.put("generator-settings", "server.generator-settings");
        m.put("allow-nether", "server.allow-nether");
        m.put("allow-flight", "server.allow-flight");
        m.put("spawn-animals", "server.spawn-animals");
        m.put("spawn-monsters", "server.spawn-monsters");
        m.put("spawn-npcs", "server.spawn-npcs");
        // [network]
        m.put("query.port", "network.query-port");
        m.put("rcon.port", "network.rcon-port");
        m.put("rcon.password", "network.rcon-password");
        // [management]
        m.put("management-server-enabled", "management.enabled");
        m.put("management-server-host", "management.host");
        m.put("management-server-port", "management.port");
        m.put("management-server-secret", "management.secret");
        m.put("management-server-tls-enabled", "management.tls-enabled");
        m.put("management-server-tls-keystore", "management.tls-keystore");
        m.put("management-server-tls-keystore-password", "management.tls-keystore-password");
        m.put("management-server-allowed-origins", "management.allowed-origins");
        return m;
    }

    /**
     * Load server settings from server.toml (TOML only).
     * The path parameter is kept for signature compatibility with vanilla
     * DedicatedServerProperties.fromFile(Path) and is ignored.
     */
    public static Properties loadFromFile(Path ignoredPath) {
        Path tomlPath = Path.of(SERVER_TOML);
        if (!Files.exists(tomlPath)) {
            try (InputStream in = Settings.class.getResourceAsStream(DEFAULT_SERVER_TOML_RESOURCE)) {
                if (in != null) {
                    Files.copy(in, tomlPath);
                    LOGGER.info("Created server.toml from default template");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to create server.toml: {}", (Object)e.getMessage());
            }
        }
        if (!Files.isRegularFile(tomlPath)) {
            LOGGER.error("server.toml not found — using built-in default server settings");
            return new Properties();
        }
        return loadFromToml(tomlPath);
    }

    private static Properties loadFromToml(Path tomlPath) {
        Properties props = new Properties();

        Map<String, Object> root;
        try {
            com.moandjiezana.toml.Toml toml = new com.moandjiezana.toml.Toml().read(tomlPath.toFile());
            root = toml.toMap();
        } catch (Exception e) {
            LOGGER.error("Failed to parse server.toml: {} — using built-in default server settings", (Object)String.valueOf(e.getMessage()));
            return props;
        }

        // 1) Collect section-qualified values and plain-key values (first section wins).
        Map<String, String> qualified = new LinkedHashMap<>();
        Map<String, String> plain = new LinkedHashMap<>();
        for (Map.Entry<String, Object> top : root.entrySet()) {
            Object topValue = top.getValue();
            if (!(topValue instanceof Map)) {
                // Top-level scalar (no section): reachable by its plain name.
                plain.putIfAbsent(top.getKey(), String.valueOf(topValue));
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> section = (Map<String, Object>)topValue;
            for (Map.Entry<String, Object> leaf : section.entrySet()) {
                String qual = top.getKey() + "." + leaf.getKey();
                String strVal = leaf.getValue() != null ? String.valueOf(leaf.getValue()) : "";
                qualified.put(qual, strVal);
                plain.putIfAbsent(leaf.getKey(), strVal);
            }
        }
        for (Map.Entry<String, String> e : plain.entrySet()) {
            props.setProperty(e.getKey(), e.getValue());
        }

        // 2) Register vanilla key aliases from their section-qualified TOML keys.
        //    Qualified names are authoritative; plain fallback covers custom sections.
        for (Map.Entry<String, String> alias : VANILLA_KEY_ALIASES.entrySet()) {
            String vanillaKey = alias.getKey();
            String target = alias.getValue();
            String value = qualified.containsKey(target) ? qualified.get(target) : plain.get(target);
            if (value == null) {
                String legacy = LEGACY_KEY_ALIASES.get(vanillaKey);
                if (legacy != null) {
                    value = plain.get(legacy);
                }
            }
            if (value != null) {
                props.setProperty(vanillaKey, value);
            }
        }
        return props;
    }

    /**
     * Reactive: server.toml is user-edited — the server never rewrites it.
     * Kept as a no-op for signature compatibility with vanilla callers
     * (DedicatedServerSettings.forceSave / update).
     */
    public void store(Path output) {
        LOGGER.debug("store() skipped — server.toml is user-managed");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Function wrapNumberDeserializer(Function<String, Number> inner) {
        return s -> {
            try {
                return (Number)inner.apply((String)s);
            }
            catch (NumberFormatException e) {
                return null;
            }
        };
    }

    protected static <V> Function<String, @Nullable V> dispatchNumberOrString(IntFunction<@Nullable V> intDeserializer, Function<String, @Nullable V> stringDeserializer) {
        return s -> {
            try {
                return intDeserializer.apply(Integer.parseInt(s));
            }
            catch (NumberFormatException e) {
                return stringDeserializer.apply((String)s);
            }
        };
    }

    private @Nullable String getStringRaw(String key) {
        return (String)this.properties.get(key);
    }

    protected <V> @Nullable V getLegacy(String key, Function<String, V> deserializer) {
        String value = this.getStringRaw(key);
        if (value == null) {
            return null;
        }
        this.properties.remove(key);
        return deserializer.apply(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <V> V get(String key, Function deserializer, Function serializer, V defaultValue) {
        String value = this.getStringRaw(key);
        Object result = MoreObjects.firstNonNull(value != null ? deserializer.apply(value) : null, defaultValue);
        this.properties.put(key, serializer.apply(result));
        return (V)result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <V> MutableValue<V> getMutable(String key, Function deserializer, Function serializer, V defaultValue) {
        String value = this.getStringRaw(key);
        Object result = MoreObjects.firstNonNull(value != null ? deserializer.apply(value) : null, defaultValue);
        this.properties.put(key, serializer.apply(result));
        return (MutableValue<V>) new MutableValue(this, key, result, serializer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <V> V get(String key, Function deserializer, UnaryOperator validator, Function serializer, V defaultValue) {
        return (V)this.get(key, s -> {
            Object result = deserializer.apply((String)s);
            return result != null ? validator.apply(result) : null;
        }, serializer, defaultValue);
    }

    protected <V> V get(String key, Function<String, V> deserializer, V defaultValue) {
        return (V)this.get(key, deserializer, Objects::toString, defaultValue);
    }

    protected <V> MutableValue<V> getMutable(String key, Function<String, V> deserializer, V defaultValue) {
        return this.getMutable(key, deserializer, Objects::toString, defaultValue);
    }

    protected String get(String key, String defaultValue) {
        return (String)this.get(key, Function.identity(), Function.identity(), defaultValue);
    }

    protected @Nullable String getLegacyString(String key) {
        return (String)this.getLegacy(key, Function.identity());
    }

    protected int get(String key, int defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Integer::parseInt), Integer.valueOf(defaultValue));
    }

    protected MutableValue<Integer> getMutable(String key, int defaultValue) {
        return this.getMutable(key, Settings.wrapNumberDeserializer(Integer::parseInt), defaultValue);
    }

    protected MutableValue<String> getMutable(String key, String defaultValue) {
        return this.getMutable(key, String::new, defaultValue);
    }

    protected int get(String key, UnaryOperator<Integer> validator, int defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Integer::parseInt), validator, Objects::toString, defaultValue);
    }

    protected long get(String key, long defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Long::parseLong), defaultValue);
    }

    protected boolean get(String key, boolean defaultValue) {
        return this.get(key, Boolean::valueOf, defaultValue);
    }

    protected MutableValue<Boolean> getMutable(String key, boolean defaultValue) {
        return this.getMutable(key, Boolean::valueOf, defaultValue);
    }

    protected @Nullable Boolean getLegacyBoolean(String key) {
        return this.getLegacy(key, Boolean::valueOf);
    }

    protected Properties cloneProperties() {
        Properties result = new Properties();
        result.putAll((Map<?, ?>)this.properties);
        return result;
    }

    protected abstract T reload(RegistryAccess var1, Properties var2);

    public class MutableValue<V>
    implements Supplier<V> {
        private final String key;
        private final V value;
        private final Function<V, String> serializer;
        final /* synthetic */ Settings this$0;

        private MutableValue(Settings this$0, String key, V value, Function<V, String> serializer) {
            Settings settings = this$0;
            Objects.requireNonNull(settings);
            this.this$0 = settings;
            this.key = key;
            this.value = value;
            this.serializer = serializer;
        }

        @Override
        public V get() {
            return this.value;
        }

        @SuppressWarnings("unchecked")
        public T update(RegistryAccess registryAccess, V value) {
            Properties properties = this.this$0.cloneProperties();
            properties.put(this.key, this.serializer.apply(value));
            return (T)this.this$0.reload(registryAccess, properties);
        }
    }
}
