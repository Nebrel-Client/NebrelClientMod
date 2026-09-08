package de.nebrel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.nebrel.client.gui.theme.ThemeManager;
import de.nebrel.client.gui.theme.Themes;
import de.nebrel.client.util.ColorUtil;

/**
 * Persists the selected theme and the user's accent colour.
 *
 * <pre>
 * { "configVersion": 1, "theme": "nebrel_dark", "accent": "#FF8B5CF6" }
 * </pre>
 */
public final class ThemeConfigSection implements ConfigSection {

    private static final String THEME = "theme";
    private static final String ACCENT = "accent";

    private final ThemeManager themes;

    public ThemeConfigSection(ThemeManager themes) {
        this.themes = themes;
    }

    @Override
    public String fileName() {
        return "theme.json";
    }

    @Override
    public void write(JsonObject root) {
        root.addProperty(THEME, this.themes.baseTheme().id());
        root.addProperty(ACCENT, ColorUtil.toHex(this.themes.accent()));
    }

    @Override
    public void read(JsonObject root) {
        String themeId = Themes.DARK.id();
        JsonElement storedTheme = root.get(THEME);
        if (storedTheme != null && storedTheme.isJsonPrimitive()) {
            try {
                themeId = storedTheme.getAsString();
            } catch (RuntimeException ignored) {
                // keep the default
            }
        }

        int accent = Themes.DEFAULT_ACCENT;
        JsonElement storedAccent = root.get(ACCENT);
        if (storedAccent != null && storedAccent.isJsonPrimitive()) {
            try {
                accent = storedAccent.getAsJsonPrimitive().isNumber()
                        ? storedAccent.getAsInt()
                        : ColorUtil.parseHex(storedAccent.getAsString(), Themes.DEFAULT_ACCENT);
            } catch (RuntimeException ignored) {
                // keep the default
            }
        }

        // Restore without animating: this runs before the first frame.
        this.themes.restore(themeId, accent);
    }
}
