package de.nebrel.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.nebrel.client.util.NebrelMath;

/**
 * A numeric value constrained to [min, max] and quantised to {@code step}.
 *
 * <p>Values are held as doubles; {@link #getInt()} and {@link #getFloat()} are
 * provided so call sites do not have to cast on every frame.</p>
 */
public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final int decimals;
    private String unit = "";

    public NumberSetting(String id, String name, String description,
                         double defaultValue, double min, double max, double step) {
        super(id, name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 0.01D : step;
        this.decimals = decimalsFor(this.step);
        // Re-run the constructor value through the clamp.
        set(defaultValue);
    }

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step) {
        this(id, name, "", defaultValue, min, max, step);
    }

    private static int decimalsFor(double step) {
        if (step >= 1.0D) {
            return 0;
        }
        if (step >= 0.1D) {
            return 1;
        }
        if (step >= 0.01D) {
            return 2;
        }
        return 3;
    }

    @Override
    protected Double sanitise(Double candidate) {
        if (candidate == null || candidate.isNaN() || candidate.isInfinite()) {
            return defaultValue();
        }
        double snapped = NebrelMath.snap(candidate, this.step);
        return NebrelMath.round(NebrelMath.clamp(snapped, this.min, this.max), this.decimals);
    }

    public double min() {
        return this.min;
    }

    public double max() {
        return this.max;
    }

    public double step() {
        return this.step;
    }

    public int decimals() {
        return this.decimals;
    }

    public int getInt() {
        return (int) Math.round(get());
    }

    public float getFloat() {
        return get().floatValue();
    }

    /** Position of the current value on the slider track, 0.0-1.0. */
    public float fraction() {
        if (this.max - this.min == 0.0D) {
            return 0.0F;
        }
        return (float) NebrelMath.clamp((get() - this.min) / (this.max - this.min), 0.0D, 1.0D);
    }

    /** Sets the value from a 0.0-1.0 slider position. */
    public void setFraction(double fraction) {
        set(this.min + NebrelMath.clamp(fraction, 0.0D, 1.0D) * (this.max - this.min));
    }

    public NumberSetting unit(String suffix) {
        this.unit = suffix == null ? "" : suffix;
        return this;
    }

    public String unit() {
        return this.unit;
    }

    @Override
    public JsonElement write() {
        return new JsonPrimitive(get());
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        try {
            set(element.getAsDouble());
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        String number = this.decimals == 0
                ? String.valueOf(getInt())
                : String.format("%." + this.decimals + "f", get());
        return this.unit.isEmpty() ? number : number + this.unit;
    }
}
