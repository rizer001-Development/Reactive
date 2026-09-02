package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public abstract class StoredUserList<K, V extends StoredUserEntry<K>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<String, V> map = Maps.newHashMap();
    protected final NotificationService notificationService;

    public StoredUserList(File file, NotificationService notificationService) {
        this.file = file;
        this.notificationService = notificationService;
    }

    public File getFile() {
        return this.file;
    }

    // REACTIVE: TOML support
    private java.io.File getTomlFile() {
        if (this.file.getName().endsWith(".json")) {
            return new java.io.File(this.file.getParent(), this.file.getName().replace(".json", ".toml"));
        }
        return this.file;
    }

    // REACTIVE: Map filename to default template resource
    private String getDefaultTemplateName() {
        String name = this.file.getName().replace(".json", "").replace(".toml", "");
        return "/default-" + name + ".toml";
    }

    // REACTIVE: Copy default template byte-for-byte if TOML file doesn't exist
    private void ensureTomlFileExists() {
        java.io.File tomlFile = getTomlFile();
        if (tomlFile.exists()) return;
        String template = getDefaultTemplateName();
        try (InputStream in = StoredUserList.class.getResourceAsStream(template)) {
            if (in != null) {
                java.io.File parent = tomlFile.getParentFile();
                if (parent != null) parent.mkdirs();
                java.nio.file.Files.copy(in, tomlFile.toPath());
                LOGGER.info("Created {} from default template", (Object) tomlFile.getName());
            } else {
                // No template found — create minimal file
                java.nio.file.Files.writeString(tomlFile.toPath(), "# " + tomlFile.getName() + "\n\n");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create {} from template: {}", (Object) tomlFile.getName(), (Object) e.getMessage());
        }
    }

    public boolean add(V infos) {
        String keyForUser = this.getKeyForUser((K)((StoredUserEntry)infos).getUser());
        StoredUserEntry previous = (StoredUserEntry)this.map.get(keyForUser);
        if (infos.equals(previous)) {
            return false;
        }
        this.map.put(keyForUser, infos);
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after adding a user.", (Throwable)e);
        }
        return true;
    }

    public @Nullable V get(K user) {
        this.removeExpired();
        return (V)((StoredUserEntry)this.map.get(this.getKeyForUser(user)));
    }

    public boolean remove(K user) {
        StoredUserEntry removed = (StoredUserEntry)this.map.remove(this.getKeyForUser(user));
        if (removed == null) {
            return false;
        }
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after removing a user.", (Throwable)e);
        }
        return true;
    }

    public boolean remove(StoredUserEntry<K> infos) {
        return this.remove(Objects.requireNonNull(infos.getUser()));
    }

    public void clear() {
        this.map.clear();
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after removing a user.", (Throwable)e);
        }
    }

    public String[] getUserList() {
        return this.map.keySet().toArray(new String[0]);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    protected String getKeyForUser(K user) {
        return user.toString();
    }

    protected boolean contains(K user) {
        return this.map.containsKey(this.getKeyForUser(user));
    }

    private void removeExpired() {
        ArrayList toRemove = Lists.newArrayList();
        for (StoredUserEntry entry : this.map.values()) {
            if (!entry.hasExpired()) continue;
            toRemove.add(entry.getUser());
        }
        for (Object user : toRemove) {
            this.map.remove(this.getKeyForUser((K)user));
        }
    }

    protected abstract StoredUserEntry<K> createEntry(JsonObject var1);

    public Collection<V> getEntries() {
        return this.map.values();
    }

    // REACTIVE: Save as TOML — writes entries after the template header
    public void save() throws IOException {
        java.io.File tomlFile = getTomlFile();
        ensureTomlFileExists();
        try {
            // Read the template header (all comment lines at top)
            StringBuilder header = new StringBuilder();
            if (tomlFile.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(tomlFile.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        header.append(line).append("\n");
                    } else {
                        break; // Stop at first non-comment line
                    }
                }
            } else {
                header.append("# Player list\n\n");
            }

            // Write header + entries
            StringBuilder sb = new StringBuilder(header);
            for (V entry : this.map.values()) {
                String key = entry.getUser().toString();
                sb.append("[").append(key).append("]\n");
                JsonObject json = new JsonObject();
                entry.serialize(json);
                for (java.util.Map.Entry<String, JsonElement> e : json.entrySet()) {
                    JsonElement val = e.getValue();
                    if (val.isJsonPrimitive()) {
                        sb.append(e.getKey()).append(" = \"").append(val.getAsString()).append("\"\n");
                    }
                }
                sb.append("\n");
            }
            java.nio.file.Files.writeString(tomlFile.toPath(), sb.toString());
        } catch (Exception e) {
            LOGGER.warn("Failed to save to TOML, falling back to JSON", e);
            JsonArray result = new JsonArray();
            this.map.values().stream().map(entry -> Util.make(new JsonObject(), entry::serialize)).forEach(arg_0 -> ((JsonArray)result).add(arg_0));
            try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(this.file, StandardCharsets.UTF_8))) {
                GSON.toJson((JsonElement)result, GSON.newJsonWriter((Writer)writer));
            }
        }
    }

    public void load() throws IOException {
        java.io.File tomlFile = getTomlFile();
        if (tomlFile.exists()) {
            loadFromToml(tomlFile);
            return;
        }
        if (!this.file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(this.file, StandardCharsets.UTF_8))) {
            this.map.clear();
            JsonArray contents = (JsonArray)GSON.fromJson((Reader)reader, JsonArray.class);
            if (contents == null) {
                return;
            }
            for (JsonElement element : contents) {
                JsonObject object = GsonHelper.convertToJsonObject(element, "entry");
                StoredUserEntry<K> entry = this.createEntry(object);
                if (entry.getUser() == null) continue;
                this.map.put(this.getKeyForUser(entry.getUser()), (V) entry);
            }
        }
    }

    // REACTIVE: Reload from TOML without restarting server
    public void reload() {
        try {
            this.load();
            LOGGER.info("Reloaded {}", (Object) getTomlFile().getName());
        } catch (Exception e) {
            LOGGER.warn("Failed to reload {}: {}", (Object) getTomlFile().getName(), (Object) e.getMessage());
        }
    }

    private void loadFromToml(java.io.File tomlFile) {
        try {
            this.map.clear();
            com.moandjiezana.toml.Toml toml = new com.moandjiezana.toml.Toml().read(tomlFile);
            java.util.Map<String, Object> map = toml.toMap();
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof java.util.Map) {
                    java.util.Map<String, Object> userData = (java.util.Map<String, Object>) entry.getValue();
                    JsonObject json = new JsonObject();
                    json.addProperty("uuid", userData.getOrDefault("uuid", "").toString());
                    json.addProperty("name", userData.getOrDefault("name", entry.getKey()).toString());
                    if (userData.containsKey("expiresOn")) json.addProperty("expiresOn", userData.get("expiresOn").toString());
                    if (userData.containsKey("source")) json.addProperty("source", userData.get("source").toString());
                    if (userData.containsKey("reason")) json.addProperty("reason", userData.get("reason").toString());
                    @SuppressWarnings("unchecked")
                    V userEntry = (V)this.createEntry(json);
                    if (userEntry != null && ((StoredUserEntry)userEntry).getUser() != null) {
                        this.map.put(this.getKeyForUser((K)((StoredUserEntry)userEntry).getUser()), userEntry);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load TOML file: {}", (Object)tomlFile.getName(), e);
        }
    }
}
