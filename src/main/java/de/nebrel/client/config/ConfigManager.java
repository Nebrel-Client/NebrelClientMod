package de.nebrel.client.config;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and saves every {@link ConfigSection} under {@code config/nebrelclient/}.
 *
 * <p>Writes are never done from a render or tick callback directly. Changing
 * anything only sets a dirty flag; {@link #tick()} flushes at most once every
 * {@link #SAVE_DEBOUNCE_MILLIS}, and {@link #saveNow()} forces a write when the
 * menu closes or the game shuts down. Dragging a slider therefore costs zero
 * disk IO.</p>
 */
public final class ConfigManager {

    /** Minimum gap between two automatic writes. */
    private static final long SAVE_DEBOUNCE_MILLIS = 3_000L;

    private final Path directory;
    private final List<ConfigSection> sections = new ArrayList<>();

    private volatile boolean dirty;
    private long lastSaveMillis;
    private boolean loaded;

    public ConfigManager(Path configRoot) {
        this.directory = configRoot.resolve("nebrelclient");
    }

    public Path directory() {
        return this.directory;
    }

    public ConfigManager register(ConfigSection section) {
        this.sections.add(section);
        return this;
    }

    /** Flags the config as needing a write. Cheap, safe to call from anywhere. */
    public void markDirty() {
        this.dirty = true;
    }

    public boolean dirty() {
        return this.dirty;
    }

    public boolean loaded() {
        return this.loaded;
    }

    // -- loading -------------------------------------------------------------

    /**
     * Reads every registered section.
     *
     * <p>A failure in one section is logged and skipped so the rest still
     * loads; a broken theme file must not cost the user their module setup.</p>
     */
    public void load() {
        ensureDirectory();
        for (ConfigSection section : this.sections) {
            try {
                JsonObject root = new ConfigFile(this.directory, section.fileName()).load();
                section.read(root);
            } catch (RuntimeException exception) {
                ConfigLog.warn("Failed to apply " + section.fileName(), exception);
            }
        }
        this.loaded = true;
        this.dirty = false;
        this.lastSaveMillis = System.currentTimeMillis();
    }

    // -- saving --------------------------------------------------------------

    /** Called once per client tick. Writes only when dirty and past the debounce. */
    public void tick() {
        if (!this.dirty || !this.loaded) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastSaveMillis < SAVE_DEBOUNCE_MILLIS) {
            return;
        }
        saveNow();
    }

    /** Writes every section immediately, regardless of the debounce. */
    public void saveNow() {
        if (!this.loaded) {
            return;
        }
        ensureDirectory();
        boolean allOk = true;
        for (ConfigSection section : this.sections) {
            try {
                JsonObject root = new JsonObject();
                section.write(root);
                allOk &= new ConfigFile(this.directory, section.fileName()).save(root);
            } catch (RuntimeException exception) {
                ConfigLog.warn("Failed to serialise " + section.fileName(), exception);
                allOk = false;
            }
        }
        this.lastSaveMillis = System.currentTimeMillis();
        // Keep the flag set on failure so the next tick tries again.
        this.dirty = !allOk;
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(this.directory);
        } catch (IOException exception) {
            ConfigLog.warn("Could not create " + this.directory, exception);
        }
    }
}
