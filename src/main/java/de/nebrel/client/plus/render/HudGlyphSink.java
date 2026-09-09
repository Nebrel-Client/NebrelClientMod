package de.nebrel.client.plus.render;

import de.nebrel.client.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/**
 * Draws glyphs in screen space, for the tab list, the chat and the preview.
 *
 * <p>Scale and lean are applied about each glyph's own centre, so a growing or
 * skewing name expands around its baseline rather than drifting to one side.
 * Every transform is pushed and popped per glyph, which keeps the matrix stack
 * balanced no matter how many transform effects are combined.</p>
 */
public final class HudGlyphSink implements GlyphSink {

    private DrawContext context;
    private boolean shadow = true;

    /** Points the sink at a draw context. Reused between frames. */
    public HudGlyphSink bind(DrawContext context, boolean shadow) {
        this.context = context;
        this.shadow = shadow;
        return this;
    }

    @Override
    public void glyph(String text, float x, float y, float scale, float skew, int color,
                      int background) {
        if (this.context == null || text == null || text.isEmpty()) {
            return;
        }
        float glyphWidth = RenderUtil.textWidth(text);
        float glyphHeight = RenderUtil.lineHeight();

        if (background != 0) {
            RenderUtil.roundedRect(this.context, x - 1.0F, y - 1.0F,
                    glyphWidth + 2.0F, glyphHeight + 1.0F, 2.0F, background);
        }

        boolean transformed = scale != 1.0F || skew != 0.0F;
        if (!transformed) {
            if (this.shadow) {
                RenderUtil.text(this.context, text, x, y, color);
            } else {
                RenderUtil.textFlat(this.context, text, x, y, color);
            }
            return;
        }

        MatrixStack matrices = this.context.getMatrices();
        matrices.push();
        // Move to the glyph's centre, transform there, then move back, so the
        // glyph grows and leans in place instead of sliding away from it.
        float centerX = x + glyphWidth / 2.0F;
        float centerY = y + glyphHeight / 2.0F;
        matrices.translate(centerX, centerY, 0.0F);
        if (skew != 0.0F) {
            matrices.multiply(new Quaternionf().rotationZ(skew));
        }
        if (scale != 1.0F) {
            matrices.scale(scale, scale, 1.0F);
        }
        matrices.translate(-centerX, -centerY, 0.0F);

        if (this.shadow) {
            RenderUtil.text(this.context, text, x, y, color);
        } else {
            RenderUtil.textFlat(this.context, text, x, y, color);
        }
        matrices.pop();
    }

    @Override
    public void plate(float x, float y, float width, float height, float radius, int color) {
        if (this.context != null && color != 0) {
            RenderUtil.roundedRect(this.context, x, y, width, height, radius, color);
        }
    }

    @Override
    public void plateOutline(float x, float y, float width, float height, float radius, int color) {
        if (this.context != null && color != 0) {
            RenderUtil.roundedOutline(this.context, x, y, width, height, radius, color);
        }
    }

    @Override
    public float width(String text) {
        return RenderUtil.textWidth(text);
    }

    @Override
    public float lineHeight() {
        return RenderUtil.lineHeight();
    }
}
