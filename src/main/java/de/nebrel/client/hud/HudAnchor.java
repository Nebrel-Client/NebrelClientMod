package de.nebrel.client.hud;

/**
 * Which corner or edge a HUD widget's stored position is measured from.
 *
 * <p>Anchoring is what keeps a HUD sane across resolution and GUI-scale
 * changes: a widget parked in the bottom-right stays in the bottom-right when
 * the window is resized, instead of drifting or falling off screen.</p>
 *
 * <p>A position is stored as a <em>pixel offset from the anchor point</em>, not
 * as a fraction of the screen. That distinction matters: a widget placed 10 px
 * from the bottom-right corner stays exactly 10 px from that corner on a
 * narrower window, whereas a fractional position would shrink the margin
 * proportionally and eventually overlap the neighbouring element.</p>
 */
public enum HudAnchor {

    TOP_LEFT("Top Left", 0.0F, 0.0F),
    TOP_CENTER("Top Center", 0.5F, 0.0F),
    TOP_RIGHT("Top Right", 1.0F, 0.0F),
    MIDDLE_LEFT("Middle Left", 0.0F, 0.5F),
    CENTER("Center", 0.5F, 0.5F),
    MIDDLE_RIGHT("Middle Right", 1.0F, 0.5F),
    BOTTOM_LEFT("Bottom Left", 0.0F, 1.0F),
    BOTTOM_CENTER("Bottom Center", 0.5F, 1.0F),
    BOTTOM_RIGHT("Bottom Right", 1.0F, 1.0F);

    private final String displayName;
    private final float xFactor;
    private final float yFactor;

    HudAnchor(String displayName, float xFactor, float yFactor) {
        this.displayName = displayName;
        this.xFactor = xFactor;
        this.yFactor = yFactor;
    }

    public String displayName() {
        return this.displayName;
    }

    public float xFactor() {
        return this.xFactor;
    }

    public float yFactor() {
        return this.yFactor;
    }

    /** The anchor's reference point on the x axis, in screen pixels. */
    public float originX(float screenWidth) {
        return this.xFactor * screenWidth;
    }

    /** The anchor's reference point on the y axis, in screen pixels. */
    public float originY(float screenHeight) {
        return this.yFactor * screenHeight;
    }

    /**
     * Absolute left edge for a widget of {@code width} stored at {@code offsetX}.
     *
     * <p>The widget is aligned to the anchor: a left anchor lines its left edge
     * up with the origin, a right anchor its right edge, a centre anchor its
     * middle. The offset then shifts it from there.</p>
     */
    public float resolveX(float offsetX, float screenWidth, float width) {
        return originX(screenWidth) + offsetX - this.xFactor * width;
    }

    /** Absolute top edge for a widget of {@code height} stored at {@code offsetY}. */
    public float resolveY(float offsetY, float screenHeight, float height) {
        return originY(screenHeight) + offsetY - this.yFactor * height;
    }

    /** Inverse of {@link #resolveX}: an absolute left edge back to a stored offset. */
    public float toOffsetX(float left, float screenWidth, float width) {
        return left + this.xFactor * width - originX(screenWidth);
    }

    /** Inverse of {@link #resolveY}. */
    public float toOffsetY(float top, float screenHeight, float height) {
        return top + this.yFactor * height - originY(screenHeight);
    }

    /**
     * Picks the anchor whose reference point is nearest the widget's centre.
     *
     * <p>Called once when a drag ends, so a widget dropped in the bottom-right
     * quadrant automatically starts anchoring there.</p>
     */
    public static HudAnchor nearest(float centerX, float centerY, float screenWidth, float screenHeight) {
        float fx = screenWidth <= 0.0F ? 0.5F : centerX / screenWidth;
        float fy = screenHeight <= 0.0F ? 0.5F : centerY / screenHeight;

        HudAnchor best = CENTER;
        float bestDistance = Float.MAX_VALUE;
        for (HudAnchor anchor : values()) {
            float dx = fx - anchor.xFactor;
            float dy = fy - anchor.yFactor;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = anchor;
            }
        }
        return best;
    }

    /**
     * Re-expresses an offset against a different anchor without moving the
     * widget on screen.
     *
     * <p>Used when a drag ends and {@link #nearest} picks a new anchor: the
     * widget must not jump at the moment it is released.</p>
     *
     * @return the offset {@code {x, y}} to store against {@code to}
     */
    public static float[] reanchor(HudAnchor from, HudAnchor to,
                                   float offsetX, float offsetY,
                                   float screenWidth, float screenHeight,
                                   float width, float height) {
        float left = from.resolveX(offsetX, screenWidth, width);
        float top = from.resolveY(offsetY, screenHeight, height);
        return new float[]{
                to.toOffsetX(left, screenWidth, width),
                to.toOffsetY(top, screenHeight, height)
        };
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
