package de.nebrel.client.config;

import com.google.gson.JsonObject;

/**
 * One persistable area of the client, mapped to one file on disk.
 *
 * <p>Keeping this an interface is what lets {@link ConfigManager} stay free of
 * Minecraft: the HUD section is implemented next to the HUD code and simply
 * registers itself, so the manager never needs to know what a widget is.</p>
 */
public interface ConfigSection {

    /** File name inside {@code config/nebrelclient/}, including the extension. */
    String fileName();

    /** Fills {@code root} with everything this section wants persisted. */
    void write(JsonObject root);

    /**
     * Restores state from {@code root}.
     *
     * <p>Must tolerate missing and unknown keys: anything absent keeps its
     * default, anything unrecognised is ignored.</p>
     */
    void read(JsonObject root);
}
