package de.nebrel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.SettingSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-wide preferences, shown on the menu's own settings page.
 *
 * <p>Built out of the same {@link Setting} types the modules use, so the
 * settings view renders it with the same components instead of needing a
 * bespoke screen.</p>
 */
public final class ClientSettings implements ConfigSection {

    /** How the menu background is treated behind the panel. */
    public enum BackgroundStyle {
        DIM("Dim"),
        BLUR("Blur"),
        NONE("None");

        private final String label;

        BackgroundStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    /** GLFW_KEY_RIGHT_SHIFT. */
    public static final int DEFAULT_MENU_KEY = 344;

    private final List<Setting<?>> settings = new ArrayList<>();

    public final KeybindSetting menuKey;
    public final EnumSetting<BackgroundStyle> backgroundStyle;
    public final NumberSetting backgroundStrength;
    public final BooleanSetting animations;
    public final NumberSetting animationSpeed;
    public final BooleanSetting smoothScrolling;
    public final NumberSetting menuScale;
    public final BooleanSetting showDescriptions;
    public final BooleanSetting notifications;
    public final BooleanSetting hudEditorGrid;
    public final NumberSetting hudEditorGridSize;

    public ClientSettings() {
        this.menuKey = add(new KeybindSetting("client.menuKey", "Open Menu",
                "Opens the Nebrel Client menu", DEFAULT_MENU_KEY));

        this.backgroundStyle = add(new EnumSetting<>("client.backgroundStyle", "Background",
                "How the world behind the menu is treated", BackgroundStyle.DIM));
        this.backgroundStyle.section(SettingSection.APPEARANCE);

        this.backgroundStrength = add(new NumberSetting("client.backgroundStrength", "Background Strength",
                "Dim amount, or blur radius when blur is selected", 1.0D, 0.0D, 2.0D, 0.05D));
        this.backgroundStrength.section(SettingSection.APPEARANCE);
        this.backgroundStrength.visibleWhen(() -> !this.backgroundStyle.is(BackgroundStyle.NONE));

        this.menuScale = add(new NumberSetting("client.menuScale", "Menu Scale",
                "Size of the client menu relative to the window", 1.0D, 0.75D, 1.35D, 0.05D));
        this.menuScale.section(SettingSection.APPEARANCE);

        this.showDescriptions = add(new BooleanSetting("client.showDescriptions", "Card Descriptions",
                "Show the one-line description on module cards", true));
        this.showDescriptions.section(SettingSection.APPEARANCE);

        this.animations = add(new BooleanSetting("client.animations", "Animations",
                "Animate menu transitions, hovers and toggles", true));
        this.animations.section(SettingSection.ANIMATION);

        this.animationSpeed = add(new NumberSetting("client.animationSpeed", "Animation Speed",
                "Multiplier for every UI transition", 1.0D, 0.5D, 2.0D, 0.05D));
        this.animationSpeed.section(SettingSection.ANIMATION);
        this.animationSpeed.visibleWhen(this.animations);

        this.smoothScrolling = add(new BooleanSetting("client.smoothScrolling", "Smooth Scrolling",
                "Ease scrolling instead of jumping to the new offset", true));
        this.smoothScrolling.section(SettingSection.ANIMATION);

        this.notifications = add(new BooleanSetting("client.notifications", "Notifications",
                "Show toast notifications for client events", true));
        this.notifications.section(SettingSection.BEHAVIOR);

        this.hudEditorGrid = add(new BooleanSetting("client.hudEditorGrid", "HUD Editor Grid",
                "Draw a grid in the HUD editor", false));
        this.hudEditorGrid.section(SettingSection.BEHAVIOR);

        this.hudEditorGridSize = add(new NumberSetting("client.hudEditorGridSize", "Grid Size",
                "Grid spacing in pixels", 8.0D, 2.0D, 32.0D, 1.0D));
        this.hudEditorGridSize.section(SettingSection.BEHAVIOR);
        this.hudEditorGridSize.visibleWhen(this.hudEditorGrid);
    }

    private <S extends Setting<?>> S add(S setting) {
        this.settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() {
        return List.copyOf(this.settings);
    }

    public List<Setting<?>> visibleSettings() {
        List<Setting<?>> result = new ArrayList<>(this.settings.size());
        for (Setting<?> setting : this.settings) {
            if (setting.visible()) {
                result.add(setting);
            }
        }
        return result;
    }

    /**
     * Scales an animation duration by the user's speed preference.
     *
     * @return the duration to use, or 1 ms when animations are off
     */
    public long animationDuration(long baseMillis) {
        if (!this.animations.get()) {
            return 1L;
        }
        double speed = this.animationSpeed.get();
        return Math.max(1L, Math.round(baseMillis / Math.max(0.1D, speed)));
    }

    // -- persistence ---------------------------------------------------------

    @Override
    public String fileName() {
        return "general.json";
    }

    @Override
    public void write(JsonObject root) {
        JsonObject values = new JsonObject();
        for (Setting<?> setting : this.settings) {
            values.add(setting.id(), setting.write());
        }
        root.add("general", values);
    }

    @Override
    public void read(JsonObject root) {
        if (!root.has("general") || !root.get("general").isJsonObject()) {
            return;
        }
        JsonObject values = root.getAsJsonObject("general");
        for (Setting<?> setting : this.settings) {
            JsonElement stored = values.get(setting.id());
            if (stored != null) {
                setting.read(stored);
            }
        }
    }
}
