package de.nebrel.client.util;

/**
 * Frame counter maintained by the client itself.
 *
 * <p>Counting frames here rather than reading the game's own counter keeps the
 * HUD independent of an internal field whose name and visibility have moved
 * between versions, and lets the displayed value be smoothed to something
 * readable instead of flickering every frame.</p>
 */
public final class FpsCounter {

    private static final long WINDOW_NANOS = 500_000_000L;

    private int framesInWindow;
    private long windowStartNanos = System.nanoTime();
    private int current;
    private float smoothed;

    /** Call once per rendered frame. */
    public void frame() {
        this.framesInWindow++;
        long now = System.nanoTime();
        long elapsed = now - this.windowStartNanos;
        if (elapsed >= WINDOW_NANOS) {
            this.current = Math.round(this.framesInWindow * 1_000_000_000.0F / elapsed);
            this.framesInWindow = 0;
            this.windowStartNanos = now;
            this.smoothed = this.smoothed == 0.0F
                    ? this.current
                    : NebrelMath.lerp(this.smoothed, this.current, 0.5F);
        }
    }

    /** Frames per second over the last half second. */
    public int fps() {
        return this.current;
    }

    /** Lightly smoothed value, steadier to read on a HUD. */
    public int smoothedFps() {
        return Math.round(this.smoothed);
    }
}
