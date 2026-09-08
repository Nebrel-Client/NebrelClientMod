package de.nebrel.client.gui;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.gui.theme.ThemeManager;

/**
 * The shared services every UI component needs: the palette and the user's
 * interface preferences.
 *
 * <p>Passed down explicitly rather than reached for through a static, so a
 * component can be drawn against a different theme (the theme preview in the
 * client settings) without touching global state.</p>
 */
public final class UiContext {

    private final ThemeManager themes;
    private final ClientSettings settings;

    public UiContext(ThemeManager themes, ClientSettings settings) {
        this.themes = themes;
        this.settings = settings;
    }

    public Theme theme() {
        return this.themes.current();
    }

    public ThemeManager themes() {
        return this.themes;
    }

    public ClientSettings settings() {
        return this.settings;
    }

    /** Scales a base animation duration by the user's speed preference. */
    public long duration(long baseMillis) {
        return this.settings.animationDuration(baseMillis);
    }

    public boolean animationsEnabled() {
        return this.settings.animations.get();
    }
}
