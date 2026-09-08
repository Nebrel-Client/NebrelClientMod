package de.nebrel.client.module;

import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.RangeSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.StringSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base class for every feature the user can switch on.
 *
 * <p>Deliberately contains no Minecraft types. A module that needs to draw or
 * tick declares that by implementing one of the marker interfaces in
 * {@code de.nebrel.client.module.hook}; the dispatcher then only ever iterates
 * the modules that actually asked for a given hook, instead of calling an empty
 * {@code onRender} on all thirty-odd of them every frame.</p>
 */
public abstract class Module {

    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final String icon;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);

    private final KeybindSetting keybind;

    private boolean enabled;
    private boolean favorite;

    /** Cached lowercase haystack for the search box; rebuilt when settings change. */
    private String searchIndex;

    protected Module(String id, String name, String description, ModuleCategory category, String icon) {
        if (category != null && category.virtual()) {
            throw new IllegalArgumentException(
                    "Module " + id + " cannot declare the navigation-only category " + category);
        }
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.category = category;
        this.icon = icon == null ? "◆" : icon;
        this.keybind = new KeybindSetting(id + ".keybind", "Toggle Key",
                "Key that toggles " + name, KeybindSetting.UNBOUND);
        register(this.keybind);
    }

    protected Module(String id, String name, String description, ModuleCategory category) {
        this(id, name, description, category, null);
    }

    // -- identity ------------------------------------------------------------

    /** Stable identifier. Never change it: configs are keyed on this. */
    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
    }

    public final String description() {
        return this.description;
    }

    public final ModuleCategory category() {
        return this.category;
    }

    public final String icon() {
        return this.icon;
    }

    // -- state ---------------------------------------------------------------

    public final boolean enabled() {
        return this.enabled;
    }

    public final boolean favorite() {
        return this.favorite;
    }

    public final void setFavorite(boolean value) {
        this.favorite = value;
    }

    public final KeybindSetting keybind() {
        return this.keybind;
    }

    /**
     * Switches the module on or off and fires the lifecycle callback.
     *
     * <p>Silently ignores a no-op so {@code onEnable} is never run twice.</p>
     */
    public final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public final void toggle() {
        setEnabled(!this.enabled);
    }

    /**
     * Applies a persisted enabled flag without running the lifecycle callback.
     *
     * <p>Used while loading the config, before the game is in a state where a
     * module may touch the world. {@link ModuleManager#activateLoadedModules()}
     * runs the callbacks afterwards.</p>
     */
    final void restoreEnabled(boolean value) {
        this.enabled = value;
    }

    // -- lifecycle -----------------------------------------------------------

    /** Called when the module switches on. Default does nothing. */
    protected void onEnable() {
    }

    /**
     * Called when the module switches off.
     *
     * <p>Implementations must undo anything they changed globally (gamma, FOV,
     * cached state), because the user expects the game to look exactly as it
     * did before.</p>
     */
    protected void onDisable() {
    }

    // -- settings ------------------------------------------------------------

    /** Registers a setting and returns it, so fields can be assigned inline. */
    protected final <S extends Setting<?>> S register(S setting) {
        this.settings.add(setting);
        this.searchIndex = null;
        return setting;
    }

    protected final BooleanSetting bool(String id, String name, String description, boolean value) {
        return register(new BooleanSetting(qualify(id), name, description, value));
    }

    protected final NumberSetting number(String id, String name, String description,
                                         double value, double min, double max, double step) {
        return register(new NumberSetting(qualify(id), name, description, value, min, max, step));
    }

    protected final <E extends Enum<E>> EnumSetting<E> choice(String id, String name,
                                                              String description, E value) {
        return register(new EnumSetting<>(qualify(id), name, description, value));
    }

    protected final ColorSetting color(String id, String name, String description,
                                       int value, boolean alpha) {
        return register(new ColorSetting(qualify(id), name, description, value, alpha));
    }

    protected final StringSetting text(String id, String name, String description,
                                       String value, int maxLength) {
        return register(new StringSetting(qualify(id), name, description, value, maxLength));
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    protected final <E extends Enum<E>> MultiSelectSetting<E> multi(String id, String name,
                                                                    String description,
                                                                    Class<E> type, E... defaults) {
        return register(new MultiSelectSetting<>(qualify(id), name, description, type, defaults));
    }

    protected final RangeSetting range(String id, String name, String description,
                                       double low, double high,
                                       double min, double max, double step) {
        return register(new RangeSetting(qualify(id), name, description, low, high, min, max, step));
    }

    // Section-taking overloads. Setting#section returns the base type, so
    // chaining it would erase the concrete setting type at the assignment;
    // passing the section in keeps module definitions to one line each.

    protected final BooleanSetting bool(String id, String name, String description,
                                        boolean value, String section) {
        BooleanSetting setting = bool(id, name, description, value);
        setting.section(section);
        return setting;
    }

    protected final NumberSetting number(String id, String name, String description,
                                         double value, double min, double max, double step,
                                         String section) {
        NumberSetting setting = number(id, name, description, value, min, max, step);
        setting.section(section);
        return setting;
    }

    protected final <E extends Enum<E>> EnumSetting<E> choice(String id, String name,
                                                              String description, E value,
                                                              String section) {
        EnumSetting<E> setting = choice(id, name, description, value);
        setting.section(section);
        return setting;
    }

    protected final ColorSetting color(String id, String name, String description,
                                       int value, boolean alpha, String section) {
        ColorSetting setting = color(id, name, description, value, alpha);
        setting.section(section);
        return setting;
    }

    protected final StringSetting text(String id, String name, String description,
                                       String value, int maxLength, String section) {
        StringSetting setting = text(id, name, description, value, maxLength);
        setting.section(section);
        return setting;
    }

    private String qualify(String settingId) {
        return this.id + "." + settingId;
    }

    public final List<Setting<?>> settings() {
        return this.settingsView;
    }

    /** Only the settings the settings view should currently draw. */
    public final List<Setting<?>> visibleSettings() {
        List<Setting<?>> result = new ArrayList<>(this.settings.size());
        for (Setting<?> setting : this.settings) {
            if (setting.visible()) {
                result.add(setting);
            }
        }
        return result;
    }

    /** True when the module exposes anything beyond the implicit keybind. */
    public final boolean hasConfigurableSettings() {
        for (Setting<?> setting : this.settings) {
            if (setting != this.keybind) {
                return true;
            }
        }
        return false;
    }

    public final void resetSettings() {
        for (Setting<?> setting : this.settings) {
            setting.reset();
        }
    }

    // -- search --------------------------------------------------------------

    /**
     * Matches a search query against the module name, description and the names
     * of its settings.
     *
     * <p>The haystack is built once and cached, so typing in the search box
     * does not re-concatenate every module's strings on every keystroke.</p>
     */
    public final boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (this.searchIndex == null) {
            StringBuilder builder = new StringBuilder(128);
            builder.append(this.name.toLowerCase(Locale.ROOT)).append(' ')
                    .append(this.description.toLowerCase(Locale.ROOT)).append(' ')
                    .append(this.id.toLowerCase(Locale.ROOT)).append(' ')
                    .append(this.category.displayName().toLowerCase(Locale.ROOT));
            for (Setting<?> setting : this.settings) {
                builder.append(' ').append(setting.name().toLowerCase(Locale.ROOT));
            }
            this.searchIndex = builder.toString();
        }
        return this.searchIndex.contains(query.toLowerCase(Locale.ROOT).trim());
    }

    @Override
    public String toString() {
        return "Module[" + this.id + "]";
    }
}
