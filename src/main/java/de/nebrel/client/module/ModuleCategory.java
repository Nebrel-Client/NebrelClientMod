package de.nebrel.client.module;

/**
 * The category a module belongs to.
 *
 * <p>{@link #ALL} and {@link #FAVORITES} are navigation-only: no module ever
 * declares them, they exist so the sidebar can be driven by a single enum.
 * {@link #real()} separates the two groups.</p>
 */
public enum ModuleCategory {

    ALL("All", "▦", true),
    FAVORITES("Favorites", "★", true),

    HUD("HUD", "▤", false),
    VISUAL("Visual", "◈", false),
    PLAYER("Player", "☺", false),
    UTILITY("Utility", "⚙", false),
    WORLD("World", "☷", false),
    RENDER("Render", "◑", false),
    MISC("Misc", "⋯", false);

    private final String displayName;
    private final String icon;
    private final boolean virtual;

    ModuleCategory(String displayName, String icon, boolean virtual) {
        this.displayName = displayName;
        this.icon = icon;
        this.virtual = virtual;
    }

    public String displayName() {
        return this.displayName;
    }

    /** A single glyph used as the sidebar icon. */
    public String icon() {
        return this.icon;
    }

    /** True for navigation-only entries that no module can belong to. */
    public boolean virtual() {
        return this.virtual;
    }

    /** True for categories a module can actually declare. */
    public boolean real() {
        return !this.virtual;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
