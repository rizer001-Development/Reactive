package net.minecraft.server.players;
import net.minecraft.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class CachedUserNameToIdResolver
implements UserNameToIdResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GAMEPROFILES_MRU_LIMIT = 1000;
    private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
    private boolean resolveOfflineUsers = true;
    private final Map<String, GameProfileInfo> profilesByName = new ConcurrentHashMap<String, GameProfileInfo>();
    private final Map<UUID, GameProfileInfo> profilesByUUID = new ConcurrentHashMap<UUID, GameProfileInfo>();
    private final GameProfileRepository profileRepository;
    private final Gson gson = new GsonBuilder().create();
    private final File file;
    private final AtomicLong operationCount = new AtomicLong();

    public CachedUserNameToIdResolver(GameProfileRepository profileRepository, File file) {
        this.profileRepository = profileRepository;
        this.file = file;
        Lists.reverse(this.load()).forEach(this::safeAdd);
    }

    private void safeAdd(GameProfileInfo profileInfo) {
        NameAndId nameAndId = profileInfo.nameAndId();
        profileInfo.setLastAccess(this.getNextOperation());
        this.profilesByName.put(nameAndId.name().toLowerCase(Locale.ROOT), profileInfo);
        this.profilesByUUID.put(nameAndId.id(), profileInfo);
    }

    private Optional<NameAndId> lookupGameProfile(GameProfileRepository profileRepository, String name) {
        if (!StringUtil.isValidPlayerName(name)) {
            return this.createUnknownProfile(name);
        }
        Optional<NameAndId> profile = profileRepository.findProfileByName(name).map(NameAndId::new);
        if (profile.isEmpty()) {
            return this.createUnknownProfile(name);
        }
        return profile;
    }

    private Optional<NameAndId> createUnknownProfile(String name) {
        if (this.resolveOfflineUsers) {
            return Optional.of(NameAndId.createOffline(name));
        }
        return Optional.empty();
    }

    @Override
    public void resolveOfflineUsers(boolean value) {
        this.resolveOfflineUsers = value;
    }

    @Override
    public void add(NameAndId nameAndId) {
        this.addInternal(nameAndId);
    }

    private GameProfileInfo addInternal(NameAndId profile) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
        c.setTime(new Date());
        c.add(2, 1);
        Date expirationDate = c.getTime();
        GameProfileInfo profileInfo = new GameProfileInfo(profile, expirationDate);
        this.safeAdd(profileInfo);
        this.save();
        return profileInfo;
    }

    private long getNextOperation() {
        return this.operationCount.incrementAndGet();
    }

    @Override
    public Optional<NameAndId> get(String name) {
        Optional<NameAndId> result;
        String userName = name.toLowerCase(Locale.ROOT);
        GameProfileInfo profileInfo = this.profilesByName.get(userName);
        boolean needsSave = false;
        if (profileInfo != null && new Date().getTime() >= profileInfo.expirationDate.getTime()) {
            this.profilesByUUID.remove(profileInfo.nameAndId().id());
            this.profilesByName.remove(profileInfo.nameAndId().name().toLowerCase(Locale.ROOT));
            needsSave = true;
            profileInfo = null;
        }
        if (profileInfo != null) {
            profileInfo.setLastAccess(this.getNextOperation());
            result = Optional.of(profileInfo.nameAndId());
        } else {
            Optional<NameAndId> profile = this.lookupGameProfile(this.profileRepository, userName);
            if (profile.isPresent()) {
                result = Optional.of(this.addInternal(profile.get()).nameAndId());
                needsSave = false;
            } else {
                result = Optional.empty();
            }
        }
        if (needsSave) {
            this.save();
        }
        return result;
    }

    @Override
    public Optional<NameAndId> get(UUID id) {
        GameProfileInfo profileInfo = this.profilesByUUID.get(id);
        if (profileInfo == null) {
            return Optional.empty();
        }
        profileInfo.setLastAccess(this.getNextOperation());
        return Optional.of(profileInfo.nameAndId());
    }

    private static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
    }

    // REACTIVE: Ensure TOML file exists with template
    private void ensureTomlFileExists() {
        if (this.file.exists()) return;
        try (InputStream in = CachedUserNameToIdResolver.class.getResourceAsStream("/default-usercache.toml")) {
            if (in != null) {
                java.io.File parent = this.file.getParentFile();
                if (parent != null) parent.mkdirs();
                java.nio.file.Files.copy(in, this.file.toPath());
                LOGGER.info("Created usercache.toml from default template");
            } else {
                java.nio.file.Files.writeString(this.file.toPath(), "# User cache\n\n");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create usercache.toml: {}", (Object) e.getMessage());
        }
    }

    private List<GameProfileInfo> load() {
        ArrayList result = Lists.newArrayList();

        if (this.file.getName().endsWith(".toml")) {
            return loadFromToml();
        }

        // Fallback: JSON
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(this.file, StandardCharsets.UTF_8))) {
            JsonArray entryList = (JsonArray)this.gson.fromJson((Reader)reader, JsonArray.class);
            if (entryList == null) {
                return result;
            }
            DateFormat dateFormat = CachedUserNameToIdResolver.createDateFormat();
            entryList.forEach(element -> CachedUserNameToIdResolver.readGameProfile(element, dateFormat).ifPresent(result::add));
            return result;
        }
        catch (FileNotFoundException e) {
            return result;
        }
        catch (JsonParseException | IOException e) {
            LOGGER.warn("Failed to load profile cache {}", (Object)this.file, (Object)e);
        }
        return result;
    }

    private List<GameProfileInfo> loadFromToml() {
        ArrayList result = Lists.newArrayList();
        try {
            if (!this.file.exists()) {
                ensureTomlFileExists();
                return result;
            }
            com.moandjiezana.toml.Toml toml = new com.moandjiezana.toml.Toml().read(this.file);
            java.util.Map<String, Object> map = toml.toMap();
            DateFormat dateFormat = CachedUserNameToIdResolver.createDateFormat();
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof java.util.Map) {
                    java.util.Map<String, Object> userData = (java.util.Map<String, Object>) entry.getValue();
                    String name = entry.getKey();
                    String uuid = userData.getOrDefault("uuid", "").toString();
                    String expiresOn = userData.getOrDefault("expiresOn", "").toString();
                    if (!uuid.isEmpty()) {
                        try {
                            UUID id = UUID.fromString(uuid);
                            Date expirationDate = dateFormat.parse(expiresOn);
                            NameAndId nameAndId = new NameAndId(id, name);
                            result.add(new GameProfileInfo(nameAndId, expirationDate));
                        } catch (Exception e) {
                            LOGGER.warn("Failed to parse TOML entry {}: {}", name, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load TOML profile cache {}", (Object)this.file.getName(), e);
        }
        return result;
    }

    @Override
    public void save() {
        if (this.file.getName().endsWith(".toml")) {
            saveAsToml();
            return;
        }
        // Fallback: JSON
        JsonArray entryList = new JsonArray();
        DateFormat dateFormat = CachedUserNameToIdResolver.createDateFormat();
        this.getTopMRUProfiles(1000).forEach(entry -> entryList.add(CachedUserNameToIdResolver.writeGameProfile(entry, dateFormat)));
        String toSave = this.gson.toJson((JsonElement)entryList);
        try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(this.file, StandardCharsets.UTF_8))) {
            writer.write(toSave);
        }
        catch (IOException e) {
            // empty
        }
    }

    private void saveAsToml() {
        try {
            ensureTomlFileExists();
            DateFormat dateFormat = CachedUserNameToIdResolver.createDateFormat();

            // Read template header
            StringBuilder header = new StringBuilder();
            if (this.file.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        header.append(line).append("\n");
                    } else {
                        break;
                    }
                }
            } else {
                header.append("# User cache\n\n");
            }

            StringBuilder sb = new StringBuilder(header);
            this.getTopMRUProfiles(1000).forEach(entry -> {
                sb.append("[").append(entry.nameAndId().name()).append("]\n");
                sb.append("uuid = \"").append(entry.nameAndId().id().toString()).append("\"\n");
                sb.append("expiresOn = \"").append(dateFormat.format(entry.expirationDate())).append("\"\n\n");
            });
            java.nio.file.Files.writeString(this.file.toPath(), sb.toString());
        } catch (Exception e) {
            LOGGER.warn("Failed to save TOML profile cache", e);
        }
    }

    // REACTIVE: Reload from TOML
    public void reload() {
        if (this.file.getName().endsWith(".toml")) {
            this.profilesByName.clear();
            this.profilesByUUID.clear();
            Lists.reverse(this.loadFromToml()).forEach(this::safeAdd);
            LOGGER.info("Reloaded usercache.toml");
        }
    }

    private Stream<GameProfileInfo> getTopMRUProfiles(int limit) {
        return ImmutableList.copyOf(this.profilesByUUID.values()).stream().sorted(Comparator.comparing(GameProfileInfo::lastAccess).reversed()).limit(limit);
    }

    private static JsonElement writeGameProfile(GameProfileInfo src, DateFormat dateFormat) {
        JsonObject object = new JsonObject();
        src.nameAndId().appendTo(object);
        object.addProperty("expiresOn", dateFormat.format(src.expirationDate()));
        return object;
    }

    private static Optional<GameProfileInfo> readGameProfile(JsonElement json, DateFormat dateFormat) {
        JsonElement expirationElement;
        JsonObject object;
        NameAndId nameAndId;
        if (json.isJsonObject() && (nameAndId = NameAndId.fromJson(object = json.getAsJsonObject())) != null && (expirationElement = object.get("expiresOn")) != null) {
            String dateAsString = expirationElement.getAsString();
            try {
                Date expirationDate = dateFormat.parse(dateAsString);
                return Optional.of(new GameProfileInfo(nameAndId, expirationDate));
            }
            catch (ParseException e) {
                LOGGER.warn("Failed to parse date {}", (Object)dateAsString, (Object)e);
            }
        }
        return Optional.empty();
    }

    private static class GameProfileInfo {
        private final NameAndId nameAndId;
        private final Date expirationDate;
        private volatile long lastAccess;

        private GameProfileInfo(NameAndId nameAndId, Date expirationDate) {
            this.nameAndId = nameAndId;
            this.expirationDate = expirationDate;
        }

        public NameAndId nameAndId() {
            return this.nameAndId;
        }

        public Date expirationDate() {
            return this.expirationDate;
        }

        public void setLastAccess(long currentOperation) {
            this.lastAccess = currentOperation;
        }

        public long lastAccess() {
            return this.lastAccess;
        }
    }
}
