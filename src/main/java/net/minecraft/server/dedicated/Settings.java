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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
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

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public abstract class Settings<T extends Settings<T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Properties properties;

    public Settings(Properties properties) {
        this.properties = properties;
    }

    // REACTIVE: Convert server.toml to server.properties format
    private static Properties loadFromToml(Path tomlPath) {
        try {
            com.moandjiezana.toml.Toml toml = new com.moandjiezana.toml.Toml().read(tomlPath.toFile());
            Properties props = new Properties();
            java.util.Map<String, Object> map = toml.toMap();
            flattenMap(map, "", props);
            LOGGER.info("Loaded server config from {}", (Object)tomlPath.getFileName());
            return props;
        } catch (Exception e) {
            LOGGER.warn("Failed to load TOML config: {}", (Object)e.getMessage());
            return new Properties();
        }
    }
    
    private static void flattenMap(java.util.Map<String, Object> map, String prefix, Properties props) {
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            String dotKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            String dashKey = prefix.isEmpty() ? entry.getKey() : prefix + "-" + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> subMap = (java.util.Map<String, Object>) value;
                flattenMap(subMap, dotKey, props);
            } else {
                String strVal = value != null ? value.toString() : "";
                props.setProperty(dotKey, strVal);
                props.setProperty(dashKey, strVal);
                props.setProperty(entry.getKey(), strVal);
            }
        }
    }

    public static Properties loadFromFile(Path file) {
        // REACTIVE: Check for server.toml first
        Path tomlPath = file.resolveSibling("server.toml");
        if (!Files.exists(tomlPath)) {
            try (InputStream in = Settings.class.getResourceAsStream("/default-server.toml")) {
                if (in != null) {
                    Files.copy(in, tomlPath);
                    LOGGER.info("Created server.toml from default template");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to create server.toml: {}", (Object)e.getMessage());
            }
        }
        if (Files.exists(tomlPath)) {
            return loadFromToml(tomlPath);
        }
        
        // Vanilla: load from server.properties
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(file)) {
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (CharacterCodingException e) {
            LOGGER.info("Failed to load properties as UTF-8 from file {}, trying ISO_8859_1", (Object)file);
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
                properties.load(reader);
            } catch (IOException e2) {
                LOGGER.error("Failed to load properties from file: {}", (Object)file, (Object)e2);
                return new Properties();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load properties from file: {}", (Object)file, (Object)e);
            return new Properties();
        }
        return properties;
    }

    // REACTIVE: server.toml is user-edited, do NOT overwrite it
    public void store(Path output) {
        // No-op: server.toml is managed by the user, not by the server
        LOGGER.debug("store() called but skipped — server.toml is user-managed");
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
