package de.nebrel.client.render.animation;

import de.nebrel.client.util.NebrelMath;

/**
 * Inertia-free smooth scrolling with clamping.
 *
 * <p>The target offset moves instantly when the wheel turns; the rendered
 * offset chases it with a frame-rate independent exponential approach. That
 * keeps trackpads (many small deltas) and mouse wheels (few large deltas)
 * feeling the same without any velocity bookkeeping.</p>
 */
public final class SmoothScroll {

    private static final float APPROACH_SPEED = 0.45F;

    private float target;
    private float current;
    private float maxOffset;
    private long lastUpdateNanos;

    public SmoothScroll() {
        this.lastUpdateNanos = System.nanoTime();
    }

    /**
     * Applies a wheel delta.
     *
     * @param amount    the raw scroll amount, positive scrolls up
     * @param stepPixels pixels travelled per wheel notch
     */
    public void scroll(double amount, float stepPixels) {
        this.target = NebrelMath.clamp(this.target - (float) amount * stepPixels, 0.0F, this.maxOffset);
    }

    /** Recomputes the clamp from the current content and viewport size. */
    public void updateBounds(float contentHeight, float viewportHeight) {
        this.maxOffset = Math.max(0.0F, contentHeight - viewportHeight);
        this.target = NebrelMath.clamp(this.target, 0.0F, this.maxOffset);
        this.current = NebrelMath.clamp(this.current, 0.0F, this.maxOffset);
    }

    /** Advances and returns the offset to render at, in pixels from the top. */
    public float offset() {
        long now = System.nanoTime();
        float delta = (now - this.lastUpdateNanos) / 1_000_000_000.0F;
        this.lastUpdateNanos = now;
        // Guard against pauses (window minimised, world load) producing a huge dt.
        delta = NebrelMath.clamp(delta, 0.0F, 0.1F);
        this.current = NebrelMath.approach(this.current, this.target, APPROACH_SPEED, delta);
        if (Math.abs(this.current - this.target) < 0.05F) {
            this.current = this.target;
        }
        return this.current;
    }

    public void reset() {
        this.target = 0.0F;
        this.current = 0.0F;
    }

    public float maxOffset() {
        return this.maxOffset;
    }

    public boolean scrollable() {
        return this.maxOffset > 0.0F;
    }

    /** 0.0 at the top, 1.0 at the bottom; used to place the scrollbar thumb. */
    public float progress() {
        return this.maxOffset <= 0.0F ? 0.0F : this.current / this.maxOffset;
    }
}
