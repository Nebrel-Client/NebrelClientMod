package de.nebrel.client.gui.theme;

import de.nebrel.client.util.ColorUtil;

/**
 * A complete set of colour tokens.
 *
 * <p>Every UI component reads colours from here and nowhere else. No component
 * is allowed to hard-code a literal, which is what makes both runtime theme
 * switching and a user-chosen accent work without touching component code.</p>
 *
 * <p>The accent is stored separately from the rest of the palette:
 * {@link #withAccent(int)} produces a copy with a new accent and its derived
 * hover/pressed/soft variants, leaving the surfaces untouched.</p>
 */
public final class Theme {

    private final String id;
    private final String displayName;
    private final boolean dark;

    // Surfaces, from furthest back to nearest front.
    public final int background;
    public final int surface;
    public final int surfaceElevated;
    public final int surfaceHover;

    // Text.
    public final int textPrimary;
    public final int textSecondary;
    public final int textDisabled;

    // Accent and its states.
    public final int accent;
    public final int accentHover;
    public final int accentPressed;
    /** Very low alpha accent, for active-row backgrounds and subtle borders. */
    public final int accentSoft;

    // Lines.
    public final int border;
    public final int divider;

    // Status.
    public final int success;
    public final int warning;
    public final int error;

    /** Scrim drawn over the world behind the menu. */
    public final int scrim;
    /** Drop shadow colour for panels. */
    public final int shadow;

    private Theme(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.dark = builder.dark;
        this.background = builder.background;
        this.surface = builder.surface;
        this.surfaceElevated = builder.surfaceElevated;
        this.surfaceHover = builder.surfaceHover;
        this.textPrimary = builder.textPrimary;
        this.textSecondary = builder.textSecondary;
        this.textDisabled = builder.textDisabled;
        this.accent = builder.accent;
        this.accentHover = builder.accentHover;
        this.accentPressed = builder.accentPressed;
        this.accentSoft = builder.accentSoft;
        this.border = builder.border;
        this.divider = builder.divider;
        this.success = builder.success;
        this.warning = builder.warning;
        this.error = builder.error;
        this.scrim = builder.scrim;
        this.shadow = builder.shadow;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public boolean dark() {
        return this.dark;
    }

    /**
     * A copy of this theme with a different accent.
     *
     * <p>Hover and pressed states are derived by shifting the accent towards
     * white and black respectively, so any user-picked colour gets a sensible
     * interaction ramp for free.</p>
     */
    public Theme withAccent(int newAccent) {
        int opaque = newAccent | 0xFF000000;
        return toBuilder()
                .accent(opaque)
                .accentHover(ColorUtil.shade(opaque, this.dark ? 0.18F : 0.12F))
                .accentPressed(ColorUtil.shade(opaque, this.dark ? -0.16F : -0.10F))
                .accentSoft(ColorUtil.withAlpha(opaque, this.dark ? 38 : 30))
                .build();
    }

    private Builder toBuilder() {
        return new Builder(this.id, this.displayName, this.dark)
                .background(this.background)
                .surface(this.surface)
                .surfaceElevated(this.surfaceElevated)
                .surfaceHover(this.surfaceHover)
                .textPrimary(this.textPrimary)
                .textSecondary(this.textSecondary)
                .textDisabled(this.textDisabled)
                .accent(this.accent)
                .accentHover(this.accentHover)
                .accentPressed(this.accentPressed)
                .accentSoft(this.accentSoft)
                .border(this.border)
                .divider(this.divider)
                .success(this.success)
                .warning(this.warning)
                .error(this.error)
                .scrim(this.scrim)
                .shadow(this.shadow);
    }

    public static Builder builder(String id, String displayName, boolean dark) {
        return new Builder(id, displayName, dark);
    }

    /** Fluent builder; every token must be set explicitly. */
    public static final class Builder {
        private final String id;
        private final String displayName;
        private final boolean dark;

        private int background;
        private int surface;
        private int surfaceElevated;
        private int surfaceHover;
        private int textPrimary;
        private int textSecondary;
        private int textDisabled;
        private int accent;
        private int accentHover;
        private int accentPressed;
        private int accentSoft;
        private int border;
        private int divider;
        private int success;
        private int warning;
        private int error;
        private int scrim;
        private int shadow;

        private Builder(String id, String displayName, boolean dark) {
            this.id = id;
            this.displayName = displayName;
            this.dark = dark;
        }

        public Builder background(int v) { this.background = v; return this; }
        public Builder surface(int v) { this.surface = v; return this; }
        public Builder surfaceElevated(int v) { this.surfaceElevated = v; return this; }
        public Builder surfaceHover(int v) { this.surfaceHover = v; return this; }
        public Builder textPrimary(int v) { this.textPrimary = v; return this; }
        public Builder textSecondary(int v) { this.textSecondary = v; return this; }
        public Builder textDisabled(int v) { this.textDisabled = v; return this; }
        public Builder accent(int v) { this.accent = v; return this; }
        public Builder accentHover(int v) { this.accentHover = v; return this; }
        public Builder accentPressed(int v) { this.accentPressed = v; return this; }
        public Builder accentSoft(int v) { this.accentSoft = v; return this; }
        public Builder border(int v) { this.border = v; return this; }
        public Builder divider(int v) { this.divider = v; return this; }
        public Builder success(int v) { this.success = v; return this; }
        public Builder warning(int v) { this.warning = v; return this; }
        public Builder error(int v) { this.error = v; return this; }
        public Builder scrim(int v) { this.scrim = v; return this; }
        public Builder shadow(int v) { this.shadow = v; return this; }

        public Theme build() {
            return new Theme(this);
        }
    }
}
