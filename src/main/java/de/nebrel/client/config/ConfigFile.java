package de.nebrel.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * One JSON file on disk, loaded and saved atomically.
 *
 * <p>Every file carries a {@code configVersion}. {@link ConfigMigrations} is
 * given the raw object before it is read, so a config written by an older build
 * is upgraded in place rather than discarded.</p>
 *
 * <p>Nothing here touches the disk outside {@link #load()} and {@link #save}:
 * saving is driven by {@link ConfigManager}'s dirty flag, never per frame.</p>
 */
public final class ConfigFile {

    /** Bumped whenever the on-disk shape changes in a way that needs migrating. */
    public static final int CURRENT_VERSION = 1;

    private static final String VERSION_KEY = "configVersion";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path path;
    private final String name;

    public ConfigFile(Path directory, String fileName) {
        this.path = directory.resolve(fileName);
        this.name = fileName;
    }

    public Path path() {
        return this.path;
    }

    public String name() {
        return this.name;
    }

    public boolean exists() {
        return Files.isRegularFile(this.path);
    }

    /**
     * Reads and migrates the file.
     *
     * @return the stored object, or an empty one when the file is missing,
     *         empty or corrupt. Never null and never throws, so a damaged
     *         config degrades to defaults instead of blocking startup.
     */
    public JsonObject load() {
        if (!exists()) {
            return new JsonObject();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(this.path, StandardCharsets.UTF_8)) {
            var parsed = JsonParser.parseReader(reader);
            root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            quarantine(exception);
            return new JsonObject();
        }

        int version = root.has(VERSION_KEY) && root.get(VERSION_KEY).isJsonPrimitive()
                ? safeInt(root, VERSION_KEY)
                : 0;
        if (version != CURRENT_VERSION) {
            root = ConfigMigrations.migrate(this.name, root, version, CURRENT_VERSION);
        }
        return root;
    }

    private static int safeInt(JsonObject object, String key) {
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    /**
     * Writes the object, stamping the current version.
     *
     * <p>Serialises to a sibling temp file first and then moves it into place,
     * so a crash mid-write cannot leave a truncated config behind.</p>
     *
     * @return true when the file was written
     */
    public boolean save(JsonObject root) {
        root.addProperty(VERSION_KEY, CURRENT_VERSION);
        Path temp = this.path.resolveSibling(this.name + ".tmp");
        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temp, this.path,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException atomicUnsupported) {
                // Some filesystems (certain network mounts) reject ATOMIC_MOVE.
                Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            ConfigLog.warn("Could not write " + this.name, exception);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // nothing further we can do
            }
            return false;
        }
    }

    /**
     * Renames an unreadable config to {@code <name>.broken} so the user can
     * recover it, then lets the caller fall back to defaults.
     */
    private void quarantine(Exception cause) {
        ConfigLog.warn("Could not read " + this.name + ", falling back to defaults", cause);
        try {
            Files.move(this.path, this.path.resolveSibling(this.name + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // best effort only
        }
    }
}
