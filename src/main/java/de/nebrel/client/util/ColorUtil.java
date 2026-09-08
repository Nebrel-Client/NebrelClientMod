package de.nebrel.client.util;

/**
 * Packed ARGB colour helpers.
 *
 * <p>Colours are stored as {@code 0xAARRGGBB} ints everywhere in the client so
 * they can be handed straight to {@code DrawContext}. This class has no
 * Minecraft dependency.</p>
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    /** Replaces the alpha channel with {@code alpha} (0-255). */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((NebrelMath.clamp(alpha, 0, 255)) << 24);
    }

    /** Multiplies the existing alpha by {@code factor} (0.0-1.0). */
    public static int fadeAlpha(int color, float factor) {
        int a = Math.round(alpha(color) * NebrelMath.clamp(factor, 0.0F, 1.0F));
        return withAlpha(color, a);
    }

    /** Linear interpolation in straight (non-premultiplied) ARGB space. */
    public static int lerp(int from, int to, float t) {
        float f = NebrelMath.clamp(t, 0.0F, 1.0F);
        return argb(
                Math.round(NebrelMath.lerp(alpha(from), alpha(to), f)),
                Math.round(NebrelMath.lerp(red(from), red(to), f)),
                Math.round(NebrelMath.lerp(green(from), green(to), f)),
                Math.round(NebrelMath.lerp(blue(from), blue(to), f)));
    }

    /** Blends {@code overlay} over {@code base} using the overlay's alpha. */
    public static int overlay(int base, int overlay) {
        float a = alpha(overlay) / 255.0F;
        return argb(
                alpha(base),
                Math.round(NebrelMath.lerp(red(base), red(overlay), a)),
                Math.round(NebrelMath.lerp(green(base), green(overlay), a)),
                Math.round(NebrelMath.lerp(blue(base), blue(overlay), a)));
    }

    /** Scales RGB towards white ({@code amount > 0}) or black ({@code amount < 0}). */
    public static int shade(int color, float amount) {
        int target = amount >= 0.0F ? 0xFFFFFF : 0x000000;
        float t = Math.abs(NebrelMath.clamp(amount, -1.0F, 1.0F));
        return argb(
                alpha(color),
                Math.round(NebrelMath.lerp(red(color), (target >> 16) & 0xFF, t)),
                Math.round(NebrelMath.lerp(green(color), (target >> 8) & 0xFF, t)),
                Math.round(NebrelMath.lerp(blue(color), target & 0xFF, t)));
    }

    /** Perceived luminance in the 0.0-1.0 range (ITU-R BT.601 weights). */
    public static float luminance(int color) {
        return (0.299F * red(color) + 0.587F * green(color) + 0.114F * blue(color)) / 255.0F;
    }

    /** Picks black or white text for the given background. */
    public static int readableTextOn(int background) {
        return luminance(background) > 0.55F ? 0xFF101014 : 0xFFF5F5FA;
    }

    public static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * f);
        float t = brightness * (1.0F - saturation * (1.0F - f));

        float r;
        float g;
        float b;
        switch ((int) h) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return argb(255, Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F));
    }

    /** Returns {hue, saturation, brightness}, all in 0.0-1.0. */
    public static float[] rgbToHsb(int color) {
        float r = red(color) / 255.0F;
        float g = green(color) / 255.0F;
        float b = blue(color) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0.0F;
        if (delta > 0.0F) {
            if (max == r) {
                hue = ((g - b) / delta) % 6.0F;
            } else if (max == g) {
                hue = (b - r) / delta + 2.0F;
            } else {
                hue = (r - g) / delta + 4.0F;
            }
            hue /= 6.0F;
            if (hue < 0.0F) {
                hue += 1.0F;
            }
        }
        float saturation = max == 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    /**
     * Time-driven rainbow.
     *
     * @param timeMillis a monotonic clock in milliseconds
     * @param speed      cycles per second
     * @param offset     phase shift in 0.0-1.0, lets callers stagger elements
     */
    public static int rainbow(long timeMillis, float speed, float offset) {
        float hue = (float) ((timeMillis / 1000.0D * speed) + offset);
        return hsbToRgb(hue - (float) Math.floor(hue), 0.75F, 1.0F);
    }

    /** Parses {@code #RRGGBB} or {@code #AARRGGBB}; returns {@code fallback} on bad input. */
    public static int parseHex(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        try {
            if (cleaned.length() == 6) {
                return 0xFF000000 | Integer.parseInt(cleaned, 16);
            }
            if (cleaned.length() == 8) {
                return (int) Long.parseLong(cleaned, 16);
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return fallback;
    }

    public static String toHex(int color) {
        return String.format("#%08X", color);
    }
}
