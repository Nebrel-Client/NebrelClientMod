package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.nebrel.client.util.ColorUtil;

/**
 * A packed ARGB colour, optionally with a rainbow mode.
 *
 * <p>When rainbow is enabled the stored colour is left untouched and
 * {@link #effective()} returns a time-driven hue instead, so switching rainbow
 * off restores exactly the colour the user picked.</p>
 */
public final class ColorSetting extends Setting<Integer> {

    private final boolean supportsAlpha;
    private boolean rainbow;
    private float rainbowSpeed = 0.2F;
    private float rainbowOffset;

    public ColorSetting(String id, String name, String description, int defaultValue, boolean supportsAlpha) {
        super(id, name, description, defaultValue);
        this.supportsAlpha = supportsAlpha;
        // Run the declared default through the alpha rule now that the flag is
        // set, so an opaque-only setting never starts out transparent.
        set(defaultValue);
    }

    public ColorSetting(String id, String name, int defaultValue) {
        this(id, name, "", defaultValue, true);
    }

    public boolean supportsAlpha() {
        return this.supportsAlpha;
    }

    @Override
    protected Integer sanitise(Integer candidate) {
        if (candidate == null) {
            return defaultValue();
        }
        return this.supportsAlpha ? candidate : (candidate | 0xFF000000);
    }

    // -- rainbow -------------------------------------------------------------

    public ColorSetting rainbowCapable(float speed) {
        this.rainbowSpeed = speed;
        return this;
    }

    public boolean rainbow() {
        return this.rainbow;
    }

    public void setRainbow(boolean enabled) {
        this.rainbow = enabled;
    }

    public void setRainbowSpeed(float speed) {
        this.rainbowSpeed = speed;
    }

    public void setRainbowOffset(float offset) {
        this.rainbowOffset = offset;
    }

    /**
     * The colour to actually draw with this frame.
     *
     * <p>Keeps the picked alpha when cycling hues, so a translucent element
     * stays translucent in rainbow mode.</p>
     */
    public int effective() {
        if (!this.rainbow) {
            return get();
        }
        int hue = ColorUtil.rainbow(System.currentTimeMillis(), this.rainbowSpeed, this.rainbowOffset);
        return ColorUtil.withAlpha(hue, ColorUtil.alpha(get()));
    }

    /** Replaces the RGB channels, keeping the current alpha. */
    public void setRgb(int rgb) {
        set(ColorUtil.withAlpha(rgb, ColorUtil.alpha(get())));
    }

    /** Replaces only the alpha channel. */
    public void setAlpha(int alpha) {
        set(ColorUtil.withAlpha(get(), alpha));
    }

    @Override
    public JsonElement write() {
        return new JsonPrimitive(ColorUtil.toHex(get()));
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        try {
            // Accept both the "#AARRGGBB" form written above and a raw int, so
            // configs written by an older build still load.
            if (element.getAsJsonPrimitive().isNumber()) {
                set(element.getAsInt());
            } else {
                set(ColorUtil.parseHex(element.getAsString(), get()));
            }
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        return ColorUtil.toHex(get());
    }
}
