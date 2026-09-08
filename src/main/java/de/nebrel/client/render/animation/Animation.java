package de.nebrel.client.render.animation;

import de.nebrel.client.util.NebrelMath;

/**
 * A single animated scalar.
 *
 * <p>Animations are pull based: nothing ticks them. {@link #value()} looks at
 * the wall clock and derives the current value, so an animation costs nothing
 * while it is not being drawn and never drifts when frames are dropped.</p>
 *
 * <p>Instances are meant to be long lived fields on components, never allocated
 * per frame.</p>
 */
public final class Animation {

    private final Easing easing;
    private final long durationMillis;

    private float origin;
    private float target;
    private long startMillis;

    public Animation(float initial, long durationMillis, Easing easing) {
        this.origin = initial;
        this.target = initial;
        this.durationMillis = Math.max(1L, durationMillis);
        this.easing = easing;
        this.startMillis = 0L;
    }

    public static Animation of(float initial, long durationMillis) {
        return new Animation(initial, durationMillis, Easing.EASE_OUT_CUBIC);
    }

    /**
     * Retargets the animation, starting from wherever it currently is so that
     * an interrupted transition continues smoothly instead of jumping.
     */
    public void animateTo(float newTarget) {
        if (Float.compare(newTarget, this.target) == 0) {
            return;
        }
        this.origin = value();
        this.target = newTarget;
        this.startMillis = now();
    }

    /** Jumps to {@code newValue} without animating. */
    public void set(float newValue) {
        this.origin = newValue;
        this.target = newValue;
        this.startMillis = 0L;
    }

    /** Convenience for boolean-driven transitions such as hover or toggle. */
    public void animateTo(boolean on) {
        animateTo(on ? 1.0F : 0.0F);
    }

    public float value() {
        if (this.startMillis == 0L) {
            return this.target;
        }
        long elapsed = now() - this.startMillis;
        if (elapsed >= this.durationMillis) {
            this.origin = this.target;
            this.startMillis = 0L;
            return this.target;
        }
        float t = NebrelMath.clamp(elapsed / (float) this.durationMillis, 0.0F, 1.0F);
        return NebrelMath.lerp(this.origin, this.target, this.easing.apply(t));
    }

    public float target() {
        return this.target;
    }

    public boolean finished() {
        return this.startMillis == 0L;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }
}
