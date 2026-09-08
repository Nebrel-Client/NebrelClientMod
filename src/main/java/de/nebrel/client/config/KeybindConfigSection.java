package de.nebrel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.Setting;

/**
 * Persists every {@link KeybindSetting} in the client, in its own file.
 *
 * <p>Kept apart from {@code modules.json} so a user can share or reset their
 * key layout without touching module settings.</p>
 *
 * <pre>
 * { "configVersion": 1, "keybinds": { "crosshair.keybind": 67 } }
 * </pre>
 */
public final class KeybindConfigSection implements ConfigSection {

    private static final String KEYBINDS = "keybinds";

    private final ModuleManager modules;

    public KeybindConfigSection(ModuleManager modules) {
        this.modules = modules;
    }

    @Override
    public String fileName() {
        return "keybinds.json";
    }

    @Override
    public void write(JsonObject root) {
        JsonObject binds = new JsonObject();
        for (Module module : this.modules.getModules()) {
            for (Setting<?> setting : module.settings()) {
                if (setting instanceof KeybindSetting keybind) {
                    binds.add(keybind.id(), keybind.write());
                }
            }
        }
        root.add(KEYBINDS, binds);
    }

    @Override
    public void read(JsonObject root) {
        if (!root.has(KEYBINDS) || !root.get(KEYBINDS).isJsonObject()) {
            return;
        }
        JsonObject binds = root.getAsJsonObject(KEYBINDS);
        for (Module module : this.modules.getModules()) {
            for (Setting<?> setting : module.settings()) {
                if (setting instanceof KeybindSetting keybind) {
                    JsonElement value = binds.get(keybind.id());
                    if (value != null) {
                        keybind.read(value);
                    }
                }
            }
        }
    }
}
