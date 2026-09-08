package de.nebrel.client.render;

import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.render.VertexConsumer;

/**
 * A {@link VertexConsumer} that multiplies every vertex colour by a tint.
 *
 * <p>Used to recolour the enchantment glint. The glint is drawn through its own
 * render layer with its own scrolling texture; the only thing that can change
 * its colour without touching a shader is the per-vertex colour, which is what
 * this intercepts.</p>
 *
 * <p>Every other vertex attribute is passed straight through, so the geometry,
 * UVs, lighting and normals are exactly what the caller supplied.</p>
 */
public final class TintingVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int tintRed;
    private final int tintGreen;
    private final int tintBlue;
    private final int tintAlpha;

    /**
     * @param delegate the consumer to forward to
     * @param tint     packed ARGB; each channel scales the incoming colour
     */
    public TintingVertexConsumer(VertexConsumer delegate, int tint) {
        this.delegate = delegate;
        this.tintRed = ColorUtil.red(tint);
        this.tintGreen = ColorUtil.green(tint);
        this.tintBlue = ColorUtil.blue(tint);
        this.tintAlpha = ColorUtil.alpha(tint);
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        // Multiplicative so a white glint takes the tint exactly, and a
        // partially transparent one stays partially transparent.
        this.delegate.color(
                red * this.tintRed / 255,
                green * this.tintGreen / 255,
                blue * this.tintBlue / 255,
                alpha * this.tintAlpha / 255);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        this.delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        this.delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }
}
