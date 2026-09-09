package de.nebrel.client.plus.render;

/**
 * Somewhere a styled glyph can be drawn.
 *
 * <p>The world nametag draws through a vertex consumer with a 3D matrix; the
 * tab list, the chat and the designer preview draw through a {@code DrawContext}
 * in screen space. Those are different enough that writing the effect renderer
 * twice would be the obvious move — and would guarantee the preview eventually
 * stops matching the real nametag.</p>
 *
 * <p>This interface is the seam that avoids that. {@link IdentityRenderer}
 * composes badge and name once, against a sink, and each surface supplies its
 * own two-screenful implementation.</p>
 */
public interface GlyphSink {

    /**
     * Draws one glyph.
     *
     * @param text       the glyph, normally a single character
     * @param x          left edge, before the effect offset
     * @param y          top edge, before the effect offset
     * @param scale      size multiplier, about the glyph's own centre
     * @param skew       lean in radians, about the glyph's own centre
     * @param color      packed ARGB
     * @param background packed ARGB for a plate behind the glyph, or 0 for none
     */
    void glyph(String text, float x, float y, float scale, float skew, int color, int background);

    /**
     * Draws a plate.
     *
     * <p>Surfaces that cannot draw arbitrary geometry may ignore this and rely
     * on the {@code background} argument of {@link #glyph} instead.</p>
     */
    void plate(float x, float y, float width, float height, float radius, int color);

    /**
     * Draws a plate's border only.
     *
     * <p>Default is a no-op, which is the honest answer for a surface that has
     * no geometry of its own. {@link #supportsPlates()} says which it is, so a
     * caller can pick a fallback rather than draw nothing.</p>
     */
    default void plateOutline(float x, float y, float width, float height, float radius,
                              int color) {
    }

    /** Width of a string at scale 1. */
    float width(String text);

    /** Height of one line at scale 1. */
    float lineHeight();

    /** True when this surface can draw plates and outlines of its own. */
    default boolean supportsPlates() {
        return true;
    }
}
