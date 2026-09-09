package de.nebrel.client.plus.render;

/**
 * The animation clock every nametag effect reads.
 *
 * <p>One shared clock rather than each effect calling the system timer.
 * Sampling separately would let two effects on the same name drift a frame
 * apart, and would let the designer preview run on a different phase from the
 * nametag it is previewing.</p>
 *
 * <p>Measured from class load, so the value stays small and keeps full float
 * precision for a very long session.</p>
 */
public final class NametagClock {

    private static final long ORIGIN_NANOS = System.nanoTime();

    private NametagClock() {
    }

    /** Seconds since the client started. */
    public static float seconds() {
        return (System.nanoTime() - ORIGIN_NANOS) / 1_000_000_000.0F;
    }
}
