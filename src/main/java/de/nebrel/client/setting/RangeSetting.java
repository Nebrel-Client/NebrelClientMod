package de.nebrel.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import de.nebrel.client.util.NebrelMath;

/**
 * A low/high pair inside fixed bounds, drawn as a two-handled slider.
 *
 * <p>Used where a module needs a window rather than a single number, for
 * example the distance band a health indicator is shown in.</p>
 */
public final class RangeSetting extends Setting<double[]> {

    private final double min;
    private final double max;
    private final double step;
    private final int decimals;
    private String unit = "";

    public RangeSetting(String id, String name, String description,
                        double defaultLow, double defaultHigh,
                        double min, double max, double step) {
        super(id, name, description, new double[]{defaultLow, defaultHigh});
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 0.01D : step;
        this.decimals = this.step >= 1.0D ? 0 : (this.step >= 0.1D ? 1 : 2);
        set(new double[]{defaultLow, defaultHigh});
    }

    public double low() {
        return get()[0];
    }

    public double high() {
        return get()[1];
    }

    public void setLow(double value) {
        set(new double[]{value, high()});
    }

    public void setHigh(double value) {
        set(new double[]{low(), value});
    }

    public boolean contains(double value) {
        return value >= low() && value <= high();
    }

    public double min() {
        return this.min;
    }

    public double max() {
        return this.max;
    }

    public RangeSetting unit(String suffix) {
        this.unit = suffix == null ? "" : suffix;
        return this;
    }

    public float lowFraction() {
        return fractionOf(low());
    }

    public float highFraction() {
        return fractionOf(high());
    }

    private float fractionOf(double value) {
        if (this.max - this.min == 0.0D) {
            return 0.0F;
        }
        return (float) NebrelMath.clamp((value - this.min) / (this.max - this.min), 0.0D, 1.0D);
    }

    @Override
    protected double[] sanitise(double[] candidate) {
        if (candidate == null || candidate.length != 2) {
            return new double[]{this.min, this.max};
        }
        double low = quantise(candidate[0]);
        double high = quantise(candidate[1]);
        // Keep the pair ordered rather than rejecting a crossed drag.
        if (low > high) {
            double swap = low;
            low = high;
            high = swap;
        }
        return new double[]{low, high};
    }

    private double quantise(double raw) {
        double snapped = NebrelMath.snap(raw, this.step);
        return NebrelMath.round(NebrelMath.clamp(snapped, this.min, this.max), this.decimals);
    }

    @Override
    public void set(double[] newValue) {
        // double[] does not implement value equality, so Setting's identity
        // short-circuit would never fire. Compare the contents instead.
        double[] sanitised = sanitise(newValue);
        double[] current = get();
        if (current != null && current.length == 2
                && current[0] == sanitised[0] && current[1] == sanitised[1]) {
            return;
        }
        super.set(sanitised);
    }

    @Override
    public boolean isDefault() {
        double[] normalised = sanitise(defaultValue());
        double[] current = get();
        return current != null && current.length == 2
                && current[0] == normalised[0] && current[1] == normalised[1];
    }

    @Override
    public JsonElement write() {
        JsonArray array = new JsonArray();
        array.add(low());
        array.add(high());
        return array;
    }

    @Override
    public void read(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 2) {
            return;
        }
        try {
            set(new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble()});
        } catch (RuntimeException ignored) {
            // keep the previous value on malformed input
        }
    }

    @Override
    public String displayValue() {
        String format = this.decimals == 0 ? "%.0f" : "%." + this.decimals + "f";
        return String.format(format, low()) + this.unit + " - "
                + String.format(format, high()) + this.unit;
    }
}
