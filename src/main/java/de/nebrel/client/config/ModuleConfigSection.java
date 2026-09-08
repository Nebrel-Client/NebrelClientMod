package de.nebrel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.Setting;

/**
 * Persists module state: enabled, favourite, and every setting value.
 *
 * <p>Shape:</p>
 * <pre>
 * {
 *   "configVersion": 1,
 *   "modules": {
 *     "crosshair": {
 *       "enabled": true,
 *       "favorite": false,
 *       "settings": { "crosshair.gap": 2.0, "crosshair.color": "#FFFFFFFF" }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Keybinds live in their own file and are skipped here, so rebinding a key
 * does not rewrite the whole module state.</p>
 */
public final class ModuleConfigSection implements ConfigSection {

    private static final String MODULES = "modules";
    private static final String ENABLED = "enabled";
    private static final String FAVORITE = "favorite";
    private static final String SETTINGS = "settings";

    private final ModuleManager modules;

    public ModuleConfigSection(ModuleManager modules) {
        this.modules = modules;
    }

    @Override
    public String fileName() {
        return "modules.json";
    }

    @Override
    public void write(JsonObject root) {
        JsonObject all = new JsonObject();
        for (Module module : this.modules.getModules()) {
            JsonObject entry = new JsonObject();
            entry.addProperty(ENABLED, module.enabled());
            entry.addProperty(FAVORITE, module.favorite());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                if (setting instanceof KeybindSetting) {
                    continue;
                }
                settings.add(setting.id(), setting.write());
            }
            entry.add(SETTINGS, settings);
            all.add(module.id(), entry);
        }
        root.add(MODULES, all);
    }

    @Override
    public void read(JsonObject root) {
        if (!root.has(MODULES) || !root.get(MODULES).isJsonObject()) {
            return;
        }
        JsonObject all = root.getAsJsonObject(MODULES);

        for (Module module : this.modules.getModules()) {
            JsonElement stored = all.get(module.id());
            if (stored == null || !stored.isJsonObject()) {
                // Module added in a newer build: keep its defaults.
                continue;
            }
            JsonObject entry = stored.getAsJsonObject();

            if (entry.has(ENABLED)) {
                // Restore the flag without firing onEnable: the client is not
                // ready for modules to touch the world yet.
                this.modules.restoreEnabled(module, readBoolean(entry, ENABLED, false));
            }
            module.setFavorite(readBoolean(entry, FAVORITE, false));

            if (entry.has(SETTINGS) && entry.get(SETTINGS).isJsonObject()) {
                JsonObject settings = entry.getAsJsonObject(SETTINGS);
                for (Setting<?> setting : module.settings()) {
                    if (setting instanceof KeybindSetting) {
                        continue;
                    }
                    JsonElement value = settings.get(setting.id());
                    if (value != null) {
                        // Each setting swallows malformed input itself.
                        setting.read(value);
                    }
                }
            }
        }
        // Entries in the file for modules that no longer exist are simply not
        // visited here; they disappear on the next write.
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        try {
            JsonElement element = object.get(key);
            return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
