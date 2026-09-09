package de.nebrel.client.plus.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Draws glyphs in world space, for the nametag above a player.
 *
 * <p>The caller has already translated and rotated the matrix stack to face the
 * camera, so this works in the flat text space that leaves behind and applies
 * only the per-glyph transform.</p>
 *
 * <p>Plates go through the text renderer's own background colour rather than
 * custom geometry. That reuses the layer vanilla already uses for nametag
 * backgrounds — correct sorting and transparency for free, and nothing extra to
 * bind or restore.</p>
 */
public final class WorldGlyphSink implements GlyphSink {

    private MatrixStack matrices;
    private VertexConsumerProvider consumers;
    private int light;
    private boolean shadow;

    public WorldGlyphSink bind(MatrixStack matrices, VertexConsumerProvider consumers,
                               int light, boolean shadow) {
        this.matrices = matrices;
        this.consumers = consumers;
        this.light = light;
        this.shadow = shadow;
        return this;
    }

    private static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    @Override
    public void glyph(String text, float x, float y, float scale, float skew, int color,
                      int background) {
        if (this.matrices == null || this.consumers == null || text == null || text.isEmpty()) {
            return;
        }
        TextRenderer font = font();
        float glyphWidth = font.getWidth(text);
        float glyphHeight = font.fontHeight;

        this.matrices.push();

        if (scale != 1.0F || skew != 0.0F) {
            float centerX = x + glyphWidth / 2.0F;
            float centerY = y + glyphHeight / 2.0F;
            this.matrices.translate(centerX, centerY, 0.0F);
            if (skew != 0.0F) {
                this.matrices.multiply(new Quaternionf().rotationZ(skew));
            }
            if (scale != 1.0F) {
                this.matrices.scale(scale, scale, 1.0F);
            }
            this.matrices.translate(-centerX, -centerY, 0.0F);
        }

        Matrix4f matrix = this.matrices.peek().getPositionMatrix();
        font.draw(text, x, y, color, this.shadow, matrix, this.consumers,
                TextRenderer.TextLayerType.NORMAL, background, this.light);

        this.matrices.pop();
    }

    @Override
    public void plate(float x, float y, float width, float height, float radius, int color) {
        // World space has no cheap rounded rectangle. The badge passes its plate
        // through the glyph's background colour instead, which uses vanilla's
        // own nametag background layer.
    }

    @Override
    public boolean supportsPlates() {
        return false;
    }

    @Override
    public float width(String text) {
        return font().getWidth(text);
    }

    @Override
    public float lineHeight() {
        return font().fontHeight;
    }
}
