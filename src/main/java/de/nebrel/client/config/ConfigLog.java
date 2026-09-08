package de.nebrel.client.config;

import java.util.function.BiConsumer;

/**
 * Indirection so the config layer can report problems without importing a
 * logging framework or Minecraft.
 *
 * <p>{@code NebrelClientMod} points this at the mod's SLF4J logger during
 * initialisation. Until then, and in headless verification runs, messages go to
 * standard error.</p>
 */
public final class ConfigLog {

    private static BiConsumer<String, Throwable> sink =
            (message, error) -> {
                System.err.println("[NebrelClient/config] " + message);
                if (error != null) {
                    System.err.println("  caused by: " + error);
                }
            };

    private ConfigLog() {
    }

    public static void setSink(BiConsumer<String, Throwable> newSink) {
        if (newSink != null) {
            sink = newSink;
        }
    }

    public static void warn(String message, Throwable error) {
        sink.accept(message, error);
    }

    public static void warn(String message) {
        sink.accept(message, null);
    }
}
