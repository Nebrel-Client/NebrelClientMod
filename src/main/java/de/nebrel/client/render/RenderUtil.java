package de.nebrel.client.render;

import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Drawing primitives shared by every Nebrel surface.
 *
 * <p>Everything here is built out of {@code DrawContext.fill} and the vanilla
 * text renderer. That is a deliberate constraint:</p>
 * <ul>
 *   <li>No texture atlas and no custom shader, so there is nothing to bind,
 *       cache or restore, and no render state for a module to leak. Vanilla's
 *       GUI render layer is left exactly as it was found.</li>
 *   <li>No image assets to ship, which keeps the client's look entirely in
 *       code and themeable down to the last pixel.</li>
 * </ul>
 *
 * <p>Rounded corners are drawn as horizontal spans, one per row, with a single
 * alpha-blended pixel at the boundary to soften the step. That is O(radius)
 * fills per corner rather than O(radius squared), so a screen full of cards
 * stays cheap.</p>
 */
public final class RenderUtil {

    private RenderUtil() {
    }

    // -- rectangles ----------------------------------------------------------

    /** Axis-aligned rectangle. Coordinates are left/top/width/height. */
    public static void rect(DrawContext context, float x, float y, float width, float height, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtil.alpha(color) == 0) {
            return;
        }
        context.fill(Math.round(x), Math.round(y),
                Math.round(x + width), Math.round(y + height), color);
    }

    /** One-pixel-thick outline drawn just inside the given bounds. */
    public static void outline(DrawContext context, float x, float y, float width, float height,
                               float thickness, int color) {
        if (thickness <= 0.0F || ColorUtil.alpha(color) == 0) {
            return;
        }
        rect(context, x, y, width, thickness, color);
        rect(context, x, y + height - thickness, width, thickness, color);
        rect(context, x, y + thickness, thickness, height - thickness * 2.0F, color);
        rect(context, x + width - thickness, y + thickness, thickness, height - thickness * 2.0F, color);
    }

    /** Vertical gradient from {@code top} to {@code bottom}. */
    public static void gradientVertical(DrawContext context, float x, float y, float width, float height,
                                        int top, int bottom) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        context.fillGradient(Math.round(x), Math.round(y),
                Math.round(x + width), Math.round(y + height), top, bottom);
    }

    /**
     * Horizontal gradient.
     *
     * <p>{@code fillGradient} only interpolates vertically, so this walks the
     * columns. Used sparingly, for the colour picker's saturation ramp.</p>
     */
    public static void gradientHorizontal(DrawContext context, float x, float y, float width, float height,
                                          int left, int right) {
        int columns = Math.max(1, Math.round(width));
        int startX = Math.round(x);
        int startY = Math.round(y);
        int endY = Math.round(y + height);
        for (int i = 0; i < columns; i++) {
            int color = ColorUtil.lerp(left, right, columns == 1 ? 0.0F : i / (float) (columns - 1));
            context.fill(startX + i, startY, startX + i + 1, endY, color);
        }
    }

    // -- rounded rectangles --------------------------------------------------

    /**
     * Filled rectangle with rounded corners.
     *
     * @param radius corner radius in pixels; clamped to half the shorter side
     */
    public static void roundedRect(DrawContext context, float x, float y, float width, float height,
                                   float radius, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtil.alpha(color) == 0) {
            return;
        }
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);

        int r = (int) NebrelMath.clamp(Math.round(radius), 0, Math.min((right - left) / 2, (bottom - top) / 2));
        if (r <= 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        // Middle band spans the full width; the corner rows are filled below.
        context.fill(left, top + r, right, bottom - r, color);

        for (int row = 0; row < r; row++) {
            // Distance from the corner centre to the row's midline.
            float dy = r - row - 0.5F;
            float halfChord = (float) Math.sqrt(Math.max(0.0D, r * r - dy * dy));
            int inset = r - (int) Math.floor(halfChord);
            float coverage = halfChord - (float) Math.floor(halfChord);

            int rowTop = top + row;
            int rowBottom = bottom - row - 1;

            // Solid span between the two corner insets.
            context.fill(left + inset, rowTop, right - inset, rowTop + 1, color);
            context.fill(left + inset, rowBottom, right - inset, rowBottom + 1, color);

            // Soften the step with a single partially covered pixel per side.
            if (coverage > 0.02F && inset > 0) {
                int soft = ColorUtil.fadeAlpha(color, coverage);
                context.fill(left + inset - 1, rowTop, left + inset, rowTop + 1, soft);
                context.fill(right - inset, rowTop, right - inset + 1, rowTop + 1, soft);
                context.fill(left + inset - 1, rowBottom, left + inset, rowBottom + 1, soft);
                context.fill(right - inset, rowBottom, right - inset + 1, rowBottom + 1, soft);
            }
        }
    }

    /** Rounded outline, one pixel thick. */
    public static void roundedOutline(DrawContext context, float x, float y, float width, float height,
                                      float radius, int color) {
        if (width <= 0.0F || height <= 0.0F || ColorUtil.alpha(color) == 0) {
            return;
        }
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);
        int r = (int) NebrelMath.clamp(Math.round(radius), 0, Math.min((right - left) / 2, (bottom - top) / 2));

        // Straight sides, stopping short of the corner arcs.
        context.fill(left + r, top, right - r, top + 1, color);
        context.fill(left + r, bottom - 1, right - r, bottom, color);
        context.fill(left, top + r, left + 1, bottom - r, color);
        context.fill(right - 1, top + r, right, bottom - r, color);

        for (int row = 0; row < r; row++) {
            float dy = r - row - 0.5F;
            float halfChord = (float) Math.sqrt(Math.max(0.0D, r * r - dy * dy));
            int inset = r - (int) Math.floor(halfChord);
            int rowTop = top + row;
            int rowBottom = bottom - row - 1;

            context.fill(left + inset, rowTop, left + inset + 1, rowTop + 1, color);
            context.fill(right - inset - 1, rowTop, right - inset, rowTop + 1, color);
            context.fill(left + inset, rowBottom, left + inset + 1, rowBottom + 1, color);
            context.fill(right - inset - 1, rowBottom, right - inset, rowBottom + 1, color);
        }
    }

    /**
     * A soft drop shadow behind a panel.
     *
     * <p>Approximated with a few concentric rounded rectangles of decreasing
     * alpha. Cheap, and enough to lift a panel off the world behind it.</p>
     */
    public static void shadow(DrawContext context, float x, float y, float width, float height,
                              float radius, int color, int spread) {
        for (int i = spread; i >= 1; i--) {
            float factor = (float) (spread - i + 1) / (spread + 1);
            int layer = ColorUtil.fadeAlpha(color, factor * factor * 0.6F);
            roundedRect(context, x - i, y - i + 1, width + i * 2.0F, height + i * 2.0F,
                    radius + i, layer);
        }
    }

    /** A horizontal hairline in the divider colour. */
    public static void divider(DrawContext context, float x, float y, float width, int color) {
        rect(context, x, y, width, 1.0F, color);
    }

    // -- text ----------------------------------------------------------------

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int textWidth(String text) {
        return font().getWidth(text);
    }

    public static int lineHeight() {
        return font().fontHeight;
    }

    /** Draws text with a shadow at integer coordinates. */
    public static void text(DrawContext context, String value, float x, float y, int color) {
        context.drawText(font(), value, Math.round(x), Math.round(y), color, true);
    }

    /** Draws text without a shadow; used inside the menu, where shadows read as noise. */
    public static void textFlat(DrawContext context, String value, float x, float y, int color) {
        context.drawText(font(), value, Math.round(x), Math.round(y), color, false);
    }

    /** Draws text horizontally centred on {@code centerX}. */
    public static void textCentered(DrawContext context, String value, float centerX, float y, int color) {
        textFlat(context, value, centerX - textWidth(value) / 2.0F, y, color);
    }

    /** Draws text right-aligned so it ends at {@code rightX}. */
    public static void textRight(DrawContext context, String value, float rightX, float y, int color) {
        textFlat(context, value, rightX - textWidth(value), y, color);
    }

    /**
     * Draws text scaled about its top-left corner.
     *
     * <p>Used for the small secondary labels; the vanilla font has one size, so
     * anything smaller has to go through the matrix stack.</p>
     */
    public static void textScaled(DrawContext context, String value, float x, float y,
                                  float scale, int color, boolean shadow) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(font(), value, 0, 0, color, shadow);
        matrices.pop();
    }

    public static void textScaledCentered(DrawContext context, String value, float centerX, float y,
                                          float scale, int color) {
        float width = textWidth(value) * scale;
        textScaled(context, value, centerX - width / 2.0F, y, scale, color, false);
    }

    /** Width of {@code value} when drawn at {@code scale}. */
    public static float textWidthScaled(String value, float scale) {
        return textWidth(value) * scale;
    }

    /**
     * Shortens {@code value} with a trailing ellipsis so it fits {@code maxWidth}.
     *
     * @return the original string when it already fits
     */
    public static String truncate(String value, int maxWidth) {
        if (value == null || value.isEmpty() || textWidth(value) <= maxWidth) {
            return value == null ? "" : value;
        }
        String ellipsis = "...";
        int budget = maxWidth - textWidth(ellipsis);
        if (budget <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        int used = 0;
        for (int i = 0; i < value.length(); i++) {
            int charWidth = font().getWidth(String.valueOf(value.charAt(i)));
            if (used + charWidth > budget) {
                break;
            }
            used += charWidth;
            builder.append(value.charAt(i));
        }
        return builder.append(ellipsis).toString();
    }

    // -- clipping ------------------------------------------------------------

    /**
     * Pushes a scissor rectangle.
     *
     * <p>{@code DrawContext} keeps its own scissor stack, so these nest and
     * always unwind to whatever was active before. Every call must be paired
     * with {@link #popClip(DrawContext)}.</p>
     */
    public static void pushClip(DrawContext context, float x, float y, float width, float height) {
        context.enableScissor(Math.round(x), Math.round(y),
                Math.round(x + width), Math.round(y + height));
    }

    public static void popClip(DrawContext context) {
        context.disableScissor();
    }

    // -- misc ----------------------------------------------------------------

    /** True when the pointer is inside the rectangle. */
    public static boolean hovered(double mouseX, double mouseY,
                                  float x, float y, float width, float height) {
        return NebrelMath.inside(mouseX, mouseY, x, y, width, height);
    }

    /**
     * A vertical scrollbar track and thumb.
     *
     * @param progress    0.0 at the top, 1.0 at the bottom
     * @param visibleRatio the fraction of the content that fits, sets thumb length
     */
    public static void scrollbar(DrawContext context, float x, float y, float width, float height,
                                 float progress, float visibleRatio, int trackColor, int thumbColor) {
        if (visibleRatio >= 1.0F) {
            return;
        }
        roundedRect(context, x, y, width, height, width / 2.0F, trackColor);
        float thumbHeight = Math.max(width * 2.0F, height * NebrelMath.clamp(visibleRatio, 0.05F, 1.0F));
        float thumbY = y + (height - thumbHeight) * NebrelMath.clamp(progress, 0.0F, 1.0F);
        roundedRect(context, x, thumbY, width, thumbHeight, width / 2.0F, thumbColor);
    }
}
