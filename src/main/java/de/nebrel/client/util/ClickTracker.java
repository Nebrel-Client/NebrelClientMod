package de.nebrel.client.util;

/**
 * Rolling clicks-per-second counter for one mouse button.
 *
 * <p>Backed by a fixed ring of timestamps rather than a growing list, so the
 * per-tick cost is constant and nothing is allocated while clicking.</p>
 *
 * <p>This only <em>observes</em> clicks the user actually made. Nothing here
 * generates input.</p>
 */
public final class ClickTracker {

    private static final int CAPACITY = 64;
    private static final long WINDOW_MILLIS = 1_000L;

    private final long[] timestamps = new long[CAPACITY];
    private int head;
    private int size;

    /** Records a click that just happened. */
    public void click() {
        this.timestamps[this.head] = System.currentTimeMillis();
        this.head = (this.head + 1) % CAPACITY;
        if (this.size < CAPACITY) {
            this.size++;
        }
    }

    /** Clicks within the last second. */
    public int cps() {
        long cutoff = System.currentTimeMillis() - WINDOW_MILLIS;
        int count = 0;
        for (int i = 0; i < this.size; i++) {
            // Walk backwards from the most recent entry.
            int index = Math.floorMod(this.head - 1 - i, CAPACITY);
            if (this.timestamps[index] >= cutoff) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public void reset() {
        this.head = 0;
        this.size = 0;
    }
}
