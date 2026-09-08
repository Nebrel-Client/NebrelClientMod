package de.nebrel.client.util;

/**
 * Small numeric helpers shared across the client.
 *
 * <p>Deliberately free of any Minecraft import so it can be compiled and unit
 * checked without the game on the classpath.</p>
 */
public final class NebrelMath {

    private NebrelMath() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Frame-rate independent exponential approach.
     *
     * @param speed fraction of the remaining distance covered per 1/20 s
     */
    public static float approach(float current, float target, float speed, float deltaSeconds) {
        if (deltaSeconds <= 0.0F) {
            return current;
        }
        float factor = 1.0F - (float) Math.pow(1.0F - clamp(speed, 0.0F, 1.0F), deltaSeconds * 20.0F);
        return current + (target - current) * factor;
    }

    /** Snaps {@code value} to the nearest multiple of {@code step}. */
    public static double snap(double value, double step) {
        if (step <= 0.0D) {
            return value;
        }
        return Math.round(value / step) * step;
    }

    /** Maps {@code value} from [inMin, inMax] onto [outMin, outMax]. */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        if (inMax - inMin == 0.0D) {
            return outMin;
        }
        return outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin);
    }

    /** True when (x, y) lies inside the rectangle. */
    public static boolean inside(double x, double y, double left, double top, double width, double height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** Rounds a double to {@code decimals} places, used by sliders and labels. */
    public static double round(double value, int decimals) {
        double factor = Math.pow(10.0D, Math.max(0, decimals));
        return Math.round(value * factor) / factor;
    }
}
