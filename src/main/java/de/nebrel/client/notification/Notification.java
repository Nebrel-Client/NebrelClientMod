package de.nebrel.client.notification;

/** One toast: a title, an optional body and a severity that picks its accent. */
public final class Notification {

    public enum Level {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private final String title;
    private final String message;
    private final Level level;
    private final long createdAt;
    private final long lifetimeMillis;

    public Notification(String title, String message, Level level, long lifetimeMillis) {
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.level = level == null ? Level.INFO : level;
        this.lifetimeMillis = Math.max(500L, lifetimeMillis);
        this.createdAt = System.currentTimeMillis();
    }

    public String title() {
        return this.title;
    }

    public String message() {
        return this.message;
    }

    public Level level() {
        return this.level;
    }

    public boolean hasMessage() {
        return !this.message.isEmpty();
    }

    public long age() {
        return System.currentTimeMillis() - this.createdAt;
    }

    public boolean expired() {
        return age() > this.lifetimeMillis;
    }

    /**
     * Slide and fade factor: 0.0 fully hidden, 1.0 fully shown.
     *
     * <p>Eases in over the first 180 ms and out over the last 260 ms.</p>
     */
    public float visibility() {
        long age = age();
        long remaining = this.lifetimeMillis - age;
        float in = Math.min(1.0F, age / 180.0F);
        float out = Math.min(1.0F, Math.max(0.0F, remaining / 260.0F));
        return Math.min(in, out);
    }
}
