package de.nebrel.client.gui.theme;

import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.util.ColorUtil;

/**
 * Holds the active theme and the user's accent choice.
 *
 * <p>Switching theme cross-fades: {@link #current()} returns the destination
 * palette, while {@link #blend(int, int)} lets components mix the outgoing and
 * incoming values for the ~200 ms the transition lasts. Components that do not
 * care simply read {@link #current()} and switch instantly.</p>
 */
public final class ThemeManager {

    private static final long TRANSITION_MILLIS = 220L;

    private Theme base = Themes.DARK;
    private Theme previous = Themes.DARK;
    private Theme active = Themes.DARK.withAccent(Themes.DEFAULT_ACCENT);
    private Theme previousActive = this.active;

    private int accent = Themes.DEFAULT_ACCENT;
    private final Animation transition = new Animation(1.0F, TRANSITION_MILLIS,
            de.nebrel.client.render.animation.Easing.EASE_OUT_CUBIC);

    /** The palette to draw with. */
    public Theme current() {
        return this.active;
    }

    /** The palette being faded out; equals {@link #current()} once settled. */
    public Theme outgoing() {
        return this.previousActive;
    }

    /** 0.0 right after a switch, 1.0 once the cross-fade has finished. */
    public float transitionProgress() {
        return this.transition.value();
    }

    public boolean transitioning() {
        return !this.transition.finished();
    }

    /**
     * Mixes an outgoing and incoming token by the current transition progress.
     *
     * @param from the value read from {@link #outgoing()}
     * @param to   the value read from {@link #current()}
     */
    public int blend(int from, int to) {
        if (this.transition.finished()) {
            return to;
        }
        return ColorUtil.lerp(from, to, this.transition.value());
    }

    public Theme baseTheme() {
        return this.base;
    }

    public int accent() {
        return this.accent;
    }

    public void setTheme(Theme newBase) {
        if (newBase == null || newBase.id().equals(this.base.id())) {
            return;
        }
        this.previous = this.base;
        this.previousActive = this.active;
        this.base = newBase;
        this.active = newBase.withAccent(this.accent);
        this.transition.set(0.0F);
        this.transition.animateTo(1.0F);
    }

    public void setTheme(String id) {
        setTheme(Themes.byId(id));
    }

    /** Switches between the built-in dark and light palettes. */
    public void toggleDarkLight() {
        setTheme(this.base.dark() ? Themes.LIGHT : Themes.DARK);
    }

    /**
     * Applies a new accent.
     *
     * <p>Not animated: the accent is used for small highlights, and cross-fading
     * it makes colour-picker dragging feel laggy.</p>
     */
    public void setAccent(int newAccent) {
        int opaque = newAccent | 0xFF000000;
        if (opaque == this.accent) {
            return;
        }
        this.accent = opaque;
        this.active = this.base.withAccent(opaque);
        this.previousActive = this.active;
    }

    public void resetAccent() {
        setAccent(Themes.DEFAULT_ACCENT);
    }

    /** Restores persisted state without animating. */
    public void restore(String themeId, int accentColor) {
        this.base = Themes.byId(themeId);
        this.previous = this.base;
        this.accent = accentColor | 0xFF000000;
        this.active = this.base.withAccent(this.accent);
        this.previousActive = this.active;
        this.transition.set(1.0F);
    }

    /** Id of the theme that was active before the last switch. */
    public String previousThemeId() {
        return this.previous.id();
    }
}
